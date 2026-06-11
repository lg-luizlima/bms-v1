package br.com.tlf.core.application.service.creditcore;

import static br.com.tlf.shared.constants.ApplicationConstants.MAX_VALIDITY_DAYS;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.tlf.core.application.mapper.creditcore.CreditCoreMapper;
import br.com.tlf.core.application.mapper.outboxeventqueue.OutBoxEventQueueMapper;
import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.core.port.in.creditcore.CreditCorePortIn;
import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueEntity;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.OutboxEventQueueJpaRepository;
import br.com.tlf.shared.util.HmacUtils;
import br.com.tlf.shared.util.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCoreServiceImpl implements CreditCorePortIn {

    private final CustomerConsentRepository customerConsentRepository;
    private final TermsCatalogRepository termsCatalogRepository;
    private final OutboxEventQueueJpaRepository outboxEventQueueRepository;
    private final CreditCoreMapper creditCoreMapper;
    private final OutBoxEventQueueMapper outBoxEventQueueMapper;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void createConsent(String authorization, ConsentRequestDTO requestDTO) {

        String cpfHash = HmacUtils.generateHmacSha256(JwtTokenUtils.cpfToken(authorization));

        ConsentRequestVO consentRequestVO = creditCoreMapper.toVO(requestDTO, cpfHash);

        Map<String, TermsCatalogVO> termMap = termsCatalogRepository
                                                .findLatestActiveByProduct(requestDTO.getProduct()).stream()
                                                .collect(
                                                    Collectors.toMap(TermsCatalogVO::getTermCode, t -> t)
                                                );

        consentRequestVO
            .getAcceptedTerms()
            .stream()
            .forEach(acceptedTerm -> {
                if (!customerConsentRepository.getActiveConsent(cpfHash, acceptedTerm.getTermCode()))
                    saveConsent(
                        creditCoreMapper
                        .toCustomerConsentVO(
                            cpfHash
                            , consentRequestVO
                            , acceptedTerm
                            , termMap.get(acceptedTerm.getTermCode())
                        )
                    );
            });

        redisTemplate
        .opsForValue()
        .set(
            "sync_status:" + consentRequestVO.getCpf()
            , "PROCESSING"
            , MAX_VALIDITY_DAYS
            , TimeUnit.DAYS
        );

    }

    @Transactional
    private void saveConsent(CustomerConsentVO consent) {
        customerConsentRepository.saveConsent(consent);
        OutboxEventQueueEntity outboxEvent = outBoxEventQueueMapper.toEntity(
                consent.getId().toString(), toJson(consent));
        outboxEventQueueRepository.save(outboxEvent);
    }

    @Override
    public ActiveConsentResponseDTO getPendingTerms(String authorization, String product) {

        String cpfHash = HmacUtils.generateHmacSha256(JwtTokenUtils.cpfToken(authorization));
        log.info("CPF hash computed successfully");

        List<TermsCatalogVO> termsCatalog = termsCatalogRepository.findLatestActiveByProduct(product);

        List<TermsCatalogVO> signedTerms = termsCatalog.stream()
                .filter(t -> customerConsentRepository.getActiveConsentTermId(cpfHash, t.getTermCode())
                        .map(termId -> termId.equals(t.getId()))
                        .orElse(false))
                .toList();
        log.info("Signed terms: {}", signedTerms);

        List<TermsCatalogVO> pendingTerms = termsCatalog.stream()
                .filter(t -> !signedTerms.contains(t))
                .toList();
        log.info("pendingTerms: {}", pendingTerms);

        Boolean hasPendingMandatoryTerms = pendingTerms.stream()
                .anyMatch((pendingTerm) -> Boolean.TRUE.equals(pendingTerm.getIsMandatory()));

        ActiveConsentResponseVO build = ActiveConsentResponseVO.builder()
                .product(termsCatalog.getFirst().getProduct())
                .hasPendingMandatoryTerms(hasPendingMandatoryTerms)
                .pendingTerms(creditCoreMapper.toPendingTermVO(pendingTerms))
                .build();

        return creditCoreMapper.toActiveConsentResponseDTO(build);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object to JSON: {}", e.getMessage());
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }

}
