package br.com.tlf.api.rest.dto.request.consent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsentRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "Product identifier", example = "EP_INSS")
    private String product;

    @NotEmpty
    @Valid
    @Schema(description = "List of accepted terms")
    private List<AcceptedTermDTO> acceptedTerms;

    @NotNull
    @Valid
    @Schema(description = "Audit signature data")
    private SignatureDTO signature;
}
