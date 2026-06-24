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
import br.com.tlf.core.domain.exception.InvalidTermException;
import br.com.tlf.core.domain.exception.MandatoryTermNotAcceptedException;
import br.com.tlf.core.domain.exception.ProductNotFoundException;
import br.com.tlf.core.domain.vo.consent.AcceptedTermVO;
import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.core.port.in.creditcore.CreditCorePortIn;
import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.core.port.out.eventhub.EventHubPort;
import br.com.tlf.core.port.out.eventhub.dto.request.EventHubRequestDTO;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.infrastructure.persistence.postgresql.entity.OutboxEventQueueJpaEntity;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.OutboxEventQueueJpaRepository;
import br.com.tlf.shared.util.jwt.JwtTokenUtils;
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
        private final EventHubPort eventHubPort;

        @Override
        public void createConsent(String authorization, ConsentRequestDTO requestDTO) {


                eventHubPort.sendEvent(EventHubRequestDTO.builder()
                                .event(requestDTO)
                                .eventType("CREATE_CONSENT_REQUEST")
                                .build());

                String customerId = JwtTokenUtils.cpfToken(authorization);

                ConsentRequestVO consentRequestVO = creditCoreMapper.toVO(requestDTO, customerId);

                List<TermsCatalogVO> latestActiveByProduct = termsCatalogRepository
                                .findLatestActiveByProduct(requestDTO.getProduct());

                validateMandatoryTerms(latestActiveByProduct, consentRequestVO.getAcceptedTerms(), customerId);

                processAcceptedTerms(customerId, consentRequestVO, latestActiveByProduct);
                
                redisTemplate.opsForValue().set(
                                "sync_status:" + consentRequestVO.getCustomerId(),
                                "PROCESSING",
                                MAX_VALIDITY_DAYS,
                                TimeUnit.DAYS);
        }

        private void processAcceptedTerms(String cpfToken, ConsentRequestVO consentRequestVO,
                        List<TermsCatalogVO> latestActiveByProduct) {
                Map<String, TermsCatalogVO> catalogMap = latestActiveByProduct.stream()
                                .collect(Collectors.toMap(
                                                TermsCatalogVO::getTermCode,
                                                t -> t,
                                                (existing, replacement) -> {
                                                        double existingVersion = Double.parseDouble(existing.getVersion());
                                                        double replacementVersion = Double.parseDouble(replacement.getVersion());
                                                        return replacementVersion > existingVersion ? replacement : existing;
                                                }));

                for (AcceptedTermVO acceptedTerm : consentRequestVO.getAcceptedTerms()) {
                        TermsCatalogVO catalogTerm = catalogMap.get(acceptedTerm.getTermCode());

                        if (catalogTerm == null) {
                                log.error("[createConsent] Invalid term code not found in catalog: {}", acceptedTerm.getTermCode());
                                throw new InvalidTermException(
                                                "Invalid term code",
                                                List.of(acceptedTerm.getTermCode()));
                        }

                        if (!isTermSigned(catalogTerm, cpfToken)) {
                                CustomerConsentVO consent = creditCoreMapper.toCustomerConsentVO(cpfToken, consentRequestVO, acceptedTerm, catalogTerm);
                                saveConsent(consent);
                        }
                }
        }

        @Override
        public ActiveConsentResponseDTO getPendingTerms(String authorization, String product) {

                String customerId = JwtTokenUtils.cpfToken(authorization);

                List<TermsCatalogVO> termsCatalog = termsCatalogRepository.findLatestActiveByProduct(product);

                if (termsCatalog.isEmpty())
                        throw new ProductNotFoundException("Product not found.", List.of("No product was found for " + product));

                List<TermsCatalogVO> signedTerms = termsCatalog.stream()
                                .filter(t -> isTermSigned(t, customerId))
                                .toList();

                List<TermsCatalogVO> pendingTerms = termsCatalog.stream()
                                .filter(t -> !signedTerms.contains(t))
                                .toList();

                Boolean hasPendingMandatoryTerms = pendingTerms.stream()
                                .anyMatch((pendingTerm) -> Boolean.TRUE.equals(pendingTerm.getIsMandatory()));

                //Test
                log.warn("[getPendingTerms] Pending terms for CPF hash {} and product {}: {}", customerId, product, pendingTerms.size());
                pendingTerms
                .stream()
                .map(String::valueOf)
                .forEach(log::info);        

                ActiveConsentResponseVO build = ActiveConsentResponseVO.builder()
                                .product(termsCatalog.getFirst().getProduct())
                                .hasPendingMandatoryTerms(hasPendingMandatoryTerms)
                                .pendingTerms(creditCoreMapper.toPendingTermVO(pendingTerms))
                                .build();

                return creditCoreMapper.toActiveConsentResponseDTO(build);
        }


        @Transactional
        private void saveConsent(CustomerConsentVO consent) {
                customerConsentRepository.saveConsent(consent);
                OutboxEventQueueJpaEntity outboxEvent = outBoxEventQueueMapper.toEntity(
                                consent.getCustomerId(), toJson(consent));
                outboxEventQueueRepository.save(outboxEvent);
        }

        private String toJson(Object value) {
                try {
                        return objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                        log.error("Error serializing object to JSON: {}", e.getMessage());
                        throw new IllegalStateException("Failed to serialize object to JSON", e);
                }
        }

        private boolean isTermSigned(TermsCatalogVO term, String cpfToken) {
                CustomerConsentVO existingConsent = customerConsentRepository
                                .getActiveCustomerConsent(cpfToken, term.getTermCode());

                return existingConsent != null && existingConsent.getTermId().equals(term.getId());
        }

        private void validateMandatoryTerms(List<TermsCatalogVO> catalogTerms, List<AcceptedTermVO> acceptedTerms, String cpfToken) {

                Map<String, Boolean> acceptedTermsMap = acceptedTerms.stream()
                                .collect(Collectors.toMap(AcceptedTermVO::getTermCode, AcceptedTermVO::getOptIn));

                List<String> missingMandatoryTerms = catalogTerms.stream()
                                .filter(term -> Boolean.TRUE.equals(term.getIsMandatory()))
                                .filter(term -> !isTermSigned(term, cpfToken))
                                .filter(term -> {
                                        Boolean optIn = acceptedTermsMap.get(term.getTermCode());
                                        return optIn == null || Boolean.FALSE.equals(optIn);
                                })
                                .map(TermsCatalogVO::getTermCode)
                                .toList();

                if (!missingMandatoryTerms.isEmpty()) {
                        log.warn("[createConsent] Missing mandatory terms: {}", missingMandatoryTerms);
                        throw new MandatoryTermNotAcceptedException(
                                        "Mandatory terms not accepted",
                                        missingMandatoryTerms);
                }
        }

}
