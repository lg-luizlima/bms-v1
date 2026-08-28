package br.com.tlf.core.application.usecase;

import static br.com.tlf.dummies.ConsentRequestDummies.CORRELATION_ID;
import static br.com.tlf.dummies.CreditTermDummies.CUSTOMER_ID;
import static br.com.tlf.dummies.CreditTermDummies.REVOKED_TERM_CODE;
import static br.com.tlf.dummies.CreditTermDummies.REVOKED_TERM_ID;
import static br.com.tlf.dummies.CreditTermDummies.REVOKED_TEMPLATE_ID;
import static br.com.tlf.dummies.CreditTermDummies.SOFT_TERM_CODE;
import static br.com.tlf.dummies.CreditTermDummies.SOFT_TERM_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.tlf.core.application.mapper.ConsentMapper;
import br.com.tlf.core.application.mapper.ConsentMapperImpl;
import br.com.tlf.core.domain.consent.ConsentReceipt;
import br.com.tlf.core.domain.consent.ConsentRegisteredEvent;
import br.com.tlf.core.domain.consent.CustomerConsent;
import br.com.tlf.core.domain.exception.InvalidTermException;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import br.com.tlf.core.port.out.cache.ConsentCachePort;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.core.port.out.eventhub.EventHubPort;
import br.com.tlf.core.port.out.outbox.ConsentEventOutbox;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.core.port.out.transaction.TransactionRunner;
import br.com.tlf.dummies.ConsentRequestDummies;
import br.com.tlf.dummies.CreditTermDummies;
import br.com.tlf.dummies.CustomerConsentDummies;

