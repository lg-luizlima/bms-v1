package br.com.tlf.infrastructure.cache.redis;


final class RedisKeys {

    private RedisKeys() {
    }

    static String syncStatus(String customerId) {
        return "sync_status:" + customerId;
    }

    static String postConsentIdempotency(String customerId, String correlationId) {
        return "post_consent_idempotency:" + customerId + ":" + correlationId;
    }
}
