package br.com.tlf.core.application.service.creditcore;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import br.com.tlf.shared.observability.ObservabilityPiiProperties;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConsentIdempotencyChecker {

    private static final long IDEMPOTENCY_TTL_SECONDS = 60L;

    private final StringRedisTemplate redisTemplate;
    private final ObservationRegistry observationRegistry;
    private final ObservabilityPiiProperties observabilityPiiProperties;

    public Optional<Instant> findCachedResponse(String cpf, String correlationId) {
        String redisKey = idempotencyKey(cpf, correlationId);

        Observation redisReadObservation = Observation.createNotStarted("redis.idempotency.read", observationRegistry)
                .contextualName("GET post_consent_idempotency")
                .lowCardinalityKeyValue("redis.command", "GET");

        if (observabilityPiiProperties.isRedisValuesEnabled()) {
            redisReadObservation.highCardinalityKeyValue("redis.key", redisKey);
        }

        String cachedValue = redisReadObservation.observe(() -> redisTemplate.opsForValue().get(redisKey));

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

        redisWriteObservation.observe(() ->
                redisTemplate.opsForValue().set(redisKey, redisValue, IDEMPOTENCY_TTL_SECONDS, TimeUnit.SECONDS));
    }

    private String idempotencyKey(String cpf, String correlationId) {
        return "post_consent_idempotency:" + cpf + ":" + correlationId;
    }
}
