package br.com.tlf.core.domain.consent;

import java.time.Instant;
import java.util.UUID;

import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import lombok.Builder;

@Builder
public record CustomerConsent(
        UUID id,
        String customerId,
        String termCode,
        UUID termId,
        Boolean optIn,
        Instant acceptedAt,
        Instant expiresAt,
        Signature signature) {


    public boolean covers(TermsCatalogEntry term) {
        if (term.revokesPreviousVersions()) {
            return termId != null && termId.equals(term.id());
        }
        return true;
    }
}
