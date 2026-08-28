package br.com.tlf.core.domain.terms;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public record TermsCatalog(List<TermsCatalogEntry> entries) {

    public TermsCatalog {
        entries = List.copyOf(entries);
    }

    public static TermsCatalog of(Collection<TermsCatalogEntry> entries) {
        return new TermsCatalog(List.copyOf(entries));
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public TermsCatalog latestVersionPerTermCode() {
        Map<String, TermsCatalogEntry> latest = new LinkedHashMap<>();
        for (TermsCatalogEntry entry : entries) {
            latest.merge(entry.termCode(), entry,
                    (existing, candidate) -> candidate.isNewerThan(existing) ? candidate : existing);
        }
        return TermsCatalog.of(latest.values());
    }
}
