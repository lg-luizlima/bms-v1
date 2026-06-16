package br.com.tlf.core.application.service.creditcore;

import static br.com.tlf.dummies.CreditTermDummies.CUSTOMER_ID;
import static br.com.tlf.dummies.CreditTermDummies.PRODUCT;
import static br.com.tlf.dummies.CreditTermDummies.REVOKED_TERM_CODE;
import static br.com.tlf.dummies.CreditTermDummies.REVOKED_TERM_ID;
import static br.com.tlf.dummies.CreditTermDummies.SOFT_TERM_CODE;
import static br.com.tlf.dummies.CreditTermDummies.SOFT_TERM_ID;
import static br.com.tlf.dummies.ConsentRequestDummies.BEARER_TOKEN;
import static br.com.tlf.dummies.ConsentRequestDummies.CPF_PLAIN;
import static br.com.tlf.shared.constants.ApplicationConstants.MAX_VALIDITY_DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.tlf.core.application.mapper.creditcore.CreditCoreMapper;
import br.com.tlf.core.application.mapper.outboxeventqueue.OutBoxEventQueueMapper;
import br.com.tlf.core.domain.exception.MandatoryTermNotAcceptedException;
import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.core.domain.vo.terms.PendingTermVO;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.dummies.ConsentRequestDummies;
import br.com.tlf.dummies.CreditTermDummies;
import br.com.tlf.dummies.CustomerConsentDummies;
import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueJpaEntity;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.OutboxEventQueueJpaRepository;
import br.com.tlf.shared.util.HmacUtils;
import br.com.tlf.shared.util.jwt.JwtTokenUtils;

@ExtendWith(MockitoExtension.class)
class CreditCoreServiceImplTest {

    @Mock private CustomerConsentRepository customerConsentRepository;
    @Mock private TermsCatalogRepository termsCatalogRepository;
    @Mock private OutboxEventQueueJpaRepository outboxEventQueueRepository;
    @Mock private CreditCoreMapper creditCoreMapper;
    @Mock private OutBoxEventQueueMapper outBoxEventQueueMapper;
    @Mock private ObjectMapper objectMapper;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CreditCoreServiceImpl underTest;

    // ─── createConsent ────────────────────────────────────────────────────────

    @Test
    void createConsent_happyPath_savesConsentAndSetsRedis() throws Exception {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        ConsentRequestVO requestVO = ConsentRequestDummies.consentRequestVO();
        CustomerConsentVO consentVO = CustomerConsentDummies.revokedTermConsent();
        OutboxEventQueueJpaEntity outboxEntity = mock(OutboxEventQueueJpaEntity.class);

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class);
             MockedStatic<HmacUtils> hmacMock = mockStatic(HmacUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);
            hmacMock.when(() -> HmacUtils.generateHmacSha256(CPF_PLAIN)).thenReturn(CUSTOMER_ID);

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
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(outBoxEventQueueMapper.toEntity(eq(CustomerConsentDummies.CONSENT_ID.toString()), any()))
                    .thenReturn(outboxEntity);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            underTest.createConsent(BEARER_TOKEN, ConsentRequestDummies.requestWithMandatoryTerm());

