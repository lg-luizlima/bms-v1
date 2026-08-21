package br.com.tlf.core.application.service.creditcore;

import static br.com.tlf.dummies.CreditTermDummies.CUSTOMER_ID;
import static br.com.tlf.dummies.CreditTermDummies.PRODUCT;
import static br.com.tlf.dummies.CreditTermDummies.REVOKED_TERM_CODE;
import static br.com.tlf.dummies.CreditTermDummies.REVOKED_TERM_ID;
import static br.com.tlf.dummies.CreditTermDummies.SOFT_TERM_CODE;
import static br.com.tlf.dummies.CreditTermDummies.SOFT_TERM_ID;
import static br.com.tlf.dummies.ConsentRequestDummies.BEARER_TOKEN;
import static br.com.tlf.dummies.ConsentRequestDummies.CPF_PLAIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.tlf.core.application.mapper.creditcore.CreditCoreMapper;
import br.com.tlf.core.application.mapper.outboxeventqueue.OutBoxEventQueueMapper;
import br.com.tlf.core.application.service.customer.CustomerIdResolver;
import br.com.tlf.core.domain.exception.MandatoryTermNotAcceptedException;
import br.com.tlf.core.domain.vo.OutBoxEventQueueVO;
import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.core.domain.vo.terms.PendingTermVO;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.core.port.in.dto.response.ConsentResponseDTO;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.core.port.out.eventhub.EventHubPort;
import br.com.tlf.core.port.out.outbox.OutboxEventQueueRepository;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.dummies.ConsentRequestDummies;
import br.com.tlf.dummies.CreditTermDummies;
import br.com.tlf.dummies.CustomerConsentDummies;
import br.com.tlf.shared.observability.ObservabilityPiiProperties;
import br.com.tlf.shared.util.JsonSerializer;
import br.com.tlf.shared.util.jwt.JwtTokenUtils;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

@ExtendWith(MockitoExtension.class)
class CreditCoreServiceImplTest {

    @Mock private CustomerConsentRepository customerConsentRepository;
    @Mock private TermsCatalogRepository termsCatalogRepository;
    @Mock private OutboxEventQueueRepository outboxEventQueueRepository;
    @Mock private CreditCoreMapper creditCoreMapper;
    @Mock private OutBoxEventQueueMapper outBoxEventQueueMapper;
    @Mock private JsonSerializer jsonSerializer;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private EventHubPort eventHubPort;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private Tracer tracer;
    @Mock private Propagator propagator;
    // Registro real (sem listeners) em vez de mock: Observation.createNotStarted(...) acessa
    // observationConfig() internamente, o que um mock não-stubado não suportaria sem NPE.
    @Spy private ObservationRegistry observationRegistry = ObservationRegistry.create();
    @Spy private ObservabilityPiiProperties observabilityPiiProperties = new ObservabilityPiiProperties();
    @Spy private CustomerIdResolver customerIdResolver = new CustomerIdResolver();
    @Mock private ConsentIdempotencyChecker consentIdempotencyChecker;

    @InjectMocks
    private CreditCoreServiceImpl underTest;

