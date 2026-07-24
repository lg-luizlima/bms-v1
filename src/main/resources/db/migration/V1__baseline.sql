CREATE TABLE tb_terms (
    id                        UUID PRIMARY KEY,
    product                   VARCHAR(100) NOT NULL,
    term_code                 VARCHAR(100) NOT NULL,
    version                   VARCHAR NOT NULL,
    is_mandatory              BOOLEAN NOT NULL,
    validity_days             INTEGER,
    revoke_previous_versions  BOOLEAN NOT NULL,
    content_type              VARCHAR(100) NOT NULL,
    content_summary           VARCHAR NOT NULL,
    content_text              TEXT,
    content_url               VARCHAR(500),
    start_at                  TIMESTAMPTZ NOT NULL,
    end_at                    TIMESTAMPTZ
);

CREATE TABLE tb_customer_consents (
    id             UUID PRIMARY KEY,
    cpf_hash       VARCHAR(64) NOT NULL,
    term_code      VARCHAR(100) NOT NULL,
    term_id        UUID NOT NULL,
    opt_in         BOOLEAN NOT NULL,
    accepted_at    TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    audit_details  JSONB NOT NULL
);

CREATE INDEX idx_customer_consent_cpf_hash ON tb_customer_consents (cpf_hash);
CREATE INDEX idx_customer_consent_expires_at ON tb_customer_consents (expires_at);

CREATE TABLE tb_outbox_events (
    event_id        UUID PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(100) NOT NULL,
    topic_name      VARCHAR(200) NOT NULL,
    payload         JSONB NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
