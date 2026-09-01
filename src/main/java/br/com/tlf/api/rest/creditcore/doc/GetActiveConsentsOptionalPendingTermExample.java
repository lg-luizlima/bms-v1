package br.com.tlf.api.rest.creditcore.doc;

import java.util.List;
import java.util.function.Supplier;

import br.com.tlf.api.rest.creditcore.CreditCoreControllerOpenApi;
import br.com.tlf.api.rest.creditcore.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.api.rest.creditcore.dto.response.PendingTermDTO;
import br.com.tlf.api.rest.shared.ResponseDTO;

/** Swagger example for {@code GET /terms}'s 200 response when a pending term is not mandatory. */
public class GetActiveConsentsOptionalPendingTermExample
        implements Supplier<ResponseDTO<ActiveConsentResponseDTO>> {

    @Override
    public ResponseDTO<ActiveConsentResponseDTO> get() {
        PendingTermDTO term = PendingTermDTO.builder()
                .termId("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
                .termCode("COMMUNICATION_TERMS")
                .title("Termos e Condições de Comunicação")
                .contentSummary("Consentimento para recebimento de comunicações promocionais.")
                .contentUrl("https://fintech-hml.vivo.com.br/files/termo-marketing.pdf")
                .isMandatory(false)
                .build();

        ActiveConsentResponseDTO data = ActiveConsentResponseDTO.builder()
                .product("CONSIGNADO_DATAPREV")
                .hasPendingMandatoryTerms(false)
                .pendingTerms(List.of(term))
                .build();

        return ResponseDTO.ok(data, CreditCoreControllerOpenApi.GET_TERMS_SUCCESS_MESSAGE);
    }
}
