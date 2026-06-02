package br.com.tlf.domain.service.customerconsent;

import static br.com.tlf.application.ApplicationConstants.CLASS_METHOD_MESSAGE_PATTERN;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import br.com.tlf.application.mapper.creditterm.CreditTermMapper;
import br.com.tlf.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.domain.vo.terms.PendingTermVO;
import br.com.tlf.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.persistence.repository.custom.catalog.TermsCatalogRepository;
import br.com.tlf.persistence.repository.custom.consent.CustomerConsentRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.tlf.configuration.common.rest.exceptionhandler.error.InvalidTermException;
import br.com.tlf.configuration.common.rest.exceptionhandler.model.ConsentErrorEntry;
import br.com.tlf.domain.entity.CustomerConsentEntity;
import br.com.tlf.domain.entity.OutboxEventQueueEntity;
import br.com.tlf.domain.entity.TermsCatalogEntity;
import br.com.tlf.domain.service.outbox.OutboxEventQueueService;
import br.com.tlf.domain.util.LogUtils;
import br.com.tlf.domain.vo.consent.AcceptedTermVO;
import br.com.tlf.domain.vo.consent.ConsentRequestVO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerConsentServiceImpl implements CustomerConsentService {

    private final CustomerConsentRepository customerConsentRepository;
    private final OutboxEventQueueService outboxEventQueueService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void createConsent(List<TermsCatalogVO>activeTerms ,ConsentRequestVO request) {
        LogUtils.log(CLASS_METHOD_MESSAGE_PATTERN, this.getClass().getSimpleName(), "createConsent");
        LogUtils.log("Starting consent creation for product: {}", request.getProduct());

        Map<String, TermsCatalogVO> termMap = new HashMap<>();
        for (TermsCatalogVO term : activeTerms) {
            termMap.put(term.getTermCode(), term);
        }

        LogUtils.log("Validating {} accepted terms", request.getAcceptedTerms().size());
        List<ConsentErrorEntry> errors = validateConsents(request.getAcceptedTerms(), termMap);

        if (!errors.isEmpty()) {
            LogUtils.log("Validation failed with {} errors", errors.size());
            throw new InvalidTermException("Validation errors in accepted terms", errors);
        }
        LogUtils.log("All terms validated successfully");

        String cpfHash = hashCpf(request.getCpf());
        LogUtils.log("CPF hash computed successfully");
        long maxValidityDays = 30L;

        for (AcceptedTermVO acceptedTerm : request.getAcceptedTerms()) {
            LogUtils.log("Processing term: {}, optIn: {}", acceptedTerm.getTermCode(), acceptedTerm.getOptIn());
            TermsCatalogVO termsCatalog = termMap.get(acceptedTerm.getTermCode());

            if (customerConsentRepository.getActiveConsent(cpfHash, acceptedTerm.getTermCode())) {
                LogUtils.log("Active consent already exists for term: {}, skipping", acceptedTerm.getTermCode());
                continue;
            }

            long validityDays = termsCatalog.getValidityDays() != null ? termsCatalog.getValidityDays() : 30L;
            if (validityDays > maxValidityDays) {
                maxValidityDays = validityDays;
            }

            Instant now = Instant.now();

            Instant expiresAt = now.plus(validityDays, ChronoUnit.DAYS);
            LogUtils.log("Creating consent for term: {} with expiration in {} days", acceptedTerm.getTermCode(), validityDays);

            CustomerConsentVO consent = CustomerConsentVO.builder()
                    .cpfHash(cpfHash)
                    .termCode(acceptedTerm.getTermCode())
                    .termId(termsCatalog.getId())
                    .optIn(acceptedTerm.getOptIn())
                    .acceptedAt(now)
                    .expiresAt(expiresAt)
                    .auditDetails(toJson(request.getSignature()))
                    .build();
            customerConsentRepository.saveConsent(consent);
            LogUtils.log("Consent saved with ID: {}", consent.getId());

            OutboxEventQueueEntity outboxEvent = OutboxEventQueueEntity.builder()
                    .aggregateType("CustomerConsent")
                    .aggregateId(consent.getId().toString())
                    .topicName("vivopay.credit.onboarding.consent.registered.v1")
                    .payload(toJson(consent))
                    .build();
            outboxEventQueueService.save(outboxEvent);
            LogUtils.log("Outbox event created for consent ID: {}", consent.getId());
        }

        LogUtils.log("Setting Redis sync status with max validity: {} days", maxValidityDays);
        redisTemplate.opsForValue().set("sync_status:" + request.getCpf(), "PROCESSING", maxValidityDays, TimeUnit.DAYS);
        LogUtils.log("Consent creation completed successfully");
    }

    @Override
    public ActiveConsentResponseVO pendingTerms(List<TermsCatalogVO> termsCatalog, String customerId) {
        LogUtils.log(CLASS_METHOD_MESSAGE_PATTERN, this.getClass().getSimpleName(), "pendingTerms");
        LogUtils.log("Starting pending terms evaluation for {} catalog entries", termsCatalog.size());

        LogUtils.log("Computing CPF hash for customer");
        String cpfHash = hashCpf(customerId);
        LogUtils.log("CPF hash computed successfully");

        LogUtils.log("Filtering terms with revoke previous versions flag");
        List<TermsCatalogVO> consentsRevokedToSign = termsCatalog.stream()
                .filter(t -> Boolean.TRUE.equals(t.getRevokePreviousVersions()))
                .filter(t -> Boolean.TRUE.equals(customerConsentRepository.getConsentsRevoked(cpfHash, t.getId())))
                .toList();
        LogUtils.log("Found {} revoked terms requiring re-signature", consentsRevokedToSign.size());

        LogUtils.log("Filtering expired or missing consents");
        List<TermsCatalogVO> consentsExpired = termsCatalog.stream()
                .filter(t -> Boolean.FALSE.equals(t.getRevokePreviousVersions()))
                .filter(t -> Boolean.FALSE.equals(customerConsentRepository.getActiveConsent(cpfHash, t.getTermCode())))
                .toList();
        LogUtils.log("Found {} expired or unsigned terms", consentsExpired.size());

        List<PendingTermVO> pendingTerms = Stream.concat(
                        consentsRevokedToSign.stream(),
                        consentsExpired.stream()
                )
                .map(CreditTermMapper.INSTANCE::toPendingTermVO)
                .toList();
        LogUtils.log("Total pending terms: {}", pendingTerms.size());

        Boolean hasPendingMandatoryTerms = pendingTerms.stream()
                .anyMatch((pendingTerm) -> Boolean.TRUE.equals(pendingTerm.getIsMandatory()));
        LogUtils.log("Has pending mandatory terms: {}", hasPendingMandatoryTerms);

        LogUtils.log("Pending terms evaluation completed successfully");
        return ActiveConsentResponseVO.builder()
                .product(termsCatalog.getFirst().getProduct())
                .hasPendingMandatoryTerms(hasPendingMandatoryTerms)
                .pendingTerms(pendingTerms)
                .build();

    }

    private List<ConsentErrorEntry> validateConsents(List<AcceptedTermVO> acceptedTerms, Map<String, TermsCatalogVO> termMap) {
        LogUtils.log(CLASS_METHOD_MESSAGE_PATTERN, this.getClass().getSimpleName(), "validateConsents");
        List<ConsentErrorEntry> errors = new ArrayList<>();

        for (int i = 0; i < acceptedTerms.size(); i++) {
            AcceptedTermVO acceptedTerm = acceptedTerms.get(i);
            if (!termMap.containsKey(acceptedTerm.getTermCode())) {
                LogUtils.log("Invalid term found at index {}: {}", i, acceptedTerm.getTermCode());
                errors.add(new ConsentErrorEntry(
                        "INVALID_TERM",
                        String.format("acceptedTerms[%d].termCode", i),
                        String.format("The term %s is not available in the catalog.", acceptedTerm.getTermCode())
                ));
            }
        }

        for (TermsCatalogVO term : termMap.values()) {
            if (term.getIsMandatory() != null && term.getIsMandatory()) {
                boolean found = acceptedTerms.stream()
                        .anyMatch(at -> at.getTermCode().equals(term.getTermCode()) && Boolean.TRUE.equals(at.getOptIn()));

                if (!found) {
                    LogUtils.log("Mandatory term not found or not accepted: {}", term.getTermCode());
                    errors.add(new ConsentErrorEntry(
                            "MANDATORY_TERM_MISSING",
                            "acceptedTerms",
                            String.format("The term %s is mandatory and requires optIn = true", term.getTermCode())
                    ));
                }
            }
        }

        LogUtils.log("Validation completed with {} errors", errors.size());
        return errors;
    }

    private String hashCpf(String cpf) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(cpf.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            LogUtils.error("Error hashing CPF: {}", e.getMessage());
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            LogUtils.error("Error serializing object to JSON: {}", e.getMessage());
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }
    }

