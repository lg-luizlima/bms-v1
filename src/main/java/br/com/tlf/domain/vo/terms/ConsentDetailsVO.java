package br.com.tlf.domain.vo.terms;

import br.com.tlf.api.lending.rest.v1.dto.response.consent.ConsentDetailsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentDetailsVO {

    private UUID consentId;
    private Instant acceptedAt;
    private Instant expiresAt;
    private String termId;
}

