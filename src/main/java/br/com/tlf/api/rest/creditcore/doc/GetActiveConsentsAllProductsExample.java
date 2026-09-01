package br.com.tlf.api.rest.creditcore.doc;

import java.util.List;
import java.util.function.Supplier;

import br.com.tlf.api.rest.creditcore.CreditCoreControllerOpenApi;
import br.com.tlf.api.rest.creditcore.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.api.rest.creditcore.dto.response.PendingTermDTO;
import br.com.tlf.api.rest.shared.ResponseDTO;

/** Swagger example for {@code GET /terms}'s 200 response when {@code product} is omitted: an array with
 *  one entry per product that has at least one term pending acceptance. */
public class GetActiveConsentsAllProductsExample implements Supplier<ResponseDTO<List<ActiveConsentResponseDTO>>> {

    @Override
    public ResponseDTO<List<ActiveConsentResponseDTO>> get() {
        PendingTermDTO dataprevTerm = PendingTermDTO.builder()
                .termId("f2b02f36-0194-4951-95aa-50b2efdcf1b7")
                .termCode("DATAPREV_CONSENT")
                .title("Autorização de Consulta Vínculos DATAPREV")
                .contentSummary("Autorização para consulta de vínculos empregatícios junto à DATAPREV.")
                .contentUrl("https://fintech-hml.vivo.com.br/files/termo-de-autorizacao-dataprev.pdf")
                .isMandatory(true)
                .build();

        PendingTermDTO marketingTerm = PendingTermDTO.builder()
                .termId("357ae8f8-7880-4d36-bfb4-28ea637d1a29")
                .termCode("GENERAL_CREDIT_TERMS")
                .title("Termos e Condições de Comunicação")
                .contentSummary("Consentimento geral dos produtos de crédito")
                .contentUrl("https://fintech-hml.vivo.com.br/files/termo-geral-de-credito.pdf")
                .isMandatory(false)
                .build();

        List<ActiveConsentResponseDTO> data = List.of(
                ActiveConsentResponseDTO.builder()
                        .product("CONSIGNADO_DATAPREV")
                        .hasPendingMandatoryTerms(true)
                        .pendingTerms(List.of(dataprevTerm))
                        .build(),
                ActiveConsentResponseDTO.builder()
                        .product("CREDITO_PESSOAL")
                        .hasPendingMandatoryTerms(false)
                        .pendingTerms(List.of(marketingTerm))
                        .build());

        return ResponseDTO.ok(data, CreditCoreControllerOpenApi.GET_TERMS_SUCCESS_MESSAGE);
    }
}
