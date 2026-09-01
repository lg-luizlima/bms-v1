package br.com.tlf.core.application.usecase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public List<PendingTermsResult> execute(PendingTermsQuery query) {
        return query.product() != null
                ? List.of(singleProductResult(query))
                : allProductsResults(query);
    }

    private PendingTermsResult singleProductResult(PendingTermsQuery query) {
        TermsCatalog vigentTerms = termsCatalogRepository.findVigentTerms(query.product());
        if (vigentTerms.isEmpty()) {
            throw new ProductNotFoundException("Product not found.",
                    List.of("No product was found for " + query.product()));
        }

        TermsCatalog currentTerms = vigentTerms.latestVersionPerTermCode();
        Map<String, CustomerConsent> activeConsents = customerConsentRepository.findActiveConsentsByTermCode(
                query.customerId(), currentTerms.entries().stream().map(TermsCatalogEntry::termCode).toList());

        List<TermsCatalogEntry> pendingEntries = pendingEntries(currentTerms, activeConsents);
        log.info("[getPendingTerms] product={} correlationId={} pendingTerms={}",
                query.product(), query.correlationId(), pendingEntries.size());

        return buildResult(query.product(), pendingEntries);
    }

    private List<PendingTermsResult> allProductsResults(PendingTermsQuery query) {
        Map<String, TermsCatalog> catalogsByProduct = termsCatalogRepository.findVigentTermsGroupedByProduct();
        if (catalogsByProduct.isEmpty()) {
            return List.of();
        }

        Map<String, TermsCatalog> currentTermsByProduct = new LinkedHashMap<>();
        Set<String> allTermCodes = new LinkedHashSet<>();
        catalogsByProduct.forEach((product, catalog) -> {
            TermsCatalog currentTerms = catalog.latestVersionPerTermCode();
            currentTermsByProduct.put(product, currentTerms);
            currentTerms.entries().forEach(entry -> allTermCodes.add(entry.termCode()));
        });

        Map<String, CustomerConsent> activeConsents =
                customerConsentRepository.findActiveConsentsByTermCode(query.customerId(), allTermCodes);

        List<PendingTermsResult> results = new ArrayList<>();
        currentTermsByProduct.forEach((product, currentTerms) -> {
            List<TermsCatalogEntry> pendingEntries = pendingEntries(currentTerms, activeConsents);
            if (!pendingEntries.isEmpty()) {
                log.info("[getPendingTerms] product={} correlationId={} pendingTerms={}",
                        product, query.correlationId(), pendingEntries.size());
                results.add(buildResult(product, pendingEntries));
            }
        });
        return results;
    }

    private PendingTermsResult buildResult(String product, List<TermsCatalogEntry> pendingEntries) {
        List<PendingTerm> pendingTerms = termsCatalogMapper.toPendingTerms(pendingEntries);
        return PendingTermsResult.builder()
                .product(product)
                .hasPendingMandatoryTerms(pendingEntries.stream().anyMatch(TermsCatalogEntry::isMandatoryTerm))
                .pendingTerms(pendingTerms)
                .build();
    }

    private List<TermsCatalogEntry> pendingEntries(TermsCatalog currentTerms,
            Map<String, CustomerConsent> activeConsents) {
        return currentTerms.entries().stream()
                .filter(entry -> !isSigned(activeConsents, entry))
                .toList();
    }

    private boolean isSigned(Map<String, CustomerConsent> activeConsents, TermsCatalogEntry entry) {
        CustomerConsent consent = activeConsents.get(entry.termCode());
        return consent != null && consent.covers(entry);
    }
}
