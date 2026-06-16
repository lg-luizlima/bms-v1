package br.com.tlf.infrastructure.persistence.postgresql.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentJpaEntity;

public interface CustomerConsentJpaRepository extends JpaRepository<CustomerConsentJpaEntity, UUID> {

    @Query("""
        SELECT c FROM CustomerConsentJpaEntity c
        WHERE c.cpfHash = :cpfHash
          AND c.termCode = :termCode
          AND c.expiresAt > CURRENT_TIMESTAMP
        ORDER BY c.acceptedAt DESC
        LIMIT 1
    """)
    Optional<CustomerConsentJpaEntity> findActiveByCpfHashAndTermCode(String cpfHash, String termCode);
}
