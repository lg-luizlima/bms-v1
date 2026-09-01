package br.com.tlf.api.rest.creditcore.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
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
public class SignatureDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    @Schema(description = "IP address of the device that signed the consent", example = "189.45.12.7",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String ip;

    @NotBlank
    @Schema(description = "User-Agent header of the device that signed the consent",
            example = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X)",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String userAgent;

    @NotBlank
    @Schema(description = "Unique identifier of the device that signed the consent",
            example = "a3f1c2e4-8b6d-4e2a-9c1f-0d5e7b2a1c34", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deviceId;

    @NotBlank
    @Schema(description = "Channel through which the consent was signed", example = "APP",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String channel;

    @Valid
    @Schema(description = "Geolocation of the device at signature time, when available")
    private GeolocationDTO geolocation;
}
