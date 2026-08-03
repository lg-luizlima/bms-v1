CREATE TABLE IF NOT EXISTS tb_terms (
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

CREATE TABLE IF NOT EXISTS tb_customer_consents (
    id             UUID PRIMARY KEY,
    cpf_hash       VARCHAR(64) NOT NULL,
    term_code      VARCHAR(100) NOT NULL,
    term_id        UUID NOT NULL,
    opt_in         BOOLEAN NOT NULL,
    accepted_at    TIMESTAMPTZ NOT NULL,
    expires_at     TIMESTAMPTZ NOT NULL,
    audit_details  JSONB NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_customer_consent_cpf_hash ON tb_customer_consents (cpf_hash);
CREATE INDEX IF NOT EXISTS idx_customer_consent_expires_at ON tb_customer_consents (expires_at);

-- V2 (seed de tb_terms) e V3 (trace_context em tb_outbox_events) dependem de tb_terms e
-- tb_outbox_events já existirem — se V2 chegou a falhar em HML por causa da tb_terms
-- ausente, ela nunca reaplica sozinha, então repetimos o mesmo INSERT idempotente e a
-- mesma coluna aqui, cobrindo o cenário em que V2/V3 nunca completaram lá.

INSERT INTO tb_terms (id, product, term_code, version, is_mandatory, validity_days, revoke_previous_versions, content_type, content_summary, content_text, content_url, start_at, end_at)
VALUES
('357ae8f8-7880-4d36-bfb4-28ea637d1a29', 'CONSIGNADO_DATAPREV', 'DATAPREV_OPTIONAL_CONSENT', '1.0', false, 30, false, 'TEXTO', 'Consentimento opcional', NULL, NULL, '2026-07-22 21:02:23.443014+00', NULL),
('f2b02f36-0194-4951-95aa-50b2efdcf1b7', 'CONSIGNADO_DATAPREV', 'DATAPREV_CONSENT', '1.0', true, 30, true, 'TEXTO', 'Consentimento obrigatorio Dataprev', NULL, NULL, '2026-07-22 21:02:23.443014+00', NULL)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE tb_outbox_events
    ADD COLUMN IF NOT EXISTS trace_context VARCHAR(64);
