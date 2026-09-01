package br.com.tlf.infrastructure.persistence.postgresql.outbox.contract;

import java.util.UUID;


public record ConsentRegisteredPayload(
        String customerId,
        String termCode,
        String templateId,
        UUID termId,
        Boolean optIn,
        String expiresAt,
        ConsentSignaturePayload signature) {
}
