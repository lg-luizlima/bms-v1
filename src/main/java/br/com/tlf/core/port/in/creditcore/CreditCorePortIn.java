package br.com.tlf.core.port.in.creditcore;

import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;

public interface CreditCorePortIn {


    ActiveConsentResponseDTO getPendingTerms(String authorization, String product);

    void createConsent(String authorization, ConsentRequestDTO request);
}
