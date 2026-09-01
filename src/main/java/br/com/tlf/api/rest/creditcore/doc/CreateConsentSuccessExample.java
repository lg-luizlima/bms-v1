package br.com.tlf.api.rest.creditcore.doc;

import java.time.Instant;
import java.util.function.Supplier;

import br.com.tlf.api.rest.creditcore.CreditCoreControllerOpenApi;
import br.com.tlf.api.rest.creditcore.dto.response.ConsentResponseDTO;
import br.com.tlf.api.rest.shared.ResponseDTO;

/** Swagger example data for {@code POST /consents}'s 201 response. See {@code ApiSuccessExample}. */
public class CreateConsentSuccessExample implements Supplier<ResponseDTO<ConsentResponseDTO>> {

    @Override
    public ResponseDTO<ConsentResponseDTO> get() {
        ConsentResponseDTO data = ConsentResponseDTO.builder()
                .consentReceivedAt(Instant.parse("2026-09-01T14:23:05.123Z"))
                .build();

        return ResponseDTO.success(data, CreditCoreControllerOpenApi.CREATE_CONSENT_SUCCESS_MESSAGE);
    }
}
