package br.com.tlf.core.domain.terms;

import java.util.List;

import lombok.Builder;

@Builder
public record PendingTermsResult(String product, Boolean hasPendingMandatoryTerms, List<PendingTerm> pendingTerms) {
}
