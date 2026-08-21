package br.com.tlf.core.application.service.creditcore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import br.com.tlf.shared.observability.ObservabilityPiiProperties;
import io.micrometer.observation.ObservationRegistry;

@ExtendWith(MockitoExtension.class)
class ConsentIdempotencyCheckerTest {

    private static final String CPF = "12345678900";
    private static final String CORRELATION_ID = "correlation-1";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    // Registro real (sem listeners) em vez de mock: Observation.createNotStarted(...) acessa
    // observationConfig() internamente, o que um mock não-stubado não suportaria sem NPE.
    @Spy private ObservationRegistry observationRegistry = ObservationRegistry.create();
    @Spy private ObservabilityPiiProperties observabilityPiiProperties = new ObservabilityPiiProperties();

    @InjectMocks
    private ConsentIdempotencyChecker underTest;

    @Test
    void findCachedResponse_whenKeyAbsent_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("post_consent_idempotency:" + CPF + ":" + CORRELATION_ID)).thenReturn(null);

        Optional<Instant> result = underTest.findCachedResponse(CPF, CORRELATION_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void findCachedResponse_whenKeyPresent_returnsParsedInstant() {
        Instant cached = Instant.parse("2026-08-13T10:00:00Z");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("post_consent_idempotency:" + CPF + ":" + CORRELATION_ID))
                .thenReturn(cached.toString());

        Optional<Instant> result = underTest.findCachedResponse(CPF, CORRELATION_ID);

        assertThat(result).contains(cached);
    }

    @Test
    void cacheResponse_writesKeyWithSixtySecondTtl() {
        Instant consentReceivedAt = Instant.parse("2026-08-13T10:00:00Z");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        underTest.cacheResponse(CPF, CORRELATION_ID, consentReceivedAt);

        verify(valueOperations).set(
                "post_consent_idempotency:" + CPF + ":" + CORRELATION_ID,
                consentReceivedAt.toString(),
                60L,
                TimeUnit.SECONDS);
    }

    @Test
    void findCachedResponse_whenRedisUnavailable_returnsEmptyInsteadOfPropagating() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("post_consent_idempotency:" + CPF + ":" + CORRELATION_ID))
                .thenThrow(new RedisConnectionFailureException("Unable to connect to Redis"));

        Optional<Instant> result = underTest.findCachedResponse(CPF, CORRELATION_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void cacheResponse_whenRedisUnavailable_doesNotPropagate() {
        Instant consentReceivedAt = Instant.parse("2026-08-13T10:00:00Z");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RedisConnectionFailureException("Unable to connect to Redis"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        assertThatNoException()
                .isThrownBy(() -> underTest.cacheResponse(CPF, CORRELATION_ID, consentReceivedAt));
    }
}