    @SuppressWarnings("unchecked")
    private void stubTransactionTemplateToRunLambda() {
        doAnswer(invocation -> {
            invocation.<Consumer<TransactionStatus>>getArgument(0).accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    // ─── createConsent ────────────────────────────────────────────────────────

    @Test
    void createConsent_happyPath_savesConsentAndSetsRedis() throws Exception {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        ConsentRequestVO requestVO = ConsentRequestDummies.consentRequestVO();
        CustomerConsentVO consentVO = CustomerConsentDummies.revokedTermConsent();
        OutBoxEventQueueVO outboxVO = OutBoxEventQueueVO.builder()
                .aggregateId(CUSTOMER_ID)
                .payload("{}")
                .build();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            stubTransactionTemplateToRunLambda();

            when(creditCoreMapper.toVO(ConsentRequestDummies.requestWithMandatoryTerm(), CUSTOMER_ID))
                    .thenReturn(requestVO);
            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(revokedTerm));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, REVOKED_TERM_CODE))
                    .thenReturn(null);
            when(creditCoreMapper.toCustomerConsentVO(
                    eq(CUSTOMER_ID), eq(requestVO),
                    eq(ConsentRequestDummies.acceptedRevokedTermVO()), eq(revokedTerm)))
                    .thenReturn(consentVO);
            when(jsonSerializer.toJson(any())).thenReturn("{}");
            when(outBoxEventQueueMapper.toVO(eq(CUSTOMER_ID), any(), any()))
                    .thenReturn(outboxVO);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            ConsentResponseDTO response = underTest.createConsent(BEARER_TOKEN, ConsentRequestDummies.requestWithMandatoryTerm(), "channel-1", "correlation-1", "customer-1");

            assertThat(response).isNotNull();
            assertThat(response.getConsentReceivedAt()).isNotNull();
            verify(eventHubPort).sendEvent(any());
            verify(customerConsentRepository).saveConsent(consentVO);
            verify(outboxEventQueueRepository).save(outboxVO);
            verify(valueOperations).set("sync_status:" + CUSTOMER_ID, "PROCESSING", 86400L, TimeUnit.SECONDS);
            verify(consentIdempotencyChecker).cacheResponse(eq(CUSTOMER_ID), eq("correlation-1"), any());
        }
    }

    @Test
    void createConsent_idempotentReplay_returnsCachedResponseWithoutReprocessing() {
        Instant cachedConsentReceivedAt = Instant.parse("2026-08-13T10:00:00Z");

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            when(consentIdempotencyChecker.findCachedResponse(CUSTOMER_ID, "correlation-1"))
                    .thenReturn(Optional.of(cachedConsentReceivedAt));

            ConsentResponseDTO response = underTest.createConsent(BEARER_TOKEN, ConsentRequestDummies.requestWithMandatoryTerm(), "channel-1", "correlation-1", "customer-1");

            assertThat(response.getConsentReceivedAt()).isEqualTo(cachedConsentReceivedAt);
            verify(eventHubPort, never()).sendEvent(any());
            verify(termsCatalogRepository, never()).findLatestActiveByProduct(any());
            verify(customerConsentRepository, never()).saveConsent(any());
            verify(outboxEventQueueRepository, never()).save(any());
            verify(redisTemplate, never()).opsForValue();
            verify(consentIdempotencyChecker, never()).cacheResponse(any(), any(), any());
        }
    }

    @Test
    void createConsent_mandatoryTermMissing_throwsMandatoryTermNotAcceptedException() {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        ConsentRequestVO requestVO = ConsentRequestDummies.consentRequestVOMissingTerm();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            when(creditCoreMapper.toVO(ConsentRequestDummies.requestMissingMandatoryTerm(), CUSTOMER_ID))
                    .thenReturn(requestVO);
            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(revokedTerm));

            MandatoryTermNotAcceptedException ex = assertThrows(
                    MandatoryTermNotAcceptedException.class,
                    () -> underTest.createConsent(BEARER_TOKEN, ConsentRequestDummies.requestMissingMandatoryTerm(), "channel-1", "correlation-1", "customer-1"));

            assertThat(ex.getErrors()).hasSize(1);
            assertThat(ex.getErrors().get(0)).contains(REVOKED_TERM_CODE);

            verify(eventHubPort).sendEvent(any());
            verify(customerConsentRepository, never()).saveConsent(any());
            verify(outboxEventQueueRepository, never()).save(any());
            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Test
    void createConsent_termAlreadyActive_skipsConsentSaveButStillSetsRedis() {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        ConsentRequestVO requestVO = ConsentRequestDummies.consentRequestVO();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            when(creditCoreMapper.toVO(ConsentRequestDummies.requestWithMandatoryTerm(), CUSTOMER_ID))
                    .thenReturn(requestVO);
            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(revokedTerm));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, REVOKED_TERM_CODE))
                    .thenReturn(CustomerConsentDummies.consentWithTermId(REVOKED_TERM_ID, REVOKED_TERM_CODE));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            ConsentResponseDTO response = underTest.createConsent(BEARER_TOKEN, ConsentRequestDummies.requestWithMandatoryTerm(), "channel-1", "correlation-1", "customer-1");

            assertThat(response).isNotNull();
            assertThat(response.getConsentReceivedAt()).isNotNull();
            verify(eventHubPort).sendEvent(any());
            verify(customerConsentRepository, never()).saveConsent(any());
            verify(outboxEventQueueRepository, never()).save(any());
            verify(valueOperations).set("sync_status:" + CUSTOMER_ID, "PROCESSING", 86400L, TimeUnit.SECONDS);
        }
    }

    // ─── getPendingTerms ──────────────────────────────────────────────────────

    @Test
    void getPendingTerms_allTermsSigned_returnsEmptyPendingTermsAndNoMandatoryFlag() {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        TermsCatalogVO softTerm = CreditTermDummies.softTerm();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(revokedTerm, softTerm));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, REVOKED_TERM_CODE))
                    .thenReturn(CustomerConsentDummies.consentWithTermId(REVOKED_TERM_ID, REVOKED_TERM_CODE));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, SOFT_TERM_CODE))
                    .thenReturn(CustomerConsentDummies.consentWithTermId(SOFT_TERM_ID, SOFT_TERM_CODE));
            when(creditCoreMapper.toPendingTermVO(anyList()))
                    .thenReturn(Collections.emptyList());

            ArgumentCaptor<ActiveConsentResponseVO> voCaptor =
                    ArgumentCaptor.forClass(ActiveConsentResponseVO.class);
            when(creditCoreMapper.toActiveConsentResponseDTO(voCaptor.capture()))
                    .thenReturn(ActiveConsentResponseDTO.builder().build());

            underTest.getPendingTerms(BEARER_TOKEN, PRODUCT, "channel-1", "correlation-1", "customer-1");

            ActiveConsentResponseVO captured = voCaptor.getValue();
            assertThat(captured.getProduct()).isEqualTo(PRODUCT);
            assertThat(captured.getHasPendingMandatoryTerms()).isFalse();
            assertThat(captured.getPendingTerms()).isEmpty();
        }
    }

    @Test
    void getPendingTerms_mandatoryTermNotSigned_returnsHasPendingMandatoryTermsTrue() {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        TermsCatalogVO softTerm = CreditTermDummies.softTerm();
        PendingTermVO pendingMandatoryTermVO = PendingTermVO.builder()
                .termId(REVOKED_TERM_ID.toString())
                .termCode(REVOKED_TERM_CODE)
                .contentSummary(revokedTerm.getContentSummary())
                .isMandatory(true)
                .build();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(revokedTerm, softTerm));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, REVOKED_TERM_CODE))
                    .thenReturn(null);
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, SOFT_TERM_CODE))
                    .thenReturn(CustomerConsentDummies.consentWithTermId(SOFT_TERM_ID, SOFT_TERM_CODE));
            when(creditCoreMapper.toPendingTermVO(anyList()))
                    .thenReturn(List.of(pendingMandatoryTermVO));

            ArgumentCaptor<ActiveConsentResponseVO> voCaptor =
                    ArgumentCaptor.forClass(ActiveConsentResponseVO.class);
            when(creditCoreMapper.toActiveConsentResponseDTO(voCaptor.capture()))
                    .thenReturn(ActiveConsentResponseDTO.builder().build());

            underTest.getPendingTerms(BEARER_TOKEN, PRODUCT, "channel-1", "correlation-1", "customer-1");

            ActiveConsentResponseVO captured = voCaptor.getValue();
            assertThat(captured.getHasPendingMandatoryTerms()).isTrue();
            assertThat(captured.getPendingTerms()).hasSize(1);
        }
    }

    @Test
    void getPendingTerms_onlySoftTermNotSigned_returnsHasPendingMandatoryTermsFalse() {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        TermsCatalogVO softTerm = CreditTermDummies.softTerm();
        PendingTermVO pendingSoftTermVO = PendingTermVO.builder()
                .termId(SOFT_TERM_ID.toString())
                .termCode(SOFT_TERM_CODE)
                .contentSummary(softTerm.getContentSummary())
                .isMandatory(false)
                .build();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(revokedTerm, softTerm));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, REVOKED_TERM_CODE))
                    .thenReturn(CustomerConsentDummies.consentWithTermId(REVOKED_TERM_ID, REVOKED_TERM_CODE));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, SOFT_TERM_CODE))
                    .thenReturn(null);
            when(creditCoreMapper.toPendingTermVO(anyList()))
                    .thenReturn(List.of(pendingSoftTermVO));

            ArgumentCaptor<ActiveConsentResponseVO> voCaptor =
                    ArgumentCaptor.forClass(ActiveConsentResponseVO.class);
            when(creditCoreMapper.toActiveConsentResponseDTO(voCaptor.capture()))
                    .thenReturn(ActiveConsentResponseDTO.builder().build());

            underTest.getPendingTerms(BEARER_TOKEN, PRODUCT, "channel-1", "correlation-1", "customer-1");

            ActiveConsentResponseVO captured = voCaptor.getValue();
            assertThat(captured.getHasPendingMandatoryTerms()).isFalse();
            assertThat(captured.getPendingTerms()).hasSize(1);
        }
    }

    @Test
    void getPendingTerms_softTermSignedWithOlderVersion_isNotPending() {
        TermsCatalogVO softTerm = CreditTermDummies.softTerm();
        UUID olderSoftTermId = UUID.randomUUID();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(softTerm));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, SOFT_TERM_CODE))
                    .thenReturn(CustomerConsentDummies.consentWithTermId(olderSoftTermId, SOFT_TERM_CODE));
            when(creditCoreMapper.toPendingTermVO(anyList()))
                    .thenReturn(Collections.emptyList());

            ArgumentCaptor<ActiveConsentResponseVO> voCaptor =
                    ArgumentCaptor.forClass(ActiveConsentResponseVO.class);
            when(creditCoreMapper.toActiveConsentResponseDTO(voCaptor.capture()))
                    .thenReturn(ActiveConsentResponseDTO.builder().build());

            underTest.getPendingTerms(BEARER_TOKEN, PRODUCT, "channel-1", "correlation-1", "customer-1");

            ActiveConsentResponseVO captured = voCaptor.getValue();
            assertThat(captured.getHasPendingMandatoryTerms()).isFalse();
            assertThat(captured.getPendingTerms()).isEmpty();
        }
    }

    @Test
    void getPendingTerms_hardTermSignedWithOlderVersion_remainsPending() {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        UUID olderRevokedTermId = UUID.randomUUID();
        PendingTermVO pendingMandatoryTermVO = PendingTermVO.builder()
                .termId(REVOKED_TERM_ID.toString())
                .termCode(REVOKED_TERM_CODE)
                .contentSummary(revokedTerm.getContentSummary())
                .isMandatory(true)
                .build();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(revokedTerm));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, REVOKED_TERM_CODE))
                    .thenReturn(CustomerConsentDummies.consentWithTermId(olderRevokedTermId, REVOKED_TERM_CODE));
            when(creditCoreMapper.toPendingTermVO(anyList()))
                    .thenReturn(List.of(pendingMandatoryTermVO));

            ArgumentCaptor<ActiveConsentResponseVO> voCaptor =
                    ArgumentCaptor.forClass(ActiveConsentResponseVO.class);
            when(creditCoreMapper.toActiveConsentResponseDTO(voCaptor.capture()))
                    .thenReturn(ActiveConsentResponseDTO.builder().build());

            underTest.getPendingTerms(BEARER_TOKEN, PRODUCT, "channel-1", "correlation-1", "customer-1");

            ActiveConsentResponseVO captured = voCaptor.getValue();
            assertThat(captured.getHasPendingMandatoryTerms()).isTrue();
            assertThat(captured.getPendingTerms()).hasSize(1);
        }
    }

    @Test
    void getPendingTerms_duplicateVigenteVersionsSameTermCode_reducesToLatest() {
        TermsCatalogVO softTerm = CreditTermDummies.softTerm();
        TermsCatalogVO olderVersion = TermsCatalogVO.builder()
                .id(UUID.randomUUID())
                .product(softTerm.getProduct())
                .termCode(softTerm.getTermCode())
                .title(softTerm.getTitle())
                .version("1.0")
                .isMandatory(softTerm.getIsMandatory())
                .revokePreviousVersions(softTerm.getRevokePreviousVersions())
                .contentSummary(softTerm.getContentSummary())
                .build();
        TermsCatalogVO newerVersion = TermsCatalogVO.builder()
                .id(UUID.randomUUID())
                .product(softTerm.getProduct())
                .termCode(softTerm.getTermCode())
                .title(softTerm.getTitle())
                .version("2.0")
                .isMandatory(softTerm.getIsMandatory())
                .revokePreviousVersions(softTerm.getRevokePreviousVersions())
                .contentSummary(softTerm.getContentSummary())
                .build();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);

            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(olderVersion, newerVersion));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, SOFT_TERM_CODE))
                    .thenReturn(null);

            ArgumentCaptor<List<TermsCatalogVO>> pendingTermsCaptor = ArgumentCaptor.forClass(List.class);
            when(creditCoreMapper.toPendingTermVO(pendingTermsCaptor.capture()))
                    .thenReturn(Collections.emptyList());
            when(creditCoreMapper.toActiveConsentResponseDTO(any()))
                    .thenReturn(ActiveConsentResponseDTO.builder().build());

            underTest.getPendingTerms(BEARER_TOKEN, PRODUCT, "channel-1", "correlation-1", "customer-1");

            List<TermsCatalogVO> pendingTerms = pendingTermsCaptor.getValue();
            assertThat(pendingTerms).hasSize(1);
            assertThat(pendingTerms.get(0).getVersion()).isEqualTo("2.0");
        }
    }
}
