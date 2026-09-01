package br.com.tlf.core.port.out.cache;

import java.time.Instant;
import java.util.Optional;


public interface ConsentCachePort {

    Optional<Instant> findIdempotentResponse(String customerId, String correlationId);

    void writeCacheIdempotentResponse(String customerId, String correlationId, Instant consentReceivedAt);


    void ensureProcessing(String customerId, String termCode);
}
