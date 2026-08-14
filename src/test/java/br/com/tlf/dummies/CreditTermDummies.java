package br.com.tlf.dummies;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.core.domain.vo.terms.ProductTypeEnum;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;

public class CreditTermDummies {

    // ─── constantes ──────────────────────────────────────────────────────────

    /** customerId agora é o CPF puro (não há mais hash) — deve bater com ConsentRequestDummies.CPF_PLAIN. */
    public static final String CUSTOMER_ID       = ConsentRequestDummies.CPF_PLAIN;
    public static final String PRODUCT  = ProductTypeEnum.CONSIGNADO_DATAPREV.toString();

    public static final UUID   REVOKED_TERM_ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final String REVOKED_TERM_CODE = "DATAPREV_AUTH";
    public static final String REVOKED_VERSION   = "2.0";

    public static final UUID   SOFT_TERM_ID      = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    public static final String SOFT_TERM_CODE    = "PRIVACY_POLICY";
    public static final String SOFT_VERSION      = "1.0";

    // ─── fixtures de catálogo ─────────────────────────────────────────────────

    /**
     * Termo com revoke_previous_versions = true (Hard Update).
     * Quando o cliente não assinou a versão vigente, deve constar em pendingTerms
     * e hasPendingMandatoryTerms = true.
     */
    public static TermsCatalogVO revokedTerm() {
        return TermsCatalogVO.builder()
                .id(REVOKED_TERM_ID)
                .product(PRODUCT)
                .termCode(REVOKED_TERM_CODE)
                .title("Autorização de Consulta Vínculos DATAPREV")
                .version(REVOKED_VERSION)
                .isMandatory(true)
                .revokePreviousVersions(true)
                .validityDays(30)
                .contentType("TEXTO")
                .contentSummary("Autorizo consulta à Dataprev")
                .contentText("Autorizo a instituição financeira a consultar meus vínculos empregatícios e margem consignável junto à Dataprev e aos órgãos competentes pelo prazo de 30 dias...")
                .startAt(Instant.now().minusSeconds(24 * 60 * 60))
                .build();
    }

    /**
     * Termo com revoke_previous_versions = false (Soft Update).
     * Convive com versões antigas; entra em pendingTerms apenas quando expirado
     * e hasPendingMandatoryTerms permanece false.
     */

    public static TermsCatalogVO softTerm() {
        return TermsCatalogVO.builder()
                .id(SOFT_TERM_ID)
                .product(PRODUCT)
                .termCode(SOFT_TERM_CODE)
                .title("Política de Privacidade Crédito")
                .version(SOFT_VERSION)
                .isMandatory(false)
                .revokePreviousVersions(false)
                .validityDays(90)
                .contentType("TEXTO")
                .contentSummary("Política de privacidade")
                .contentText("Seus dados serão tratados conforme a LGPD...")
                .startAt(Instant.now().minusSeconds(24 * 60 * 60))
                .build();
    }

    // ─── fixtures de resposta ─────────────────────────────────────────────────

    /** Resposta esperada quando todos os consentimentos estão ativos. */
    public static ActiveConsentResponseVO noTermsPendingResponse() {
        return ActiveConsentResponseVO.builder()
                .product(PRODUCT)
                .hasPendingMandatoryTerms(Boolean.FALSE)
                .pendingTerms(Collections.emptyList())
                .build();
    }

    /** Resposta esperada quando há termo revogado pendente. */
    public static ActiveConsentResponseVO revokedTermsPendingResponse() {
        return ActiveConsentResponseVO.builder()
                .product(PRODUCT)
                .hasPendingMandatoryTerms(Boolean.TRUE)
                .pendingTerms(List.of())   // validado pelo tamanho no teste
                .build();
    }

    /** Resposta esperada quando há apenas consentimento expirado (soft). */
    public static ActiveConsentResponseVO expiredTermsPendingResponse() {
        return ActiveConsentResponseVO.builder()
                .product(PRODUCT)
                .hasPendingMandatoryTerms(Boolean.FALSE)
                .pendingTerms(List.of())   // validado pelo tamanho no teste
                .build();
    }
}
