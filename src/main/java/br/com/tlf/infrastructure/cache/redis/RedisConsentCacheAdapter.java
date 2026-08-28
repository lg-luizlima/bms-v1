package br.com.tlf.infrastructure.cache.redis;

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

    private static final String SYNC_STATUS_PROCESSING = "PROCESSING";

    private final StringRedisTemplate redisTemplate;
    private final ObservationRegistry observationRegistry;
    private final ObservabilityPiiProperties observabilityPiiProperties;
    private final RedisTtlProperties ttl;

    @Override
    public Optional<Instant> findIdempotentResponse(String customerId, String correlationId) {
        String key = RedisKeys.postConsentIdempotency(customerId, correlationId);

        String cachedValue = observed("redis.idempotency.read", "GET post_consent_idempotency", "GET", key, null,
                () -> redisTemplate.opsForValue().get(key),
                ex -> log.warn("[findIdempotentResponse] Redis unavailable, treating as cache miss "
                        + "(idempotency window not enforced for correlationId: {}): {}", correlationId, ex.getMessage()));

        return Optional.ofNullable(cachedValue).map(Instant::parse);
    }

    @Override
    public void cacheIdempotentResponse(String customerId, String correlationId, Instant consentReceivedAt) {
        String key = RedisKeys.postConsentIdempotency(customerId, correlationId);
        String value = consentReceivedAt.toString();

        observed("redis.idempotency.write", "SET post_consent_idempotency", "SET", key, value,
                () -> {
                    redisTemplate.opsForValue().set(key, value, ttl.consentIdempotencyCheck());
                    return null;
                },
                ex -> log.warn("[cacheIdempotentResponse] Redis unavailable, idempotency response not cached "
                        + "for correlationId: {}: {}", correlationId, ex.getMessage()));
    }

    @Override
    public void writeSyncStatus(String customerId) {
        String key = RedisKeys.syncStatus(customerId);

        observed("redis.sync_status.write", "SET sync_status", "SET", key, SYNC_STATUS_PROCESSING,
                () -> {
                    redisTemplate.opsForValue().set(key, SYNC_STATUS_PROCESSING, ttl.syncStatus());
                    return null;
                },
                ex -> log.warn("[writeSyncStatus] Redis unavailable, sync status not cached: {}", ex.getMessage()));
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
