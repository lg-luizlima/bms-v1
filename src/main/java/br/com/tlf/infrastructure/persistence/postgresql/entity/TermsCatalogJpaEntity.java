package br.com.tlf.infrastructure.persistence.postgresql.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(
    name = "tb_terms",
    indexes = {
        @Index(name = "idx_terms_term_code", columnList = "term_code"),
        @Index(name = "idx_terms_start_end", columnList = "start_at,end_at")
    }
)
@Getter
@NoArgsConstructor
public class TermsCatalogJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

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

    @Column(name = "requires_post_processing", nullable = false)
    private Boolean requiresPostProcessing;

    @Column(name = "template_id", length = 255)
    private String templateId;
}
