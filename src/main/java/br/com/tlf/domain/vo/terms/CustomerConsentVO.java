package br.com.tlf.domain.vo.terms;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerConsentVO {

    private UUID id;
    private String cpfHash;
    private String termCode;
    private UUID termId;
    private Boolean optIn;
    private Instant acceptedAt;
    private Instant expiresAt;
    private String auditDetails;
}
