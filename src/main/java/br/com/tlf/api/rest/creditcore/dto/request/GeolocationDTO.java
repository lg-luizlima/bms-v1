package br.com.tlf.api.rest.creditcore.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
public class GeolocationDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "Latitude at signature time", example = "-23.55052",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("lat")
    private String lat;

    @NotBlank
    @Schema(description = "Longitude at signature time", example = "-46.633308",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("long")
    private String lon;
}
