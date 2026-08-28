package br.com.tlf.infrastructure.persistence.postgresql.jpa;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentJpaEntity;

public interface CustomerConsentJpaRepository extends JpaRepository<CustomerConsentJpaEntity, UUID> {

    @Query("""
        SELECT c FROM CustomerConsentJpaEntity c
        WHERE c.cpf = :cpf
          AND c.termCode IN :termCodes
          AND (c.expiresAt IS NULL OR c.expiresAt > CURRENT_TIMESTAMP)
        ORDER BY c.acceptedAt DESC
    """)
    List<CustomerConsentJpaEntity> findActiveByCpfAndTermCodes(@Param("cpf") String cpf,
            @Param("termCodes") Collection<String> termCodes);
}
