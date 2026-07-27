package br.com.tlf.core.application.service.creditcore;

import static br.com.tlf.shared.constants.ApplicationConstants.MAX_VALIDITY_DAYS;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import br.com.tlf.core.application.mapper.creditcore.CreditCoreMapper;
import br.com.tlf.core.application.mapper.outboxeventqueue.OutBoxEventQueueMapper;
import br.com.tlf.core.domain.exception.InvalidTermException;
import br.com.tlf.core.domain.exception.MandatoryTermNotAcceptedException;
import br.com.tlf.core.domain.exception.ProductNotFoundException;
import br.com.tlf.core.domain.vo.consent.AcceptedTermVO;
import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.core.domain.vo.terms.ConsentEventPayloadVO;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.core.port.in.creditcore.CreditCorePortIn;
import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.core.port.in.dto.response.ConsentResponseDTO;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.core.port.out.eventhub.EventHubPort;
import br.com.tlf.core.port.out.eventhub.dto.request.EventHubRequestDTO;
import br.com.tlf.core.port.out.outbox.OutboxEventQueueRepository;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.shared.util.HmacUtils;
import br.com.tlf.shared.util.JsonSerializer;
import br.com.tlf.shared.util.jwt.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCoreServiceImpl implements CreditCorePortIn {

        private final CustomerConsentRepository customerConsentRepository;
        private final TermsCatalogRepository termsCatalogRepository;
        private final OutboxEventQueueRepository outboxEventQueueRepository;
        private final CreditCoreMapper creditCoreMapper;
        private final OutBoxEventQueueMapper outBoxEventQueueMapper;
        private final JsonSerializer jsonSerializer;
        private final StringRedisTemplate redisTemplate;
        private final EventHubPort eventHubPort;
        private final TransactionTemplate transactionTemplate;
        private final Tracer tracer;
        private final Propagator propagator;
        @Value("${features.eventhub.parallel-publish-enabled:true}")
        private boolean eventHubParallelPublishEnabled = true;

        @Override
        public ConsentResponseDTO createConsent(String authorization, ConsentRequestDTO requestDTO) {

                publishEventHubIfEnabled(requestDTO);

                String cpf = JwtTokenUtils.cpfToken(authorization);
                String customerId = HmacUtils.generateHmacSha256(cpf);

                log.info("createConsent - customerId: {}, product: {}, acceptedTerms: {}", customerId, requestDTO.getProduct(), requestDTO.getAcceptedTerms());

                ConsentRequestVO consentRequestVO = creditCoreMapper.toVO(requestDTO, customerId);

                List<TermsCatalogVO> latestActiveByProduct = termsCatalogRepository
                                .findLatestActiveByProduct(requestDTO.getProduct());

                validateMandatoryTerms(latestActiveByProduct, consentRequestVO.getAcceptedTerms(), customerId);

                processAcceptedTerms(cpf, customerId, consentRequestVO, latestActiveByProduct);

                redisTemplate.opsForValue().set(
                                "sync_status:" + consentRequestVO.getCustomerId(),
                                "PROCESSING",
                                MAX_VALIDITY_DAYS,
                                TimeUnit.DAYS);

                return ConsentResponseDTO.builder()
                                .consentReceivedAt(Instant.now())
                                .build();
        }

        private void publishEventHubIfEnabled(ConsentRequestDTO requestDTO) {
                if (!eventHubParallelPublishEnabled) {
                        log.info("[createConsent] Event Hub parallel publish disabled. Outbox+Debezium remains the source of event publication.");
                        return;
                }

                eventHubPort.sendEvent(EventHubRequestDTO.builder()
                                .event(requestDTO)
                                .eventType("CREATE_CONSENT_REQUEST")
                                .build());
        }

        private void processAcceptedTerms(String cpf, String cpfToken, ConsentRequestVO consentRequestVO,
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
                                log.info("[createConsent] Saving consent for customerId: {}, termCode: {}, termId: {}", cpfToken, acceptedTerm.getTermCode(), catalogTerm.getId());
                                CustomerConsentVO consent = creditCoreMapper.toCustomerConsentVO(cpfToken, consentRequestVO, acceptedTerm, catalogTerm);
                                saveConsentAtomically(consent, cpf);
                        }else{
                                log.info("[createConsent] Term already signed for customerId: {}, termCode: {}, termId: {}", cpfToken, acceptedTerm.getTermCode(), catalogTerm.getId());
                        }
                }
        }

        @Override
        public ActiveConsentResponseDTO getPendingTerms(String authorization, String product) {

                String customerId = HmacUtils.generateHmacSha256(JwtTokenUtils.cpfToken(authorization));

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

                log.warn("[getPendingTerms] Pending terms for CPF hash {} and product {}: {}", customerId, product, pendingTerms.size());

                ActiveConsentResponseVO build = ActiveConsentResponseVO.builder()
                                .product(termsCatalog.getFirst().getProduct())
                                .hasPendingMandatoryTerms(hasPendingMandatoryTerms)
                                .pendingTerms(creditCoreMapper.toPendingTermVO(pendingTerms))
                                .build();

                return creditCoreMapper.toActiveConsentResponseDTO(build);
        }

        private void saveConsentAtomically(CustomerConsentVO consent, String cpf) {
                transactionTemplate.executeWithoutResult(status -> {
                        customerConsentRepository.saveConsent(consent);

                        ConsentEventPayloadVO eventPayload = creditCoreMapper.toConsentEventPayloadVO(cpf, consent);
                        outboxEventQueueRepository.save(
                                        outBoxEventQueueMapper.toVO(consent.getCustomerId(), jsonSerializer.toJson(eventPayload),
                                                        currentTraceParent()));
                });
        }

        private String currentTraceParent() {
                Span currentSpan = tracer.currentSpan();
                if (currentSpan == null) {
                        return null;
                }
                Map<String, String> carrier = new HashMap<>();
                propagator.inject(currentSpan.context(), carrier, Map::put);
                return carrier.get("traceparent");
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
