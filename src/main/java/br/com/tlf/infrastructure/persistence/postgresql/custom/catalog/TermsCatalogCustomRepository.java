package br.com.tlf.infrastructure.persistence.postgresql.custom.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import br.com.tlf.core.domain.terms.TermsCatalog;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.TermProductProjection;
import br.com.tlf.infrastructure.persistence.postgresql.jpa.TermsCatalogJpaRepository;
import br.com.tlf.infrastructure.persistence.postgresql.mapper.TermsCatalogRepositoryMapper;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class TermsCatalogCustomRepository implements TermsCatalogRepository {

    private final TermsCatalogJpaRepository termsCatalogJpaRepository;
    private final TermsCatalogRepositoryMapper termsCatalogRepositoryMapper;

    @Override
    public List<TermsCatalogEntry> findByIds(List<UUID> termIds) {
        return termsCatalogRepositoryMapper.toDomain(termsCatalogJpaRepository.findAllById(termIds));
    }

    @Override
    public TermsCatalog findVigentTerms(String product) {
        return TermsCatalog.of(
                termsCatalogRepositoryMapper.toDomain(termsCatalogJpaRepository.findVigentTerms(product)));
    }

    @Override
    public Map<String, TermsCatalog> findVigentTermsGroupedByProduct() {
        List<TermsCatalogEntry> vigentEntries =
                termsCatalogRepositoryMapper.toDomain(termsCatalogJpaRepository.findVigentTerms(null));
        if (vigentEntries.isEmpty()) {
            return Map.of();
        }

        Map<UUID, TermsCatalogEntry> entriesById = vigentEntries.stream()
                .collect(Collectors.toMap(TermsCatalogEntry::id, Function.identity()));

        Map<String, List<TermsCatalogEntry>> entriesByProduct = new LinkedHashMap<>();
        for (TermProductProjection row : termsCatalogJpaRepository.findProductsByTermIds(
                List.copyOf(entriesById.keySet()))) {
            TermsCatalogEntry entry = entriesById.get(row.getTermId());
            if (entry != null) {
                entriesByProduct.computeIfAbsent(row.getProduct(), k -> new ArrayList<>()).add(entry);
            }
        }

        Map<String, TermsCatalog> result = new LinkedHashMap<>();
        entriesByProduct.forEach((product, entries) -> result.put(product, TermsCatalog.of(entries)));
        return result;
    }
}
