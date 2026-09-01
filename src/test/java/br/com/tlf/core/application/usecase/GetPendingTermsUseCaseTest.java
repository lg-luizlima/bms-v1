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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

    private static final String OTHER_PRODUCT = "CREDITO_PESSOAL";

    @Mock private TermsCatalogRepository termsCatalogRepository;
    @Mock private CustomerConsentRepository customerConsentRepository;

    private final TermsCatalogMapper termsCatalogMapper = new TermsCatalogMapperImpl();

    private GetPendingTermsUseCase underTest() {
        return new GetPendingTermsUseCase(termsCatalogRepository, customerConsentRepository, termsCatalogMapper);
    }

    private List<PendingTermsResult> execute(String product) {
        return underTest().execute(new PendingTermsQuery(CUSTOMER_ID, product, CORRELATION_ID, CHANNEL_ID));
    }

    private PendingTermsResult executeSingle(String product) {
        List<PendingTermsResult> results = execute(product);
        assertThat(results).hasSize(1);
        return results.getFirst();
    }

    @Test
    void allTermsSigned_returnsNoPendingTerms() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT))
                .thenReturn(TermsCatalog.of(List.of(CreditTermDummies.revokedTerm(), CreditTermDummies.softTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(
                        REVOKED_TERM_CODE, CustomerConsentDummies.revokedTermConsent(),
                        SOFT_TERM_CODE, CustomerConsentDummies.softTermConsent()));

        PendingTermsResult result = executeSingle(PRODUCT);

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

        PendingTermsResult result = executeSingle(PRODUCT);

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

        PendingTermsResult result = executeSingle(PRODUCT);

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

        assertThat(executeSingle(PRODUCT).pendingTerms()).isEmpty();
    }

    @Test
    void hardTermSignedOnOlderVersion_remainsPending() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT))
                .thenReturn(TermsCatalog.of(List.of(CreditTermDummies.revokedTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(REVOKED_TERM_CODE,
                        CustomerConsentDummies.consentWithTermId(UUID.randomUUID(), REVOKED_TERM_CODE)));

        assertThat(executeSingle(PRODUCT).pendingTerms()).extracting(PendingTerm::termCode)
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

        PendingTermsResult result = executeSingle(PRODUCT);

        assertThat(result.pendingTerms()).hasSize(1);
        assertThat(result.pendingTerms().getFirst().termId()).isEqualTo(REVOKED_TERM_ID.toString());
    }

    @Test
    void productGivenButNoVigentTerms_throwsProductNotFound() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT)).thenReturn(TermsCatalog.of(List.of()));

        assertThrows(ProductNotFoundException.class, () -> execute(PRODUCT));
    }

    @Test
    void productGiven_zeroPendingTerms_stillReturnsSingleObjectWithEmptyPendingTerms() {
        when(termsCatalogRepository.findVigentTerms(PRODUCT))
                .thenReturn(TermsCatalog.of(List.of(CreditTermDummies.revokedTerm(), CreditTermDummies.softTerm())));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(
                        REVOKED_TERM_CODE, CustomerConsentDummies.revokedTermConsent(),
                        SOFT_TERM_CODE, CustomerConsentDummies.softTermConsent()));

        List<PendingTermsResult> results = execute(PRODUCT);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().product()).isEqualTo(PRODUCT);
        assertThat(results.getFirst().pendingTerms()).isEmpty();
    }

    @Test
    void productOmitted_pendingInSomeButNotAllProducts_returnsOneEntryPerProductWithPending() {
        TermsCatalogEntry productAOnlyTerm = CreditTermDummies.revokedTerm();
        TermsCatalogEntry productBOnlyTerm = CreditTermDummies.softTerm();
        TermsCatalogEntry productCFullySignedTerm = CreditTermDummies.revokedTerm().toBuilder()
                .id(UUID.randomUUID())
                .termCode("ALREADY_SIGNED_TERM")
                .build();

        when(termsCatalogRepository.findVigentTermsGroupedByProduct()).thenReturn(Map.of(
                PRODUCT, TermsCatalog.of(List.of(productAOnlyTerm)),
                OTHER_PRODUCT, TermsCatalog.of(List.of(productBOnlyTerm)),
                "FULLY_SIGNED_PRODUCT", TermsCatalog.of(List.of(productCFullySignedTerm))));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of("ALREADY_SIGNED_TERM",
                        CustomerConsentDummies.consentWithTermId(productCFullySignedTerm.id(), "ALREADY_SIGNED_TERM")));

        List<PendingTermsResult> results = execute(null);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(PendingTermsResult::product).containsExactlyInAnyOrder(PRODUCT, OTHER_PRODUCT);
        verify(customerConsentRepository, times(1)).findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection());
    }

    @Test
    void productOmitted_noVigentTermsAnywhere_returnsEmptyListNotException() {
        when(termsCatalogRepository.findVigentTermsGroupedByProduct()).thenReturn(Map.of());

        List<PendingTermsResult> results = execute(null);

        assertThat(results).isEmpty();
        verifyNoInteractions(customerConsentRepository);
    }

    @Test
    void productOmitted_allProductsFullySigned_returnsEmptyList() {
        when(termsCatalogRepository.findVigentTermsGroupedByProduct())
                .thenReturn(Map.of(PRODUCT, TermsCatalog.of(List.of(CreditTermDummies.revokedTerm()))));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(REVOKED_TERM_CODE, CustomerConsentDummies.revokedTermConsent()));

        assertThat(execute(null)).isEmpty();
        verify(termsCatalogRepository, never()).findVigentTerms(null);
    }
}
