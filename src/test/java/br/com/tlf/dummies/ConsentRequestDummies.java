package br.com.tlf.dummies;

import java.util.Collections;
import java.util.List;

import br.com.tlf.api.rest.creditcore.dto.request.AcceptedTermDTO;
import br.com.tlf.api.rest.creditcore.dto.request.ConsentRequestDTO;
import br.com.tlf.api.rest.creditcore.dto.request.GeolocationDTO;
import br.com.tlf.api.rest.creditcore.dto.request.SignatureDTO;
import br.com.tlf.core.domain.consent.AcceptedTerm;
import br.com.tlf.core.domain.consent.Geolocation;
import br.com.tlf.core.domain.consent.Signature;
import br.com.tlf.core.port.in.command.CreateConsentCommand;

public final class ConsentRequestDummies {

    /** Raw CPF value, kept in the clear throughout — must pass Cpf's checksum validation. */
    public static final String CPF_PLAIN = "52998224725";

    public static final String BEARER_TOKEN = "Bearer fake.jwt.token";
    public static final String CHANNEL_ID = "channel-1";
    public static final String CORRELATION_ID = "correlation-1";

    private ConsentRequestDummies() {
    }

    // ─── HTTP layer ───────────────────────────────────────────────────────────

    public static AcceptedTermDTO acceptedRevokedTermDTO() {
        return AcceptedTermDTO.builder()
                .termId(CreditTermDummies.REVOKED_TERM_ID.toString())
                .optIn(Boolean.TRUE)
                .build();
    }

    public static ConsentRequestDTO requestWithMandatoryTerm() {
        return ConsentRequestDTO.builder()
                .acceptedTerms(List.of(acceptedRevokedTermDTO()))
                .signature(signatureDTO())
                .build();
    }

    public static ConsentRequestDTO requestMissingMandatoryTerm() {
        return ConsentRequestDTO.builder()
                .acceptedTerms(Collections.emptyList())
                .signature(signatureDTO())
                .build();
    }

    public static SignatureDTO signatureDTO() {
        return SignatureDTO.builder()
                .ip("192.168.0.1")
                .userAgent("Mozilla/5.0")
                .deviceId("device-abc-001")
                .channel("MOBILE")
                .geolocation(GeolocationDTO.builder().lat("-23.5505").lon("-46.6333").build())
                .build();
    }

    // ─── domain layer ─────────────────────────────────────────────────────────

    public static AcceptedTerm acceptedRevokedTerm() {
        return new AcceptedTerm(CreditTermDummies.REVOKED_TERM_ID.toString(), Boolean.TRUE);
    }

    public static AcceptedTerm acceptedSoftTerm() {
        return new AcceptedTerm(CreditTermDummies.SOFT_TERM_ID.toString(), Boolean.TRUE);
    }

    public static Signature signature() {
        return new Signature("192.168.0.1", "Mozilla/5.0", "device-abc-001", "MOBILE",
                new Geolocation("-23.5505", "-46.6333"));
    }

    public static CreateConsentCommand commandWith(AcceptedTerm... acceptedTerms) {
        return new CreateConsentCommand(CreditTermDummies.CUSTOMER_ID, CORRELATION_ID, CHANNEL_ID,
                List.of(acceptedTerms), signature());
    }

    public static CreateConsentCommand commandWithMandatoryTerm() {
        return commandWith(acceptedRevokedTerm());
    }

    public static CreateConsentCommand commandWithTermId(String termId) {
        return new CreateConsentCommand(CreditTermDummies.CUSTOMER_ID, CORRELATION_ID, CHANNEL_ID,
                List.of(new AcceptedTerm(termId, Boolean.TRUE)), signature());
    }
}
