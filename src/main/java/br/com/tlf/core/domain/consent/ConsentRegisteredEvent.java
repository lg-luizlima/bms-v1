package br.com.tlf.core.domain.consent;

import java.util.UUID;

import lombok.Builder;

@Builder
public record ConsentRegisteredEvent(
        String customerId,
        String termCode,
        String templateId,
        UUID termId,
        Boolean optIn,
        String expiresAt,
        Signature signature) {
}
