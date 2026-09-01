package br.com.tlf.api.rest.creditcore.doc;

import java.util.List;
import java.util.function.Supplier;

import br.com.tlf.api.rest.creditcore.CreditCoreControllerOpenApi;
import br.com.tlf.api.rest.creditcore.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.api.rest.shared.ResponseDTO;

/** Swagger example for {@code GET /terms}'s 200 response when the customer has nothing pending. */
public class GetActiveConsentsNoPendingTermsExample implements Supplier<ResponseDTO<ActiveConsentResponseDTO>> {

    @Override
    public ResponseDTO<ActiveConsentResponseDTO> get() {
        ActiveConsentResponseDTO data = ActiveConsentResponseDTO.builder()
                .product("CONSIGNADO_DATAPREV")
                .hasPendingMandatoryTerms(false)
                .pendingTerms(List.of())
                .build();

        return ResponseDTO.ok(data, CreditCoreControllerOpenApi.GET_TERMS_SUCCESS_MESSAGE);
    }
}
