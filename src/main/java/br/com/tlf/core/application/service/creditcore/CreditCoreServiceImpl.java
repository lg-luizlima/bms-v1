package br.com.tlf.core.application.service.creditcore;

import br.com.tlf.api.rest.config.exceptionhandler.error.InvalidTermException;
import br.com.tlf.api.rest.config.exceptionhandler.model.ConsentErrorEntry;
import br.com.tlf.core.application.mapper.creditcore.CreditCoreMapper;
import br.com.tlf.core.application.mapper.creditterm.CreditTermMapper;
import br.com.tlf.core.application.mapper.outboxeventqueue.OutBoxEventQueueMapper;
import br.com.tlf.core.domain.service.outbox.OutboxEventQueueService;
import br.com.tlf.shared.util.JwtTokenUtils;
import br.com.tlf.core.domain.vo.OutBoxEventQueueVO;
import br.com.tlf.core.domain.vo.consent.AcceptedTermVO;
import br.com.tlf.core.domain.vo.consent.ConsentRequestVO;
import br.com.tlf.core.domain.vo.terms.ActiveConsentResponseVO;
import br.com.tlf.core.domain.vo.terms.CustomerConsentVO;
import br.com.tlf.core.domain.vo.terms.TermsCatalogVO;
import br.com.tlf.core.port.in.creditcore.CreditCorePortIn;
import br.com.tlf.core.port.in.dto.request.ConsentRequestDTO;
import br.com.tlf.core.port.in.dto.response.ActiveConsentResponseDTO;
import br.com.tlf.infrastructure.persistence.postgresql.custom.catalog.TermsCatalogRepository;
import br.com.tlf.infrastructure.persistence.postgresql.custom.consent.CustomerConsentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static br.com.tlf.shared.constants.ApplicationConstants.MAX_VALIDITY_DAYS;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreditCoreServiceImpl implements CreditCorePortIn {

    private final CustomerConsentRepository customerConsentRepository;
    private final TermsCatalogRepository termsCatalogRepository;
    private final OutboxEventQueueService outboxEventQueueService;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void createConsent(String authorization, ConsentRequestDTO requestDTO) {
        log.info("Starting consent creation for product: {}", requestDTO.getProduct());

        String cpf = JwtTokenUtils.cpfToken(authorization);

        ConsentRequestVO consentRequestVO = CreditCoreMapper.INSTANCE.toVO(requestDTO, cpf);

        List<TermsCatalogVO> activeTerms = findActiveTerms(requestDTO.getProduct());


        Map<String, TermsCatalogVO> termMap = new HashMap<>();
        for (TermsCatalogVO term : activeTerms) {
            termMap.put(term.getTermCode(), term);
        }

        log.info("Validating {} accepted terms", consentRequestVO.getAcceptedTerms().size());
        List<ConsentErrorEntry> errors = validateConsents(consentRequestVO.getAcceptedTerms(), termMap);

        if (!errors.isEmpty()) {
            log.info("Validation failed with {} errors", errors.size());
            throw new InvalidTermException("Validation errors in accepted terms", errors);
        }
        log.info("All terms validated successfully");

        String cpfHash = hashCpf(consentRequestVO.getCpf());
        log.info("CPF hash computed successfully");

        consentRequestVO.getAcceptedTerms().forEach(acceptedTerm -> {
            log.info("Processing term: {}, optIn: {}", acceptedTerm.getTermCode(), acceptedTerm.getOptIn());
            TermsCatalogVO termsCatalog = termMap.get(acceptedTerm.getTermCode());

            if(customerConsentRepository.getActiveConsent(cpfHash, acceptedTerm.getTermCode())) {
                log.info("Active consent already exists for term: {}, skipping", acceptedTerm.getTermCode());
                return;
            }

            long validityDays = termsCatalog.getValidityDays() != null ? termsCatalog.getValidityDays() : MAX_VALIDITY_DAYS;
            Instant now = Instant.now();
            Instant expiresAt = now.plus(validityDays, ChronoUnit.DAYS);
            log.info("Creating consent for term: {} with expiration in {} days", acceptedTerm.getTermCode(), validityDays);

            CustomerConsentVO consent = CustomerConsentVO.builder()
                    .cpfHash(cpfHash)
                    .termCode(acceptedTerm.getTermCode())
                    .termId(termsCatalog.getId())
                    .optIn(acceptedTerm.getOptIn())
                    .acceptedAt(now)
                    .expiresAt(expiresAt)
                    .auditDetails(toJson(consentRequestVO.getSignature()))
                    .build();
            customerConsentRepository.saveConsent(consent);
            log.info("Consent saved with ID: {}", consent.getId());

            OutBoxEventQueueVO outboxEvent = OutBoxEventQueueMapper.INSTANCE.toVO(
                    consent.getId().toString(),
                    toJson(consent));
            outboxEventQueueService.save(outboxEvent);
            log.info("Outbox event created for consent ID: {}", consent.getId());
        });

        log.info("Setting Redis sync status with max validity: {} days", MAX_VALIDITY_DAYS);
        redisTemplate.opsForValue().set("sync_status:" + consentRequestVO.getCpf(), "PROCESSING", MAX_VALIDITY_DAYS, TimeUnit.DAYS);
        log.info("Consent creation completed successfully");
    }

    @Override
    public ActiveConsentResponseDTO getPendingTerms(String authorization, String product) {

        String customerId = JwtTokenUtils.cpfToken(authorization);

        String cpfHash = hashCpf(customerId);
        log.info("CPF hash computed successfully");

        List<TermsCatalogVO> termsCatalog = findActiveTerms(product);

        List<TermsCatalogVO> signedTerms= termsCatalog.stream()
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
                .pendingTerms(CreditTermMapper.INSTANCE.toPendingTermVO(pendingTerms))
                .build();

        return CreditCoreMapper.INSTANCE.toActiveConsentResponseDTO(build);
    }

    private List<ConsentErrorEntry> validateConsents(List<AcceptedTermVO> acceptedTerms, Map<String, TermsCatalogVO> termMap) {
        List<ConsentErrorEntry> errors = new ArrayList<>();

        for (int i = 0; i < acceptedTerms.size(); i++) {
            AcceptedTermVO acceptedTerm = acceptedTerms.get(i);
            if (!termMap.containsKey(acceptedTerm.getTermCode())) {
                log.info("Invalid term found at index {}: {}", i, acceptedTerm.getTermCode());
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
                    log.info("Mandatory term not found or not accepted: {}", term.getTermCode());
                    errors.add(new ConsentErrorEntry(
                            "MANDATORY_TERM_MISSING",
                            "acceptedTerms",
                            String.format("The term %s is mandatory and requires optIn = true", term.getTermCode())
                    ));
                }
            }
        }

        log.info("Validation completed with {} errors", errors.size());
        return errors;
    }

    private String hashCpf(String cpf) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(cpf.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error hashing CPF: {}", e.getMessage());
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }


    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Error serializing object to JSON: {}", e.getMessage());
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }

    private List<TermsCatalogVO> findActiveTerms(String product) {
        log.info("Fetching active catalog terms for product: {}", product);

        List<TermsCatalogVO> terms = termsCatalogRepository.findLatestActiveByProduct(product);

        log.info("Found {} active terms for product: {}", terms.size(), product);
        return terms;
    }
}
