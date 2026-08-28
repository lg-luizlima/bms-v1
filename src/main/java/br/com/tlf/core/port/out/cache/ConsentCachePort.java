package br.com.tlf.core.port.out.cache;

import java.time.Instant;
import java.util.Optional;


public interface ConsentCachePort {

    Optional<Instant> findIdempotentResponse(String customerId, String correlationId);

    void cacheIdempotentResponse(String customerId, String correlationId, Instant consentReceivedAt);

    void writeSyncStatus(String customerId);
}
