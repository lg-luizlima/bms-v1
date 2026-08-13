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

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import br.com.tlf.core.application.mapper.creditcore.CreditCoreMapper;
import br.com.tlf.core.application.mapper.outboxeventqueue.OutBoxEventQueueMapper;
import br.com.tlf.core.application.service.customer.CustomerIdResolver;
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
import br.com.tlf.shared.observability.ObservabilityPiiProperties;
import br.com.tlf.shared.util.JsonSerializer;
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
        private final ObservationRegistry observationRegistry;
        private final ObservabilityPiiProperties observabilityPiiProperties;
        private final CustomerIdResolver customerIdResolver;
        @Value("${features.eventhub.parallel-publish-enabled:true}")
        private boolean eventHubParallelPublishEnabled = true;

        @Override
        public ConsentResponseDTO createConsent(String authorization, ConsentRequestDTO requestDTO, String channelId,
                        String correlationId, String customerIdHeader) {

                publishEventHubIfEnabled(requestDTO);

                String customerId = customerIdResolver.resolve(authorization, customerIdHeader);

                log.debug("createConsent - customerId: {}, product: {}, acceptedTerms: {}, channelId: {}, correlationId: {}, customerIdHeader: {}",
                        customerId, requestDTO.getProduct(), requestDTO.getAcceptedTerms(), channelId, correlationId, customerIdHeader);
                
                ConsentRequestVO consentRequestVO = creditCoreMapper.toVO(requestDTO, customerId);

                List<TermsCatalogVO> latestActiveByProduct = termsCatalogRepository
                                .findLatestActiveByProduct(requestDTO.getProduct());

                validateMandatoryTerms(latestActiveByProduct, consentRequestVO.getAcceptedTerms(), customerId);

                processAcceptedTerms(customerId, consentRequestVO, latestActiveByProduct);

                writeSyncStatus(consentRequestVO.getCustomerId());

                return ConsentResponseDTO.builder()
                                .consentReceivedAt(Instant.now())
                                .build();
        }

        private void writeSyncStatus(String customerId) {
                String redisKey = "sync_status:" + customerId;
                String redisValue = "PROCESSING";

                Observation redisWriteObservation = Observation.createNotStarted("redis.sync_status.write", observationRegistry)
                                .contextualName("SET sync_status")
                                .lowCardinalityKeyValue("redis.command", "SET");

                if (observabilityPiiProperties.isRedisValuesEnabled()) {
                        redisWriteObservation
                                        .highCardinalityKeyValue("redis.key", redisKey)
                                        .highCardinalityKeyValue("redis.value", redisValue);
                }

                redisWriteObservation.observe(() ->
                                redisTemplate.opsForValue().set(redisKey, redisValue, MAX_VALIDITY_DAYS, TimeUnit.DAYS));
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

        private Map<String, TermsCatalogVO> latestVersionPerTermCode(List<TermsCatalogVO> terms) {
                return terms.stream()
                                .collect(Collectors.toMap(
                                                TermsCatalogVO::getTermCode,
                                                t -> t,
                                                (existing, replacement) -> {
                                                        double existingVersion = Double.parseDouble(existing.getVersion());
                                                        double replacementVersion = Double.parseDouble(replacement.getVersion());
                                                        return replacementVersion > existingVersion ? replacement : existing;
                                                }));
        }

        private void processAcceptedTerms(String customerId, ConsentRequestVO consentRequestVO,
                        List<TermsCatalogVO> latestActiveByProduct) {
                Map<String, TermsCatalogVO> catalogMap = latestVersionPerTermCode(latestActiveByProduct);

                for (AcceptedTermVO acceptedTerm : consentRequestVO.getAcceptedTerms()) {
                        TermsCatalogVO catalogTerm = catalogMap.get(acceptedTerm.getTermCode());

                        if (catalogTerm == null) {
                                log.error("[createConsent] Invalid term code not found in catalog: {}", acceptedTerm.getTermCode());
                                throw new InvalidTermException(
                                                "Invalid term code",
                                                List.of(acceptedTerm.getTermCode()));
                        }

                        if (!isTermSigned(catalogTerm, customerId)) {
                                log.info("[createConsent] Saving consent for customerId: {}, termCode: {}, termId: {}", customerId, acceptedTerm.getTermCode(), catalogTerm.getId());
                                CustomerConsentVO consent = creditCoreMapper.toCustomerConsentVO(customerId, consentRequestVO, acceptedTerm, catalogTerm);
                                saveConsentAtomically(consent, customerId);
                        }else{
                                log.info("[createConsent] Term already signed for customerId: {}, termCode: {}, termId: {}", customerId, acceptedTerm.getTermCode(), catalogTerm.getId());
                        }
                }
        }

        @Override
        public ActiveConsentResponseDTO getPendingTerms(String authorization, String product, String channelId,
                        String correlationId, String customerIdHeader) {

                String customerId = customerIdResolver.resolve(authorization, customerIdHeader);

                log.info("getPendingTerms - customerId: {}, product: {}, channelId: {}, correlationId: {}, customerIdHeader: {}",
                                customerIdHeader, product, channelId, correlationId, customerIdHeader);

                List<TermsCatalogVO> rawTermsCatalog = termsCatalogRepository.findLatestActiveByProduct(product);

                if (rawTermsCatalog.isEmpty())
                        throw new ProductNotFoundException("Product not found.", List.of("No product was found for " + product));

                List<TermsCatalogVO> termsCatalog = List.copyOf(latestVersionPerTermCode(rawTermsCatalog).values());

                List<TermsCatalogVO> signedTerms = termsCatalog.stream()
                                .filter(t -> isTermSigned(t, customerId))
                                .toList();

                List<TermsCatalogVO> pendingTerms = termsCatalog.stream()
                                .filter(t -> !signedTerms.contains(t))
                                .toList();

                Boolean hasPendingMandatoryTerms = pendingTerms.stream()
                                .anyMatch((pendingTerm) -> Boolean.TRUE.equals(pendingTerm.getIsMandatory()));

                log.warn("[getPendingTerms] Pending terms for customerId hash {} and product {}: {}", customerId, product, pendingTerms.size());

                ActiveConsentResponseVO build = ActiveConsentResponseVO.builder()
                                .product(termsCatalog.getFirst().getProduct())
                                .hasPendingMandatoryTerms(hasPendingMandatoryTerms)
                                .pendingTerms(creditCoreMapper.toPendingTermVO(pendingTerms))
                                .build();

                return creditCoreMapper.toActiveConsentResponseDTO(build);
        }

        private void saveConsentAtomically(CustomerConsentVO consent, String customerId) {
                transactionTemplate.executeWithoutResult(status -> {
                        customerConsentRepository.saveConsent(consent);

                        ConsentEventPayloadVO eventPayload = creditCoreMapper.toConsentEventPayloadVO(customerId, consent);
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

        private boolean isTermSigned(TermsCatalogVO term, String customerId) {
                CustomerConsentVO existingConsent = customerConsentRepository
                                .getActiveCustomerConsent(customerId, term.getTermCode());

                if (existingConsent == null) {
                        return false;
                }

                if (Boolean.TRUE.equals(term.getRevokePreviousVersions())) {
                        return existingConsent.getTermId().equals(term.getId());
                }

                return true;
        }

        private void validateMandatoryTerms(List<TermsCatalogVO> catalogTerms, List<AcceptedTermVO> acceptedTerms, String customerId) {

                Map<String, Boolean> acceptedTermsMap = acceptedTerms.stream()
                                .collect(Collectors.toMap(AcceptedTermVO::getTermCode, AcceptedTermVO::getOptIn));

                List<String> missingMandatoryTerms = catalogTerms.stream()
                                .filter(term -> Boolean.TRUE.equals(term.getIsMandatory()))
                                .filter(term -> !isTermSigned(term, customerId))
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
