package br.com.tlf.api.rest.creditcore.dto.request;

import jakarta.validation.Valid;
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
public class SignatureDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank
    private String ip;

    @NotBlank
    private String userAgent;

    @NotBlank
    private String deviceId;

    @NotBlank
    private String channel;

    @Valid
    private GeolocationDTO geolocation;
}
