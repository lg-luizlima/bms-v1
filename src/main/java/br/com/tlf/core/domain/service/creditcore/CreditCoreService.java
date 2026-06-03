package br.com.tlf.core.domain.service.creditcore;

import br.com.tlf.api.rest.dto.request.consent.ConsentRequestDTO;
import br.com.tlf.api.rest.dto.response.consent.ActiveConsentResponseDTO;

public interface CreditCoreService {


    ActiveConsentResponseDTO getPendingTerms(String authorization, String product);

    void createConsent(String authorization, ConsentRequestDTO request);
}
