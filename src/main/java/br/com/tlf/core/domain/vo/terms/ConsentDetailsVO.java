package br.com.tlf.core.domain.vo.terms;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
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

