package br.com.tlf.api.rest.creditcore.doc;

import java.util.List;
import java.util.function.Supplier;

import br.com.tlf.api.rest.creditcore.CreditCoreControllerOpenApi;
import br.com.tlf.api.rest.creditcore.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.api.rest.creditcore.dto.response.PendingTermDTO;
import br.com.tlf.api.rest.shared.ResponseDTO;

/** Swagger example for {@code GET /terms}'s 200 response when a pending term is mandatory. */
public class GetActiveConsentsMandatoryPendingTermExample
        implements Supplier<ResponseDTO<ActiveConsentResponseDTO>> {

    @Override
    public ResponseDTO<ActiveConsentResponseDTO> get() {
        PendingTermDTO term = PendingTermDTO.builder()
                .termId("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                .termCode("DATAPREV_CONSENT")
                .title("Autorização de Consulta Vínculos DATAPREV")
                .contentSummary("Autorização para consulta de vínculos empregatícios junto à DATAPREV.")
                .contentUrl("https://fintech-hml.vivo.com.br/files/termo-de-autorizacao-dataprev.pdf")
                .isMandatory(true)
                .build();

        ActiveConsentResponseDTO data = ActiveConsentResponseDTO.builder()
                .product("CONSIGNADO_DATAPREV")
                .hasPendingMandatoryTerms(true)
                .pendingTerms(List.of(term))
                .build();

        return ResponseDTO.ok(data, CreditCoreControllerOpenApi.GET_TERMS_SUCCESS_MESSAGE);
    }
}
