package br.com.tlf.application.facade.creditcore;

import br.com.tlf.api.lending.rest.v1.dto.request.consent.ConsentRequestDTO;
import br.com.tlf.api.lending.rest.v1.dto.response.consent.ActiveConsentResponseDTO;

public interface CreditCoreFacade {

    void createConsent(String authorization, ConsentRequestDTO request);

    ActiveConsentResponseDTO getPendingTerms(String authorization, String product) ;

}
