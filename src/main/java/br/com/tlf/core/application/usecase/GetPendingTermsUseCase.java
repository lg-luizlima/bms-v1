package br.com.tlf.core.application.usecase;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import br.com.tlf.core.application.mapper.TermsCatalogMapper;
import br.com.tlf.core.domain.consent.CustomerConsent;
import br.com.tlf.core.domain.exception.ProductNotFoundException;
import br.com.tlf.core.domain.terms.PendingTerm;
import br.com.tlf.core.domain.terms.PendingTermsResult;
import br.com.tlf.core.domain.terms.TermsCatalog;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import br.com.tlf.core.port.in.GetPendingTermsPort;
import br.com.tlf.core.port.in.command.PendingTermsQuery;
import br.com.tlf.core.port.out.customerconsent.CustomerConsentRepository;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GetPendingTermsUseCase implements GetPendingTermsPort {

    private final TermsCatalogRepository termsCatalogRepository;
    private final CustomerConsentRepository customerConsentRepository;
    private final TermsCatalogMapper termsCatalogMapper;

    @Override
    public PendingTermsResult execute(PendingTermsQuery query) {
        TermsCatalog vigentTerms = termsCatalogRepository.findVigentTerms(query.product());

        if (vigentTerms.isEmpty() && query.product() != null) {
            throw new ProductNotFoundException("Product not found.",
                    List.of("No product was found for " + query.product()));
        }

        TermsCatalog currentTerms = vigentTerms.latestVersionPerTermCode();
        List<TermsCatalogEntry> pendingEntries = pendingEntries(query.customerId(), currentTerms);

        log.debug("[getPendingTerms] product={} correlationId={} pendingTerms={}",
                query.product(), query.correlationId(), pendingEntries.size());

        List<PendingTerm> pendingTerms = termsCatalogMapper.toPendingTerms(pendingEntries);

        return PendingTermsResult.builder()
                .product(query.product())
                .hasPendingMandatoryTerms(pendingEntries.stream().anyMatch(TermsCatalogEntry::isMandatoryTerm))
                .pendingTerms(pendingTerms)
                .build();
    }

    private List<TermsCatalogEntry> pendingEntries(String customerId, TermsCatalog currentTerms) {
        Map<String, CustomerConsent> activeConsents = customerConsentRepository.findActiveConsentsByTermCode(
                customerId, currentTerms.entries().stream().map(TermsCatalogEntry::termCode).toList());

        return currentTerms.entries().stream()
                .filter(entry -> !isSigned(activeConsents, entry))
                .toList();
    }

    private boolean isSigned(Map<String, CustomerConsent> activeConsents, TermsCatalogEntry entry) {
        CustomerConsent consent = activeConsents.get(entry.termCode());
        return consent != null && consent.covers(entry);
    }
}
