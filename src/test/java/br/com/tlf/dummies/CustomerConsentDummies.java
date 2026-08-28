package br.com.tlf.dummies;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import br.com.tlf.core.domain.consent.CustomerConsent;

public final class CustomerConsentDummies {

    public static final UUID CONSENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private CustomerConsentDummies() {
    }

    public static CustomerConsent revokedTermConsent() {
        return consentWithTermId(CreditTermDummies.REVOKED_TERM_ID, CreditTermDummies.REVOKED_TERM_CODE);
    }

    public static CustomerConsent softTermConsent() {
        return consentWithTermId(CreditTermDummies.SOFT_TERM_ID, CreditTermDummies.SOFT_TERM_CODE);
    }

    public static CustomerConsent consentWithTermId(UUID termId, String termCode) {
        return CustomerConsent.builder()
                .id(CONSENT_ID)
                .customerId(CreditTermDummies.CUSTOMER_ID)
                .termCode(termCode)
                .termId(termId)
                .optIn(Boolean.TRUE)
                .acceptedAt(Instant.now())
                .expiresAt(Instant.now().plus(365, ChronoUnit.DAYS))
                .signature(ConsentRequestDummies.signature())
                .build();
    }
}
