CREATE TABLE IF NOT EXISTS Terms_Catalog (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    product                     VARCHAR(100)    NOT NULL,
    term_code                   VARCHAR(100)    NOT NULL,
    version                     VARCHAR(5)       NOT NULL,
    is_mandatory                BOOLEAN         NOT NULL,
    validity_days               INTEGER,
    revoke_previous_versions    BOOLEAN         NOT NULL,
    content_type                VARCHAR(50)     NOT NULL,
    content_summary             TEXT            NOT NULL,
    content_text                TEXT,
    content_url                 VARCHAR(500),
    start_at                    TIMESTAMP       NOT NULL,
    end_at                      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_terms_catalog_product ON Terms_Catalog (product);

CREATE TABLE IF NOT EXISTS Customer_Consent (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    cpf_hash        VARCHAR(64)     NOT NULL,
    term_code       VARCHAR(100)    NOT NULL,
    term_id         UUID            NOT NULL REFERENCES Terms_Catalog (id),
    opt_in          BOOLEAN         NOT NULL,
    accepted_at     TIMESTAMP       NOT NULL,
    expires_at      TIMESTAMP,
    audit_details   JSONB
);

CREATE INDEX IF NOT EXISTS idx_customer_consent_cpf_hash ON Customer_Consent (cpf_hash);
CREATE INDEX IF NOT EXISTS idx_customer_consent_expires_at ON Customer_Consent (expires_at);

CREATE TABLE IF NOT EXISTS Outbox_Event_Queue (
    event_id        UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(100)    NOT NULL,
    aggregate_id    VARCHAR(100)    NOT NULL,
    topic_name      VARCHAR(200)    NOT NULL,
    payload         JSONB           NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Seed data

INSERT INTO Terms_Catalog (id, product, term_code, version, is_mandatory, validity_days, revoke_previous_versions, content_type, content_summary, content_text, content_url, start_at, end_at)
VALUES (
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    'CONSIGNADO_DATAPREV',
    'DATAPREV_CONSENT',
    '2.0',
    TRUE,
    365,
    TRUE,
    'URL',
    'Termo de consentimento para consulta de dados previdenciários via Dataprev.',
    NULL,
    'http://127.0.0.1:8083/',
    '2025-01-01 00:00:00',
    NULL
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO Customer_Consent (id, cpf_hash, term_code, term_id, opt_in, accepted_at, expires_at, audit_details)
VALUES (
    'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
    'b94f6f125179b62fc35c9a6a5f5b96b8f8b23b4d9a6a5f5b96b8f8b23b4d9a6',
    'DATAPREV_CONSENT',
    'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
    TRUE,
    '2025-03-10 14:32:00',
    '2026-03-10 14:32:00',
    '{
        "signature": {
            "ip": "127.0.0.1",
            "userAgent": "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15",
            "deviceId": "abc-123-xyz-987",
            "channel": "APP",
            "geolocation": {
                "lat": "-23.5505",
                "long": "-46.6333"
            }
        }
    }'
)
ON CONFLICT (id) DO NOTHING;
