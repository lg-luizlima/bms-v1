package br.com.tlf.infrastructure.persistence.postgresql.jpa;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.tlf.infrastructure.persistence.postgresql.entity.TermsCatalogJpaEntity;

public interface TermsCatalogJpaRepository extends JpaRepository<TermsCatalogJpaEntity, UUID> {

    @Query(value = """
        SELECT t.* FROM tb_terms t
        WHERE t.start_at < now()
          AND (t.end_at IS NULL OR t.end_at > now())
          AND EXISTS (
            SELECT 1 FROM tb_term_products tp
            WHERE tp.term_id = t.id AND (:product IS NULL OR tp.product = :product)
          )
        """, nativeQuery = true)
    List<TermsCatalogJpaEntity> findVigentTerms(@Param("product") String product);
}
