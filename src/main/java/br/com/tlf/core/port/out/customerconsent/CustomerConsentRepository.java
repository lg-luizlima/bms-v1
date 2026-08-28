package br.com.tlf.core.port.out.customerconsent;

import java.util.Collection;
import java.util.Map;

import br.com.tlf.core.domain.consent.CustomerConsent;

public interface CustomerConsentRepository {

    /**
     * The customer's currently active consent for each of {@code termCodes}, keyed by term code.
     * Term codes with no active consent are absent from the map.
     *
     * <p>Batched on purpose: both use cases need this for every term they are about to inspect, and
     * asking per term turned it into one query per term.
     */
    Map<String, CustomerConsent> findActiveConsentsByTermCode(String customerId, Collection<String> termCodes);

    CustomerConsent save(CustomerConsent consent);
}
