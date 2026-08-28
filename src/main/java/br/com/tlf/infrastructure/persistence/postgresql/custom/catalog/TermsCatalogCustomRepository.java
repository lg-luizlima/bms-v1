package br.com.tlf.infrastructure.persistence.postgresql.custom.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import br.com.tlf.core.domain.terms.TermsCatalog;
import br.com.tlf.core.domain.terms.TermsCatalogEntry;
import br.com.tlf.core.port.out.termscatalog.TermsCatalogRepository;
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
}
