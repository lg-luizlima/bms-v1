package br.com.tlf.persistence.repository.custom.consent;


import br.com.tlf.domain.vo.terms.CustomerConsentVO;

import java.util.UUID;

public interface CustomerConsentRepository {

    Boolean getConsentsRevoked(String cpfHash, UUID termId);

    Boolean getActiveConsent(String cpfHash, String termCode);


    void saveConsent(CustomerConsentVO consent);
}
