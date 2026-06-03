package br.com.tlf.infrastructure.persistence.postgresql.jpa;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.tlf.infrastructure.persistence.postgresql.entity.CustomerConsentEntity;

public interface CustomerConsentJpaRepository extends JpaRepository<CustomerConsentEntity, UUID> {

    @Query("""
        SELECT c FROM CustomerConsentEntity c
        WHERE c.cpfHash = :cpfHash
          AND c.termCode = :termCode
          AND c.expiresAt > CURRENT_TIMESTAMP
        ORDER BY c.acceptedAt DESC
        LIMIT 1
    """)
    Optional<CustomerConsentEntity> findActiveByCpfHashAndTermCode(String cpfHash, String termCode);
}
