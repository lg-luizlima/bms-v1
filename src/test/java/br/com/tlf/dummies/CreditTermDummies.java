package br.com.tlf.dummies;

import java.time.Instant;
import java.util.UUID;

import br.com.tlf.core.domain.terms.ProductType;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;

public final class CreditTermDummies {

    /** customerId is the plain CPF — must match ConsentRequestDummies.CPF_PLAIN. */
    public static final String CUSTOMER_ID = ConsentRequestDummies.CPF_PLAIN;
    public static final String PRODUCT = ProductType.CONSIGNADO_DATAPREV.toString();

    public static final UUID REVOKED_TERM_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    public static final String REVOKED_TERM_CODE = "DATAPREV_AUTH";
    public static final String REVOKED_VERSION = "2.0";
    public static final String REVOKED_TEMPLATE_ID = "template-dataprev-auth-v2";

    public static final UUID SOFT_TERM_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    public static final String SOFT_TERM_CODE = "PRIVACY_POLICY";
    public static final String SOFT_VERSION = "1.0";

    private static final Instant TERM_STARTED_AT = Instant.parse("2020-01-01T00:00:00Z");

    private CreditTermDummies() {
    }

    /** Hard update: revokes previous versions, so only a consent on this exact version counts. */
    public static TermsCatalogEntry revokedTerm() {
        return TermsCatalogEntry.builder()
                .id(REVOKED_TERM_ID)
                .termCode(REVOKED_TERM_CODE)
                .title("Autorização de Consulta Vínculos DATAPREV")
                .version(REVOKED_VERSION)
                .isMandatory(true)
                .revokePreviousVersions(true)
                .validityDays(30)
                .contentType("TEXTO")
                .contentSummary("Autorizo consulta à Dataprev")
                .contentText("Autorizo a instituição financeira a consultar meus vínculos empregatícios...")
                .startAt(TERM_STARTED_AT)
                .requiresPostProcessing(true)
                .templateId(REVOKED_TEMPLATE_ID)
                .build();
    }

    /** Soft update: coexists with older versions, so any active consent on the code counts. */
    public static TermsCatalogEntry softTerm() {
        return TermsCatalogEntry.builder()
                .id(SOFT_TERM_ID)
                .termCode(SOFT_TERM_CODE)
                .title("Política de Privacidade Crédito")
                .version(SOFT_VERSION)
                .isMandatory(false)
                .revokePreviousVersions(false)
                .validityDays(90)
                .contentType("TEXTO")
                .contentSummary("Política de privacidade")
                .contentText("Seus dados serão tratados conforme a LGPD...")
                .startAt(TERM_STARTED_AT)
                .requiresPostProcessing(false)
                .build();
    }
}