            verify(customerConsentRepository).saveConsent(consentVO);
            verify(outboxEventQueueRepository).save(outboxEntity);
            verify(valueOperations).set("sync_status:" + CUSTOMER_ID, "PROCESSING", MAX_VALIDITY_DAYS, TimeUnit.DAYS);
        }
    }

    @Test
    void createConsent_mandatoryTermMissing_throwsMandatoryTermNotAcceptedException() {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        ConsentRequestVO requestVO = ConsentRequestDummies.consentRequestVOMissingTerm();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class);
             MockedStatic<HmacUtils> hmacMock = mockStatic(HmacUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);
            hmacMock.when(() -> HmacUtils.generateHmacSha256(CPF_PLAIN)).thenReturn(CUSTOMER_ID);

            when(creditCoreMapper.toVO(ConsentRequestDummies.requestMissingMandatoryTerm(), CUSTOMER_ID))
                    .thenReturn(requestVO);
            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(revokedTerm));

            MandatoryTermNotAcceptedException ex = assertThrows(
                    MandatoryTermNotAcceptedException.class,
                    () -> underTest.createConsent(BEARER_TOKEN, ConsentRequestDummies.requestMissingMandatoryTerm()));

            assertThat(ex.getErrors()).hasSize(1);
            assertThat(ex.getErrors().get(0)).contains(REVOKED_TERM_CODE);

            verify(customerConsentRepository, never()).saveConsent(any());
            verify(outboxEventQueueRepository, never()).save(any());
            verify(redisTemplate, never()).opsForValue();
        }
    }

    @Test
    void createConsent_termAlreadyActive_skipsConsentSaveButStillSetsRedis() {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        ConsentRequestVO requestVO = ConsentRequestDummies.consentRequestVO();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class);
             MockedStatic<HmacUtils> hmacMock = mockStatic(HmacUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);
            hmacMock.when(() -> HmacUtils.generateHmacSha256(CPF_PLAIN)).thenReturn(CUSTOMER_ID);

            when(creditCoreMapper.toVO(ConsentRequestDummies.requestWithMandatoryTerm(), CUSTOMER_ID))
                    .thenReturn(requestVO);
            when(termsCatalogRepository.findLatestActiveByProduct(PRODUCT))
                    .thenReturn(List.of(revokedTerm));
            when(customerConsentRepository.getActiveCustomerConsent(CUSTOMER_ID, REVOKED_TERM_CODE))
                    .thenReturn(CustomerConsentDummies.consentWithTermId(REVOKED_TERM_ID, REVOKED_TERM_CODE));
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            underTest.createConsent(BEARER_TOKEN, ConsentRequestDummies.requestWithMandatoryTerm());

            verify(customerConsentRepository, never()).saveConsent(any());
            verify(outboxEventQueueRepository, never()).save(any());
            verify(valueOperations).set("sync_status:" + CUSTOMER_ID, "PROCESSING", MAX_VALIDITY_DAYS, TimeUnit.DAYS);
        }
    }

    // ─── getPendingTerms ──────────────────────────────────────────────────────

    @Test
    void getPendingTerms_allTermsSigned_returnsEmptyPendingTermsAndNoMandatoryFlag() {
        TermsCatalogVO revokedTerm = CreditTermDummies.revokedTerm();
        TermsCatalogVO softTerm = CreditTermDummies.softTerm();

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class);
             MockedStatic<HmacUtils> hmacMock = mockStatic(HmacUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);
            hmacMock.when(() -> HmacUtils.generateHmacSha256(CPF_PLAIN)).thenReturn(CUSTOMER_ID);

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

            underTest.getPendingTerms(BEARER_TOKEN, PRODUCT);

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

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class);
             MockedStatic<HmacUtils> hmacMock = mockStatic(HmacUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);
            hmacMock.when(() -> HmacUtils.generateHmacSha256(CPF_PLAIN)).thenReturn(CUSTOMER_ID);

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

            underTest.getPendingTerms(BEARER_TOKEN, PRODUCT);

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

        try (MockedStatic<JwtTokenUtils> jwtMock = mockStatic(JwtTokenUtils.class);
             MockedStatic<HmacUtils> hmacMock = mockStatic(HmacUtils.class)) {

            jwtMock.when(() -> JwtTokenUtils.cpfToken(BEARER_TOKEN)).thenReturn(CPF_PLAIN);
            hmacMock.when(() -> HmacUtils.generateHmacSha256(CPF_PLAIN)).thenReturn(CUSTOMER_ID);

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

            underTest.getPendingTerms(BEARER_TOKEN, PRODUCT);

            ActiveConsentResponseVO captured = voCaptor.getValue();
            assertThat(captured.getHasPendingMandatoryTerms()).isFalse();
            assertThat(captured.getPendingTerms()).hasSize(1);
        }
    }
}
