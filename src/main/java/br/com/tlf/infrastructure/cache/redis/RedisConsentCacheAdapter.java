package br.com.tlf.infrastructure.cache.redis;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import br.com.tlf.core.port.out.cache.ConsentCachePort;
import br.com.tlf.shared.observability.ObservabilityPiiProperties;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class RedisConsentCacheAdapter implements ConsentCachePort {

    private static final String PROCESSING = "PROCESSING";

    private final StringRedisTemplate redisTemplate;
    private final ObservationRegistry observationRegistry;
    private final ObservabilityPiiProperties observabilityPiiProperties;
    private final RedisTtlProperties ttl;

    @Override
    public Optional<Instant> findIdempotentResponse(String customerId, String correlationId) {
        String key = RedisKeys.postConsentIdempotency(customerId, correlationId);

        log.info("[findIdempotentResponse] Checking idempotency cache for correlationId: {} with key: {}", correlationId, key);

        String cachedValue = observed("redis.idempotency.read", "GET post_consent_idempotency", "GET", key, null,
                () -> redisTemplate.opsForValue().get(key),
                ex -> log.info("[findIdempotentResponse] Redis unavailable, treating as cache miss "
                        + "(idempotency window not enforced for correlationId: {}, key: {})",
                        correlationId, key, ex.getMessage()));

        return Optional.ofNullable(cachedValue).map(Instant::parse);
    }

    @Override
    public void writeCacheIdempotentResponse(String customerId, String correlationId, Instant consentReceivedAt) {
        String key = RedisKeys.postConsentIdempotency(customerId, correlationId);
        String value = consentReceivedAt.toString();
        Duration ttlDuration = ttl.consentIdempotencyCheck();

        if (ttlDuration.isZero() || ttlDuration.isNegative()) {
            log.error("[writeCacheIdempotentResponse] Invalid TTL configured for idempotency key "
                    + "(correlationId: {}, key: {}, ttl: {}). Skipping Redis write.",
                    correlationId, key, ttlDuration);
            return;
        }

        log.info("[writeCacheIdempotentResponse] Caching idempotency response for correlationId: {} with key: {} and value: {}", correlationId, key, value);

        Boolean writeSucceeded = observed("redis.idempotency.write", "SET post_consent_idempotency", "SET", key, value,
                () -> {
                    redisTemplate.opsForValue().set(key, value, ttlDuration);
                    return Boolean.TRUE;
                },
                ex -> log.warn("[writeCacheIdempotentResponse] Redis unavailable, idempotency response not cached "
                        + "for correlationId: {}, key: {}, ttl: {}",
                        correlationId, key, ttlDuration, ex));

        if (Boolean.TRUE.equals(writeSucceeded)) {
            log.info("[writeCacheIdempotentResponse] Idempotency response cached successfully for correlationId: {}",
                    correlationId);
        }
    }

    @Override
    public void ensureProcessing(String customerId, String termCode) {
        String key = RedisKeys.syncConsentStatus(customerId);

    log.info("[ensureProcessing] Ensuring processing status for customerId: {} and termCode: {} with key: {}", customerId, termCode, key);

        Boolean fieldWasAbsent = observed("redis.consent_status.ensure_processing", "HSETNX sync_consent_status",
                "HSETNX", key, PROCESSING,
                () -> redisTemplate.<String, String>opsForHash().putIfAbsent(key, termCode, PROCESSING),
                ex -> log.warn("[ensureProcessing] Redis unavailable, status not cached for termCode: {}, key: {}",
                        termCode, key, ex.getMessage()));

        if (Boolean.TRUE.equals(fieldWasAbsent)) {
            observed("redis.consent_status.expire", "EXPIRE sync_consent_status", "EXPIRE", key, null,
                    () -> {
                        redisTemplate.expire(key, ttl.syncConsentStatus());
                        return null;
                    },
                    ex -> log.warn("[ensureProcessing] Redis unavailable, TTL not set for termCode: {}, key: {}",
                            termCode, key, ex));
        }
    }

    /**
     * Wraps one Redis command in its own observation. Lettuce's built-in instrumentation only reports
     * the command name and peer address, so key and value are attached here — and only where the
     * environment allows PII in telemetry.
     */
    private <T> T observed(String name, String contextualName, String command, String key, String value,
            Supplier<T> operation, java.util.function.Consumer<DataAccessException> onFailure) {

        Observation observation = Observation.createNotStarted(name, observationRegistry)
                .contextualName(contextualName)
                .lowCardinalityKeyValue("redis.command", command);

        if (observabilityPiiProperties.isRedisValuesEnabled()) {
            observation.highCardinalityKeyValue("redis.key", key);
            if (value != null) {
                observation.highCardinalityKeyValue("redis.value", value);
            }
        }

        try {
            return observation.observe(operation);
        } catch (DataAccessException ex) {
            onFailure.accept(ex);
            return null;
        }
    }
}