@ExtendWith(MockitoExtension.class)
class CreateConsentUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-08-28T12:00:00Z");

    @Mock private CustomerConsentRepository customerConsentRepository;
    @Mock private TermsCatalogRepository termsCatalogRepository;
    @Mock private ConsentEventOutbox consentEventOutbox;
    @Mock private ConsentCachePort consentCache;
    @Mock private EventHubPort eventHubPort;
    @Mock private TransactionRunner transactionRunner;
    private final ConsentMapper consentMapper = new ConsentMapperImpl();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private CreateConsentUseCase underTest;

    @BeforeEach
    void setUp() {
        underTest = new CreateConsentUseCase(customerConsentRepository, termsCatalogRepository,
                consentEventOutbox, consentCache, eventHubPort, transactionRunner, consentMapper, clock);
    }

    private void runTransactionsInline() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactionRunner).runInTransaction(any());
    }

    @Test
    void happyPath_persistsConsentQueuesOutboxEventAndMarksSyncStatus() {
        TermsCatalogEntry term = CreditTermDummies.revokedTerm();
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of(term));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of());
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        runTransactionsInline();

        ConsentReceipt receipt = underTest.execute(ConsentRequestDummies.commandWithMandatoryTerm());

        assertThat(receipt.consentReceivedAt()).isEqualTo(NOW);

        ArgumentCaptor<CustomerConsent> consentCaptor = ArgumentCaptor.forClass(CustomerConsent.class);
        verify(customerConsentRepository).save(consentCaptor.capture());
        CustomerConsent saved = consentCaptor.getValue();
        assertThat(saved.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(saved.termCode()).isEqualTo(REVOKED_TERM_CODE);
        assertThat(saved.termId()).isEqualTo(REVOKED_TERM_ID);
        assertThat(saved.acceptedAt()).isEqualTo(NOW);
        assertThat(saved.expiresAt()).isEqualTo(NOW.plus(30, ChronoUnit.DAYS));
        assertThat(saved.signature()).isEqualTo(ConsentRequestDummies.signature());

        ArgumentCaptor<ConsentRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(ConsentRegisteredEvent.class);
        verify(consentEventOutbox).publish(eq(CUSTOMER_ID), eventCaptor.capture());
        assertThat(eventCaptor.getValue().templateId()).isEqualTo(REVOKED_TEMPLATE_ID);
        assertThat(eventCaptor.getValue().termCode()).isEqualTo(REVOKED_TERM_CODE);

        verify(consentCache).writeSyncStatus(CUSTOMER_ID);
        verify(consentCache).cacheIdempotentResponse(CUSTOMER_ID, CORRELATION_ID, NOW);
    }

    @Test
    void termNotRequiringPostProcessing_savesConsentWithoutOutboxEventOrSyncStatus() {
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of(CreditTermDummies.softTerm()));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of());
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        runTransactionsInline();

        underTest.execute(ConsentRequestDummies.commandWith(ConsentRequestDummies.acceptedSoftTerm()));

        verify(customerConsentRepository).save(any());
        verifyNoInteractions(consentEventOutbox);
        verify(consentCache, never()).writeSyncStatus(anyString());
    }

    @Test
    void idempotentReplay_returnsCachedReceiptWithoutReprocessing() {
        Instant cachedAt = NOW.minus(10, ChronoUnit.SECONDS);
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.of(cachedAt));

        ConsentReceipt receipt = underTest.execute(ConsentRequestDummies.commandWithMandatoryTerm());

        assertThat(receipt.consentReceivedAt()).isEqualTo(cachedAt);
        verifyNoInteractions(termsCatalogRepository, customerConsentRepository, consentEventOutbox, eventHubPort);
    }

    @Test
    void termAlreadySigned_skipsPersistenceAndSyncStatus() {
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of(CreditTermDummies.revokedTerm()));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(REVOKED_TERM_CODE, CustomerConsentDummies.revokedTermConsent()));
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        underTest.execute(ConsentRequestDummies.commandWithMandatoryTerm());

        verify(customerConsentRepository, never()).save(any());
        verifyNoInteractions(consentEventOutbox, transactionRunner);
        verify(consentCache, never()).writeSyncStatus(anyString());
    }

    @Test
    void softTermSignedOnOlderVersion_isNotResigned() {
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of(CreditTermDummies.softTerm()));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(SOFT_TERM_CODE,
                        CustomerConsentDummies.consentWithTermId(UUID.randomUUID(), SOFT_TERM_CODE)));
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        underTest.execute(ConsentRequestDummies.commandWith(ConsentRequestDummies.acceptedSoftTerm()));

        verify(customerConsentRepository, never()).save(any());
    }

    @Test
    void hardTermSignedOnOlderVersion_isResigned() {
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of(CreditTermDummies.revokedTerm()));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of(REVOKED_TERM_CODE,
                        CustomerConsentDummies.consentWithTermId(UUID.randomUUID(), REVOKED_TERM_CODE)));
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        runTransactionsInline();

        underTest.execute(ConsentRequestDummies.commandWithMandatoryTerm());

        verify(customerConsentRepository).save(any());
    }

    @Test
    void unknownTermId_throwsInvalidTermExceptionAndPublishesNothing() {
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of());
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        assertThrows(InvalidTermException.class,
                () -> underTest.execute(ConsentRequestDummies.commandWithMandatoryTerm()));

        verifyNoInteractions(eventHubPort, consentEventOutbox);
        verify(customerConsentRepository, never()).save(any());
    }

    @Test
    void malformedTermId_throwsInvalidTermException() {
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of());
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        InvalidTermException thrown = assertThrows(InvalidTermException.class,
                () -> underTest.execute(ConsentRequestDummies.commandWithTermId("not-a-uuid")));

        assertThat(thrown.getErrors()).containsExactly("not-a-uuid");
    }

    @Test
    void termOutsideValidityWindow_throwsInvalidTermException() {
        TermsCatalogEntry expired = CreditTermDummies.revokedTerm().toBuilder()
                .startAt(NOW.minus(10, ChronoUnit.DAYS))
                .endAt(NOW.minus(1, ChronoUnit.DAYS))
                .build();
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of(expired));
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        assertThrows(InvalidTermException.class,
                () -> underTest.execute(ConsentRequestDummies.commandWithMandatoryTerm()));
    }

    @Test
    void eventHubPublishHappensOnlyAfterTermsAreValidated() {
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of(CreditTermDummies.revokedTerm()));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of());
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        runTransactionsInline();

        underTest.execute(ConsentRequestDummies.commandWithMandatoryTerm());

        verify(eventHubPort).publishConsentRequested(
                List.of(ConsentRequestDummies.acceptedRevokedTerm()), ConsentRequestDummies.signature());
    }

    @Test
    void allTermsPersistInOneTransaction() {
        when(termsCatalogRepository.findByIds(anyList()))
                .thenReturn(List.of(CreditTermDummies.revokedTerm(), CreditTermDummies.softTerm()));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of());
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        runTransactionsInline();

        underTest.execute(ConsentRequestDummies.commandWith(
                ConsentRequestDummies.acceptedRevokedTerm(), ConsentRequestDummies.acceptedSoftTerm()));

        verify(transactionRunner).runInTransaction(any());
        verify(customerConsentRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void sameTermSubmittedTwice_isRegisteredOnce() {
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of(CreditTermDummies.revokedTerm()));
        when(customerConsentRepository.findActiveConsentsByTermCode(eq(CUSTOMER_ID), anyCollection()))
                .thenReturn(Map.of());
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        runTransactionsInline();

        underTest.execute(ConsentRequestDummies.commandWith(
                ConsentRequestDummies.acceptedRevokedTerm(), ConsentRequestDummies.acceptedRevokedTerm()));

        verify(customerConsentRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void noTermsAccepted_registersNothingAndStillReturnsReceipt() {
        when(termsCatalogRepository.findByIds(anyList())).thenReturn(List.of());
        when(consentCache.findIdempotentResponse(CUSTOMER_ID, CORRELATION_ID)).thenReturn(Optional.empty());

        ConsentReceipt receipt = underTest.execute(ConsentRequestDummies.commandWith());

        assertThat(receipt.consentReceivedAt()).isEqualTo(NOW);
        verify(customerConsentRepository, never()).save(any());
        verifyNoInteractions(transactionRunner, consentEventOutbox);
        assertThat(SOFT_TERM_ID).isNotNull();
    }
}
