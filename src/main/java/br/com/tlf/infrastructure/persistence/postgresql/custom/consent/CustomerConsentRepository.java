package br.com.tlf.infrastructure.persistence.postgresql.custom.consent;


import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;

import java.util.Optional;
import java.util.UUID;

public interface CustomerConsentRepository {

    Boolean getActiveConsent(String cpfHash, String termCode);

    Optional<UUID> getActiveConsentTermId(String cpfHash, String termCode);

    void saveConsent(CustomerConsentVO consent);
}
