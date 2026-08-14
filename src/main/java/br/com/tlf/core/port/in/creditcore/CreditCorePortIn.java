package br.com.tlf.core.port.in.creditcore;

import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.core.port.in.dto.response.ConsentResponseDTO;

public interface CreditCorePortIn {

    ActiveConsentResponseDTO getPendingTerms(String authorization, String product, String channelId,
            String correlationId, String customerId);

    ConsentResponseDTO createConsent(String authorization, ConsentRequestDTO request, String channelId,
            String correlationId, String customerId);
}
