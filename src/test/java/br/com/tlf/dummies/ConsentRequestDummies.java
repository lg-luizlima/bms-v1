package br.com.tlf.dummies;

import java.util.Collections;
import java.util.List;

import br.com.tlf.core.domain.vo.consent.AcceptedTermVO;
import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.consent.GeolocationVO;
import br.com.tlf.core.domain.vo.consent.SignatureVO;
import br.com.tlf.core.port.in.dto.request.AcceptedTermDTO;
import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.request.GeolocationDTO;
import br.com.tlf.core.port.in.dto.request.SignatureDTO;

public class ConsentRequestDummies {

    // ─── constantes ──────────────────────────────────────────────────────────

    /** Raw CPF value that JwtTokenUtils.cpfToken() returns (mocked) — must pass CustomerIdResolver's checksum validation. */
    public static final String CPF_PLAIN = "52998224725";

    /** Authorization header value passed to the service. */
    public static final String BEARER_TOKEN = "Bearer fake.jwt.token";

    // ─── AcceptedTerm fixtures ────────────────────────────────────────────────

    public static AcceptedTermDTO acceptedRevokedTermDTO() {
        return AcceptedTermDTO.builder()
                .termId(CreditTermDummies.REVOKED_TERM_ID.toString())
                .optIn(Boolean.TRUE)
                .build();
    }

    public static AcceptedTermVO acceptedRevokedTermVO() {
        return AcceptedTermVO.builder()
                .termId(CreditTermDummies.REVOKED_TERM_ID.toString())
                .optIn(Boolean.TRUE)
                .build();
    }

    // ─── ConsentRequestDTO fixtures ───────────────────────────────────────────

    /** Happy-path request: the revoked/hard term (REVOKED_TERM_ID) is included. */
    public static ConsentRequestDTO requestWithMandatoryTerm() {
        return ConsentRequestDTO.builder()
                .acceptedTerms(List.of(acceptedRevokedTermDTO()))
                .signature(signatureDTO())
                .build();
    }

    /** Request with an empty acceptedTerms list. */
    public static ConsentRequestDTO requestMissingMandatoryTerm() {
        return ConsentRequestDTO.builder()
                .acceptedTerms(Collections.emptyList())
                .signature(signatureDTO())
                .build();
    }

    // ─── ConsentRequestVO fixtures ────────────────────────────────────────────

    /**
     * VO equivalent of requestWithMandatoryTerm().
     * cpf field holds the cpf (CUSTOMER_ID) as passed to the mapper.
     */
    public static ConsentRequestVO consentRequestVO() {
        return ConsentRequestVO.builder()
                .customerId(CreditTermDummies.CUSTOMER_ID)
                .acceptedTerms(List.of(acceptedRevokedTermVO()))
                .signature(signatureVO())
                .build();
    }

    /** VO with an empty acceptedTerms list. */
    public static ConsentRequestVO consentRequestVOMissingTerm() {
        return ConsentRequestVO.builder()
                .customerId(CreditTermDummies.CUSTOMER_ID)
                .acceptedTerms(Collections.emptyList())
                .signature(signatureVO())
                .build();
    }

    // ─── Signature fixtures ───────────────────────────────────────────────────

    public static SignatureDTO signatureDTO() {
        return SignatureDTO.builder()
                .ip("192.168.0.1")
                .userAgent("Mozilla/5.0")
                .deviceId("device-abc-001")
                .channel("MOBILE")
                .geolocation(GeolocationDTO.builder()
                        .lat("-23.5505")
                        .lon("-46.6333")
                        .build())
                .build();
    }

    public static SignatureVO signatureVO() {
        return SignatureVO.builder()
                .ip("192.168.0.1")
                .userAgent("Mozilla/5.0")
                .deviceId("device-abc-001")
                .channel("MOBILE")
                .geolocation(GeolocationVO.builder()
                        .lat("-23.5505")
                        .lon("-46.6333")
                        .build())
                .build();
    }
}
