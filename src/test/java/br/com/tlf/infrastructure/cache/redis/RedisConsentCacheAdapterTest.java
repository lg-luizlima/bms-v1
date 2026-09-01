package br.com.tlf.infrastructure.cache.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import br.com.tlf.shared.observability.ObservabilityPiiProperties;
import io.micrometer.observation.ObservationRegistry;

@ExtendWith(MockitoExtension.class)
class RedisConsentCacheAdapterTest {

    private static final String CUSTOMER_ID = "52998224725";
    private static final String CORRELATION_ID = "correlation-1";
    private static final String TERM_CODE = "DATAPREV_CONSENT";
    private static final String IDEMPOTENCY_KEY = "post_consent_idempotency:52998224725:correlation-1";
    private static final String SYNC_CONSENT_STATUS_KEY = "sync_consent_status:52998224725";

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private HashOperations<String, String, String> hashOperations;

    private RedisConsentCacheAdapter underTest;

    @BeforeEach
    void setUp() {
        RedisTtlProperties ttl = new RedisTtlProperties();
        underTest = new RedisConsentCacheAdapter(redisTemplate, ObservationRegistry.create(),
                new ObservabilityPiiProperties(), ttl);
    }

    @Test
    void findIdempotentResponse_returnsCachedInstant() {
        Instant cachedAt = Instant.parse("2026-08-28T12:00:00Z");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(IDEMPOTENCY_KEY)).thenReturn(cachedAt.toString());

        assertThat(underTest.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).contains(cachedAt);
    }

    @Test
    void findIdempotentResponse_missReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(IDEMPOTENCY_KEY)).thenReturn(null);

        assertThat(underTest.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).isEmpty();
    }

    @Test
    void findIdempotentResponse_redisDown_degradesToCacheMiss() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        Optional<Instant> result = underTest.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void writeCacheIdempotentResponse_writesWithConfiguredTtl() {
        Instant receivedAt = Instant.parse("2026-08-28T12:00:00Z");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        underTest.writeCacheIdempotentResponse(CUSTOMER_ID, CORRELATION_ID, receivedAt);

        verify(valueOperations).set(IDEMPOTENCY_KEY, receivedAt.toString(), Duration.ofSeconds(60));
    }

    @Test
    void writeCacheIdempotentResponse_redisDown_isSwallowed() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));

        underTest.writeCacheIdempotentResponse(CUSTOMER_ID, CORRELATION_ID, Instant.now());
    }

    @Test
    void ensureProcessing_setsProcessingAndExpiresWhenAbsent() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.putIfAbsent(SYNC_CONSENT_STATUS_KEY, TERM_CODE, "PROCESSING")).thenReturn(true);

        underTest.ensureProcessing(CUSTOMER_ID, TERM_CODE);

        verify(hashOperations).putIfAbsent(SYNC_CONSENT_STATUS_KEY, TERM_CODE, "PROCESSING");
        verify(redisTemplate).expire(SYNC_CONSENT_STATUS_KEY, Duration.ofSeconds(86400));
    }

    @Test
    void ensureProcessing_doesNotTouchTtlWhenAlreadyPresent() {
        when(redisTemplate.<String, String>opsForHash()).thenReturn(hashOperations);
        when(hashOperations.putIfAbsent(SYNC_CONSENT_STATUS_KEY, TERM_CODE, "PROCESSING")).thenReturn(false);

        underTest.ensureProcessing(CUSTOMER_ID, TERM_CODE);

        verify(redisTemplate, never()).expire(SYNC_CONSENT_STATUS_KEY, Duration.ofSeconds(86400));
    }

    @Test
    void ensureProcessing_redisDown_isSwallowed() {
        when(redisTemplate.<String, String>opsForHash()).thenThrow(new RedisConnectionFailureException("down"));

        underTest.ensureProcessing(CUSTOMER_ID, TERM_CODE);

        verify(redisTemplate, never()).expire(SYNC_CONSENT_STATUS_KEY, Duration.ofSeconds(86400));
    }
}
