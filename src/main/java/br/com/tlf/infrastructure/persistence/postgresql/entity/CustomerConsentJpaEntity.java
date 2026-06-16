package br.com.tlf.infrastructure.persistence.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "tb_customer_consents",
    indexes = {
        @Index(name = "idx_customer_consent_cpf_hash", columnList = "cpf_hash"),
        @Index(name = "idx_customer_consent_expires_at", columnList = "expires_at")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerConsentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "cpf_hash", length = 64, nullable = false)
    private String cpfHash;

    @Column(name = "term_code", length = 100, nullable = false)
    private String termCode;

    @Column(name = "term_id", nullable = false)
    private UUID termId;

    @Column(name = "opt_in", nullable = false)
    private Boolean optIn;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audit_details", nullable = false, columnDefinition = "jsonb")
    private String auditDetails;
}
