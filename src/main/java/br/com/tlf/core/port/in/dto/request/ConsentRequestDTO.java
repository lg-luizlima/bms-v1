package br.com.tlf.core.port.in.dto.request;

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
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsentRequestDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty
    @Valid
    @Schema(description = "List of accepted terms")
    private List<AcceptedTermDTO> acceptedTerms;

    @NotNull
    @Valid
    @Schema(description = "Audit signature data")
    private SignatureDTO signature;
}
