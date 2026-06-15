package br.com.tlf.dummies;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;

public class CustomerConsentDummies {

    // ─── constantes ──────────────────────────────────────────────────────────

    public static final UUID CONSENT_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    // ─── fixtures de consentimento ────────────────────────────────────────────

    /**
     * CustomerConsentVO para o termo obrigatório (REVOKED_TERM_CODE).
     * Representa o retorno esperado de CreditCoreMapper.toCustomerConsentVO()
     * para o fluxo happy-path.
     */
    public static CustomerConsentVO revokedTermConsent() {
        return CustomerConsentVO.builder()
                .id(CONSENT_ID)
                .cpfHash(CreditTermDummies.CUSTOMER_ID)
                .termCode(CreditTermDummies.REVOKED_TERM_CODE)
                .termId(CreditTermDummies.REVOKED_TERM_ID)
                .optIn(Boolean.TRUE)
                .acceptedAt(Instant.now())
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .auditDetails("{\"ip\":\"192.168.0.1\"}")
                .build();
    }
}
