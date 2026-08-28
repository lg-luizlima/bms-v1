package br.com.tlf.core.domain.terms;

public record PendingTerm(
        String termId,
        String termCode,
        String title,
        String contentSummary,
        String contentUrl,
        Boolean isMandatory) {
}
