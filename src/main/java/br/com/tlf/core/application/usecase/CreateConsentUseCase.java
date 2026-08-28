package br.com.tlf.core.application.usecase;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.tlf.core.application.mapper.ConsentMapper;
import br.com.tlf.core.domain.consent.AcceptedTerm;
import br.com.tlf.core.domain.consent.ConsentReceipt;
import br.com.tlf.core.domain.consent.CustomerConsent;
import br.com.tlf.core.domain.exception.InvalidTermException;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import br.com.tlf.core.port.in.CreateConsentPort;
import br.com.tlf.core.port.in.command.CreateConsentCommand;
import br.com.tlf.core.port.out.cache.ConsentCachePort;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.core.port.out.eventhub.EventHubPort;
import br.com.tlf.core.port.out.outbox.ConsentEventOutbox;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.core.port.out.transaction.TransactionRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateConsentUseCase implements CreateConsentPort {

    private final CustomerConsentRepository customerConsentRepository;
    private final TermsCatalogRepository termsCatalogRepository;
    private final ConsentEventOutbox consentEventOutbox;
    private final ConsentCachePort consentCache;
    private final EventHubPort eventHubPort;
    private final TransactionRunner transactionRunner;
    private final ConsentMapper consentMapper;
    private final Clock clock;

    @Override
    public ConsentReceipt execute(CreateConsentCommand command) {
        Optional<Instant> cachedReceipt =
                consentCache.findIdempotentResponse(command.customerId(), command.correlationId());
        if (cachedReceipt.isPresent()) {
            log.info("[createConsent] Idempotent replay for correlationId: {}", command.correlationId());
            return new ConsentReceipt(cachedReceipt.get());
        }

        Map<UUID, TermsCatalogEntry> termsById = resolveAndValidateTerms(command.acceptedTerms());

        eventHubPort.publishConsentRequested(command.acceptedTerms(), command.signature());

        if (registerConsents(command, termsById)) {
            consentCache.writeSyncStatus(command.customerId());
        }

        ConsentReceipt receipt = new ConsentReceipt(Instant.now(clock));
        consentCache.cacheIdempotentResponse(command.customerId(), command.correlationId(),
                receipt.consentReceivedAt());

        return receipt;
    }

    private Map<UUID, TermsCatalogEntry> resolveAndValidateTerms(List<AcceptedTerm> acceptedTerms) {
        List<String> invalidTermIds = new ArrayList<>();
        Map<UUID, String> rawIdByUuid = new LinkedHashMap<>();

        for (AcceptedTerm acceptedTerm : acceptedTerms) {
            try {
                rawIdByUuid.put(UUID.fromString(acceptedTerm.termId()), acceptedTerm.termId());
            } catch (IllegalArgumentException ex) {
                invalidTermIds.add(acceptedTerm.termId());
            }
        }

        Map<UUID, TermsCatalogEntry> foundTerms =
                termsCatalogRepository.findByIds(List.copyOf(rawIdByUuid.keySet())).stream()
                        .collect(Collectors.toMap(TermsCatalogEntry::id, Function.identity()));

        Instant now = Instant.now(clock);
        rawIdByUuid.forEach((termId, rawId) -> {
            TermsCatalogEntry term = foundTerms.get(termId);
            if (term == null || !term.isVigentAt(now)) {
                invalidTermIds.add(rawId);
            }
        });

        if (!invalidTermIds.isEmpty()) {
            log.error("[createConsent] Invalid, not found or out-of-validity term id(s): {}", invalidTermIds);
            throw new InvalidTermException("Invalid term id", invalidTermIds);
        }

        return foundTerms;
    }


    private boolean registerConsents(CreateConsentCommand command, Map<UUID, TermsCatalogEntry> termsById) {
        List<TermsCatalogEntry> termsToRegister = termsToRegister(command, termsById);
        if (termsToRegister.isEmpty()) {
            return false;
        }

        Instant acceptedAt = Instant.now(clock);
        transactionRunner.runInTransaction(() ->
                termsToRegister.forEach(term -> register(command, term, acceptedAt)));

        return termsToRegister.stream().anyMatch(TermsCatalogEntry::requiresPostProcessingStep);
    }

    private List<TermsCatalogEntry> termsToRegister(CreateConsentCommand command,
            Map<UUID, TermsCatalogEntry> termsById) {

        List<TermsCatalogEntry> candidates = distinctAcceptedTerms(command.acceptedTerms()).stream()
                .map(acceptedTerm -> termsById.get(UUID.fromString(acceptedTerm.termId())))
                .toList();

        Map<String, CustomerConsent> activeConsents = customerConsentRepository.findActiveConsentsByTermCode(
                command.customerId(), candidates.stream().map(TermsCatalogEntry::termCode).toList());

        List<TermsCatalogEntry> pending = new ArrayList<>();
        for (TermsCatalogEntry term : candidates) {
            CustomerConsent existing = activeConsents.get(term.termCode());
            if (existing != null && existing.covers(term)) {
                log.info("[createConsent] Term already signed, termCode: {}, termId: {}", term.termCode(), term.id());
            } else {
                pending.add(term);
            }
        }
        return pending;
    }

    private List<AcceptedTerm> distinctAcceptedTerms(List<AcceptedTerm> acceptedTerms) {
        return List.copyOf(acceptedTerms.stream()
                .collect(Collectors.toMap(AcceptedTerm::termId, Function.identity(),
                        (first, duplicate) -> first, LinkedHashMap::new))
                .values());
    }

    private void register(CreateConsentCommand command, TermsCatalogEntry term, Instant acceptedAt) {
        log.info("[createConsent] Saving consent for termCode: {}, termId: {}", term.termCode(), term.id());

        AcceptedTerm acceptedTerm = command.acceptedTerms().stream()
                .filter(candidate -> term.id().equals(UUID.fromString(candidate.termId())))
                .findFirst()
                .orElseThrow();

        CustomerConsent consent = consentMapper.toCustomerConsent(command, term, acceptedTerm, acceptedAt);

        customerConsentRepository.save(consent);

        if (term.requiresPostProcessingStep()) {
            consentEventOutbox.publish(consent.customerId(),
                    consentMapper.toEvent(consent, term, command.signature()));
        }
    }
}
