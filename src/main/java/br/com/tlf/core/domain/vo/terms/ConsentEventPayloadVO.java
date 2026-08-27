package br.com.tlf.core.domain.vo.terms;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsentEventPayloadVO {

    private String customerId;
    private String termCode;
    private String templateId;
    private UUID termId;
    private Boolean optIn;
    private String expiresAt;
    private ConsentSignaturePayloadVO signature;
}
