package br.com.tlf.api.rest.creditcore.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentResponseDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "Timestamp when the consent was received and persisted",
            example = "2026-09-01T14:23:05.123Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("consentReceivedAt")
    private Instant consentReceivedAt;
}
