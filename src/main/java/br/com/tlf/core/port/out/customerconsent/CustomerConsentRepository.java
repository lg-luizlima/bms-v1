package br.com.tlf.core.port.out.customerconsent;


import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;

import java.util.Optional;
import java.util.UUID;

public interface CustomerConsentRepository {

    Boolean getActiveConsent(String cpfHash, String termCode);

    Optional<UUID> getActiveConsentTermId(String cpfHash, String termCode);

    void saveConsent(CustomerConsentVO consent);
}
