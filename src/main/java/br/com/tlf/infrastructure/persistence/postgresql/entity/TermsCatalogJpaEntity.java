package br.com.tlf.infrastructure.persistence.postgresql.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_terms")
@Getter
@NoArgsConstructor
public class TermsCatalogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "product", length = 100, nullable = false)
    private String product;

    @Column(name = "term_code", length = 100, nullable = false)
    private String termCode;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "version", nullable = false)
    private String version;

    @Column(name = "is_mandatory", nullable = false)
    private Boolean isMandatory;

    @Column(name = "validity_days")
    private Integer validityDays;

    @Column(name = "revoke_previous_versions", nullable = false)
    private Boolean revokePreviousVersions;

    @Column(name = "content_type", length = 100, nullable = false)
    private String contentType;

    @Column(name = "content_summary", nullable = false)
    private String contentSummary;

    @Column(name = "content_text", columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "content_url", length = 500)
    private String contentUrl;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;
}
