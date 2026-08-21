package br.com.tlf.core.application.service.creditcore;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import br.com.tlf.shared.observability.ObservabilityPiiProperties;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConsentIdempotencyChecker {

    private final StringRedisTemplate redisTemplate;
    private final ObservationRegistry observationRegistry;
    private final ObservabilityPiiProperties observabilityPiiProperties;

    @Value("${redis.ttl.consent-idempotency-check-seconds:60}")
    private long idempotencyTtlSeconds = 60L;

    public Optional<Instant> findCachedResponse(String cpf, String correlationId) {
        String redisKey = idempotencyKey(cpf, correlationId);

        Observation redisReadObservation = Observation.createNotStarted("redis.idempotency.read", observationRegistry)
                .contextualName("GET post_consent_idempotency")
                .lowCardinalityKeyValue("redis.command", "GET");

        if (observabilityPiiProperties.isRedisValuesEnabled()) {
            redisReadObservation.highCardinalityKeyValue("redis.key", redisKey);
        }

        String cachedValue;
        try {
            cachedValue = redisReadObservation.observe(() -> redisTemplate.opsForValue().get(redisKey));
        } catch (DataAccessException ex) {
            log.warn("[findCachedResponse] Redis unavailable, treating as cache miss (idempotency window not enforced for correlationId: {}): {}",
                    correlationId, ex.getMessage());
            return Optional.empty();
        }

        return Optional.ofNullable(cachedValue).map(Instant::parse);
    }

    public void cacheResponse(String cpf, String correlationId, Instant consentReceivedAt) {
        String redisKey = idempotencyKey(cpf, correlationId);
        String redisValue = consentReceivedAt.toString();

        Observation redisWriteObservation = Observation.createNotStarted("redis.idempotency.write", observationRegistry)
                .contextualName("SET post_consent_idempotency")
                .lowCardinalityKeyValue("redis.command", "SET");

        if (observabilityPiiProperties.isRedisValuesEnabled()) {
            redisWriteObservation
                    .highCardinalityKeyValue("redis.key", redisKey)
                    .highCardinalityKeyValue("redis.value", redisValue);
        }

        try {
            redisWriteObservation.observe(() ->
                    redisTemplate.opsForValue().set(redisKey, redisValue, idempotencyTtlSeconds, TimeUnit.SECONDS));
        } catch (DataAccessException ex) {
            log.warn("[cacheResponse] Redis unavailable, idempotency response not cached for correlationId: {}: {}",
                    correlationId, ex.getMessage());
        }
    }

    private String idempotencyKey(String cpf, String correlationId) {
        return "post_consent_idempotency:" + cpf + ":" + correlationId;
    }
}
