package br.com.tlf.infrastructure.persistence.postgresql.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.tlf.infrastructure.persistence.postgresql.entity.TermsCatalogEntity;

public interface TermsCatalogJpaRepository extends JpaRepository<TermsCatalogEntity, UUID> {

    @Query("""
        SELECT t FROM TermsCatalogEntity t
        WHERE t.product = :product
          AND t.startAt <= CURRENT_DATE
          AND (t.endAt IS NULL OR t.endAt >= CURRENT_DATE)
    """)
    List<TermsCatalogEntity> findLatestActiveByProduct(String product);
}
