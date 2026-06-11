package br.com.tlf.core.port.in.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("lat")
    private String lat;

    @NotBlank
    @JsonProperty("long")
    private String lon;
}
