package br.com.tlf.core.port.out.customerconsent;

import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;

public interface CustomerConsentRepository {

    CustomerConsentVO getActiveCustomerConsent(String cpfToken, String termCode);

    CustomerConsentVO saveConsent(CustomerConsentVO consent);
}
