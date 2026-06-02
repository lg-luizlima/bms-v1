package br.com.tlf.api.lending.rest.v1.dto.response.consent;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentDetailsDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty("consentId")
    private UUID consentId;

    @JsonProperty("acceptedAt")
    private LocalDateTime acceptedAt;

    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;

    @JsonProperty("termId")
    private String termId;
}

