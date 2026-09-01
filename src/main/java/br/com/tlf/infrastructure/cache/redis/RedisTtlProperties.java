package br.com.tlf.infrastructure.cache.redis;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;


@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "redis.ttl")
public class RedisTtlProperties {

    private long syncConsentStatusSeconds = 86400L;
    private long consentIdempotencyCheckSeconds = 60L;

    public Duration syncConsentStatus() {
        return Duration.ofSeconds(syncConsentStatusSeconds);
    }

    public Duration consentIdempotencyCheck() {
        return Duration.ofSeconds(consentIdempotencyCheckSeconds);
    }
}
