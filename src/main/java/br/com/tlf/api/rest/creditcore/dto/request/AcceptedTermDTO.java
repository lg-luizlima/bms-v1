package br.com.tlf.api.rest.creditcore.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcceptedTermDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "Term identifier (UUID)", example = "41dfaa2f-3fee-432f-96a0-ffee95cfb2be")
    @JsonProperty("termId")
    private String termId;

    @NotNull
    @Schema(description = "Whether the user opted in", example = "true")
    private Boolean optIn;
}
