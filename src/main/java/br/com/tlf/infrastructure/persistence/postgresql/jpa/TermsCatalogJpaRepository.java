package br.com.tlf.infrastructure.persistence.postgresql.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.tlf.infrastructure.persistence.postgresql.entity.TermsCatalogJpaEntity;

public interface TermsCatalogJpaRepository extends JpaRepository<TermsCatalogJpaEntity, UUID> {

    @Query("""
        SELECT t FROM TermsCatalogJpaEntity t
        WHERE t.product = :product
          AND t.startAt <= CURRENT_TIMESTAMP
          AND (t.endAt IS NULL OR t.endAt >= CURRENT_TIMESTAMP)
    """)
    List<TermsCatalogJpaEntity> findLatestActiveByProduct(String product);
}
