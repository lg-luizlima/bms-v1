package br.com.tlf.domain.service.customerconsent;

import br.com.tlf.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.domain.vo.terms.TermsCatalogVO;

import java.util.List;

public interface CustomerConsentService {

    void createConsent(List<TermsCatalogVO> activeTerms,ConsentRequestVO request);

    ActiveConsentResponseVO pendingTerms(List<TermsCatalogVO> termsCatalog, String customerId);
}
