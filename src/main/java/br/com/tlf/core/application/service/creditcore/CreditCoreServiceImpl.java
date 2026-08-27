package br.com.tlf.core.application.service.creditcore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
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
import br.com.tlf.core.domain.exception.ProductNotFoundException;
import br.com.tlf.core.domain.vo.consent.AcceptedTermVO;
import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.consent.SignatureVO;
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
        private final ConsentIdempotencyChecker consentIdempotencyChecker;
        @Value("${features.eventhub.parallel-publish-enabled:true}")
        private boolean eventHubParallelPublishEnabled = true;
        @Value("${redis.ttl.sync-status-seconds:86400}")
        private long syncStatusTtlSeconds = 86400L;

        @Override
        public ConsentResponseDTO createConsent(String authorization, ConsentRequestDTO requestDTO, String channelId,
                        String correlationId, String customerIdHeader) {

                String customerId = customerIdResolver.resolve(authorization, customerIdHeader);

                Optional<Instant> cachedResponse = consentIdempotencyChecker.findCachedResponse(customerId, correlationId);
                if (cachedResponse.isPresent()) {
                        log.info("[createConsent] Idempotent replay for customerId: {}, correlationId: {}", customerId, correlationId);
                        return ConsentResponseDTO.builder()
                                        .consentReceivedAt(cachedResponse.get())
                                        .build();
                }

                publishEventHubIfEnabled(requestDTO);

                log.debug("createConsent - customerId: {}, acceptedTerms: {}, channelId: {}, correlationId: {}, customerIdHeader: {}",
                        customerId, requestDTO.getAcceptedTerms(), channelId, correlationId, customerIdHeader);

                ConsentRequestVO consentRequestVO = creditCoreMapper.toVO(requestDTO, customerId);

                Map<UUID, TermsCatalogVO> termsById = resolveAndValidateTerms(consentRequestVO.getAcceptedTerms());

                boolean anyPostProcessing = processAcceptedTerms(customerId, consentRequestVO, termsById);

                if (anyPostProcessing) {
                        writeSyncStatus(consentRequestVO.getCustomerId());
                }

                ConsentResponseDTO response = ConsentResponseDTO.builder()
                                .consentReceivedAt(Instant.now())
                                .build();

                consentIdempotencyChecker.cacheResponse(customerId, correlationId, response.getConsentReceivedAt());

                return response;
        }

        private Map<UUID, TermsCatalogVO> resolveAndValidateTerms(List<AcceptedTermVO> acceptedTerms) {
                List<String> invalidTermIds = new ArrayList<>();
                List<UUID> parsedIds = new ArrayList<>();
                Map<UUID, String> uuidToRaw = new HashMap<>();

                for (AcceptedTermVO acceptedTerm : acceptedTerms) {
                        try {
                                UUID termId = UUID.fromString(acceptedTerm.getTermId());
                                parsedIds.add(termId);
                                uuidToRaw.put(termId, acceptedTerm.getTermId());
                        } catch (IllegalArgumentException ex) {
                                invalidTermIds.add(acceptedTerm.getTermId());
                        }
                }

                Map<UUID, TermsCatalogVO> foundTerms = termsCatalogRepository.findByIds(parsedIds).stream()
                                .collect(Collectors.toMap(TermsCatalogVO::getId, t -> t));

                Instant now = Instant.now();
                for (UUID termId : parsedIds) {
                        TermsCatalogVO term = foundTerms.get(termId);
                        if (term == null || !isVigent(term, now)) {
                                invalidTermIds.add(uuidToRaw.get(termId));
                        }
                }

                if (!invalidTermIds.isEmpty()) {
                        log.error("[createConsent] Invalid, not found or out-of-validity term id(s): {}", invalidTermIds);
                        throw new InvalidTermException("Invalid term id", invalidTermIds);
                }

                return foundTerms;
        }

        private boolean isVigent(TermsCatalogVO term, Instant now) {
                boolean afterStart = now.isAfter(term.getStartAt());
                boolean beforeEnd = term.getEndAt() == null || now.isBefore(term.getEndAt());
                return afterStart && beforeEnd;
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

                try {
                        redisWriteObservation.observe(() ->
                                        redisTemplate.opsForValue().set(redisKey, redisValue, syncStatusTtlSeconds, TimeUnit.SECONDS));
                } catch (DataAccessException ex) {
                        log.warn("[writeSyncStatus] Redis unavailable, sync status not cached for customerId: {}: {}",
                                        customerId, ex.getMessage());
                }
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

        private boolean processAcceptedTerms(String customerId, ConsentRequestVO consentRequestVO,
                        Map<UUID, TermsCatalogVO> termsById) {
                boolean anyPostProcessing = false;

                for (AcceptedTermVO acceptedTerm : consentRequestVO.getAcceptedTerms()) {
                        TermsCatalogVO catalogTerm = termsById.get(UUID.fromString(acceptedTerm.getTermId()));

                        if (!isTermSigned(catalogTerm, customerId)) {
                                log.info("[createConsent] Saving consent for customerId: {}, termCode: {}, termId: {}", customerId, catalogTerm.getTermCode(), catalogTerm.getId());
                                CustomerConsentVO consent = creditCoreMapper.toCustomerConsentVO(customerId, consentRequestVO, acceptedTerm, catalogTerm);
                                if (saveConsentAtomically(consent, customerId, catalogTerm, consentRequestVO.getSignature())) {
                                        anyPostProcessing = true;
                                }
                        } else {
                                log.info("[createConsent] Term already signed for customerId: {}, termCode: {}, termId: {}", customerId, catalogTerm.getTermCode(), catalogTerm.getId());
                        }
                }

                return anyPostProcessing;
        }

        @Override
        public ActiveConsentResponseDTO getPendingTerms(String authorization, String product, String channelId,
                        String correlationId, String customerIdHeader) {

                String customerId = customerIdResolver.resolve(authorization, customerIdHeader);

                log.info("getPendingTerms - customerId: {}, product: {}, channelId: {}, correlationId: {}, customerIdHeader: {}",
                                customerIdHeader, product, channelId, correlationId, customerIdHeader);

                List<TermsCatalogVO> rawTermsCatalog = termsCatalogRepository.findVigentTerms(product);

                if (rawTermsCatalog.isEmpty() && product != null) {
                        throw new ProductNotFoundException("Product not found.", List.of("No product was found for " + product));
                }

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
                                .product(product)
                                .hasPendingMandatoryTerms(hasPendingMandatoryTerms)
                                .pendingTerms(creditCoreMapper.toPendingTermVO(pendingTerms))
                                .build();

                return creditCoreMapper.toActiveConsentResponseDTO(build);
        }

        private boolean saveConsentAtomically(CustomerConsentVO consent, String customerId, TermsCatalogVO termCatalog,
                        SignatureVO signature) {
                boolean requiresPostProcessing = Boolean.TRUE.equals(termCatalog.getRequiresPostProcessing());

                transactionTemplate.executeWithoutResult(status -> {
                        customerConsentRepository.saveConsent(consent);

                        if (requiresPostProcessing) {
                                ConsentEventPayloadVO eventPayload = creditCoreMapper.toConsentEventPayloadVO(customerId, consent,
                                                termCatalog, signature);
                                outboxEventQueueRepository.save(
                                                outBoxEventQueueMapper.toVO(consent.getCustomerId(), jsonSerializer.toJson(eventPayload),
                                                                currentTraceParent()));
                        }
                });

                return requiresPostProcessing;
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

}
