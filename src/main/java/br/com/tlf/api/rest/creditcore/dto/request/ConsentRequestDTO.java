package br.com.tlf.api.rest.creditcore.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class ConsentRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty
    @Valid
    @Schema(description = "List of terms the customer is accepting or opting out of",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private List<AcceptedTermDTO> acceptedTerms;

    @NotNull
    @Valid
    @Schema(description = "Audit signature data proving who signed the consent and how",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private SignatureDTO signature;
}
