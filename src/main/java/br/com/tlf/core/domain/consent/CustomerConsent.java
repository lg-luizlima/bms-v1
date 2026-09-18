package br.com.tlf.core.domain.consent;

import java.time.Instant;
import java.util.UUID;

import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
public record CustomerConsent(
        UUID id,
        String customerId,
        String termCode,
        UUID termId,
        Boolean optIn,
        Instant acceptedAt,
        Instant expiresAt,
        Signature signature) {

    private static final String BLOCKED_REVOKE_TERM = "DATAPREV_CONSENT";

    public boolean covers(TermsCatalogEntry term) {

        if (term.revokesPreviousVersions()) {

            if(term.termCode().equals(BLOCKED_REVOKE_TERM)) {
                log.warn("CustomerConsent.covers: termCode {} is blocked from revoking previous versions", BLOCKED_REVOKE_TERM);
                return false;
            }

            return termId != null && termId.equals(term.id()) ;
        }
        return true;
    }
}
