package br.com.tlf.core.application.usecase;

import static br.com.tlf.dummies.ConsentRequestDummies.CHANNEL_ID;
import static br.com.tlf.dummies.ConsentRequestDummies.CORRELATION_ID;
import static br.com.tlf.dummies.CreditTermDummies.CUSTOMER_ID;
import static br.com.tlf.dummies.CreditTermDummies.PRODUCT;
import static br.com.tlf.dummies.CreditTermDummies.REVOKED_TERM_CODE;
import static br.com.tlf.dummies.CreditTermDummies.REVOKED_TERM_ID;
import static br.com.tlf.dummies.CreditTermDummies.SOFT_TERM_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.tlf.core.application.mapper.TermsCatalogMapper;
import br.com.tlf.core.application.mapper.TermsCatalogMapperImpl;
import br.com.tlf.core.domain.exception.ProductNotFoundException;
import br.com.tlf.core.domain.terms.PendingTerm;
import br.com.tlf.core.domain.terms.PendingTermsResult;
import br.com.tlf.core.domain.terms.TermsCatalog;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import br.com.tlf.core.port.in.command.PendingTermsQuery;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.dummies.CreditTermDummies;
import br.com.tlf.dummies.CustomerConsentDummies;

@ExtendWith(MockitoExtension.class)
class GetPendingTermsUseCaseTest {

    @Mock private TermsCatalogRepository termsCatalogRepository;
    @Mock private CustomerConsentRepository customerConsentRepository;

    private final TermsCatalogMapper termsCatalogMapper = new TermsCatalogMapperImpl();

    private GetPendingTermsUseCase underTest() {
        return new GetPendingTermsUseCase(termsCatalogRepository, customerConsentRepository, termsCatalogMapper);
    }

    private PendingTermsResult execute(String product) {
        return underTest().execute(new PendingTermsQuery(CUSTOMER_ID, product, CORRELATION_ID, CHANNEL_ID));
    }

    @Test
    void allTermsSigned_returnsNoPendingTerms() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT))
                .thenReturn(TermsCatalog.of(List.of(CreditTermDummies.revokedTerm(), CreditTermDummies.softTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(
                        REVOKED_TERM_CODE, CustomerConsentDummies.revokedTermConsent(),
                        SOFT_TERM_CODE, CustomerConsentDummies.softTermConsent()));

        PendingTermsResult result = execute(PRODUCT);

        assertThat(result.product()).isEqualTo(PRODUCT);
        assertThat(result.pendingTerms()).isEmpty();
        assertThat(result.hasPendingMandatoryTerms()).isFalse();
    }

    @Test
    void mandatoryTermNotSigned_flagsPendingMandatoryTerms() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT))
                .thenReturn(TermsCatalog.of(List.of(CreditTermDummies.revokedTerm(), CreditTermDummies.softTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(SOFT_TERM_CODE, CustomerConsentDummies.softTermConsent()));

        PendingTermsResult result = execute(PRODUCT);

        assertThat(result.hasPendingMandatoryTerms()).isTrue();
        assertThat(result.pendingTerms()).extracting(PendingTerm::termCode).containsExactly(REVOKED_TERM_CODE);
        assertThat(result.pendingTerms()).extracting(PendingTerm::termId)
                .containsExactly(REVOKED_TERM_ID.toString());
    }

    @Test
    void onlySoftTermNotSigned_doesNotFlagPendingMandatoryTerms() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT))
                .thenReturn(TermsCatalog.of(List.of(CreditTermDummies.revokedTerm(), CreditTermDummies.softTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(REVOKED_TERM_CODE, CustomerConsentDummies.revokedTermConsent()));

        PendingTermsResult result = execute(PRODUCT);

        assertThat(result.hasPendingMandatoryTerms()).isFalse();
        assertThat(result.pendingTerms()).extracting(PendingTerm::termCode).containsExactly(SOFT_TERM_CODE);
    }

    @Test
    void softTermSignedOnOlderVersion_isNotPending() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT))
                .thenReturn(TermsCatalog.of(List.of(CreditTermDummies.softTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(SOFT_TERM_CODE,
                        CustomerConsentDummies.consentWithTermId(UUID.randomUUID(), SOFT_TERM_CODE)));

        assertThat(execute(PRODUCT).pendingTerms()).isEmpty();
    }

    @Test
    void hardTermSignedOnOlderVersion_remainsPending() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT))
                .thenReturn(TermsCatalog.of(List.of(CreditTermDummies.revokedTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(REVOKED_TERM_CODE,
                        CustomerConsentDummies.consentWithTermId(UUID.randomUUID(), REVOKED_TERM_CODE)));

        assertThat(execute(PRODUCT).pendingTerms()).extracting(PendingTerm::termCode)
                .containsExactly(REVOKED_TERM_CODE);
    }

    @Test
    void duplicateVigentVersionsOfSameTermCode_reduceToLatest() {
        TermsCatalogEntry olderVersion = CreditTermDummies.revokedTerm().toBuilder()
                .id(UUID.randomUUID())
                .version("1.0")
                .build();
        when(termsCatalogRepository.findVigentTerms(PRODUCT))
                .thenReturn(TermsCatalog.of(List.of(olderVersion, CreditTermDummies.revokedTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of());

        PendingTermsResult result = execute(PRODUCT);

        assertThat(result.pendingTerms()).hasSize(1);
        assertThat(result.pendingTerms().getFirst().termId()).isEqualTo(REVOKED_TERM_ID.toString());
    }

    @Test
    void nullProduct_returnsResultWithoutProductAndWithoutThrowing() {
        when(termsCatalogRepository.findVigentTerms(null))
                .thenReturn(TermsCatalog.of(List.of(CreditTermDummies.revokedTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of());

        PendingTermsResult result = execute(null);

        assertThat(result.product()).isNull();
        assertThat(result.pendingTerms()).hasSize(1);
    }

    @Test
    void productGivenButNoVigentTerms_throwsProductNotFound() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT)).thenReturn(TermsCatalog.of(List.of()));

        assertThrows(ProductNotFoundException.class, () -> execute(PRODUCT));
    }

    @Test
    void nullProductAndNoVigentTerms_returnsEmptyResult() {
        when(termsCatalogRepository.findVigentTerms(null)).thenReturn(TermsCatalog.of(List.of()));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of());

        PendingTermsResult result = execute(null);

        assertThat(result.pendingTerms()).isEmpty();
        assertThat(result.hasPendingMandatoryTerms()).isFalse();
    }
}
