CREATE TABLE tb_terms (
                          id                          UUID PRIMARY KEY,
                          term_code                   VARCHAR(100) NOT NULL,
                          version                     VARCHAR NOT NULL,
                          title                       VARCHAR(255) NOT NULL,
                          is_mandatory                BOOLEAN NOT NULL,
                          validity_days               INTEGER,
                          revoke_previous_versions    BOOLEAN NOT NULL,
                          content_type                VARCHAR(100) NOT NULL,
                          content_summary             VARCHAR NOT NULL,
                          content_text                TEXT,
                          content_url                 VARCHAR(500),
                          start_at                    TIMESTAMPTZ NOT NULL,
                          end_at                      TIMESTAMPTZ,
                          requires_post_processing    BOOLEAN NOT NULL DEFAULT FALSE,
                          template_id                 VARCHAR(255)
);

CREATE INDEX idx_tb_terms_start_end ON tb_terms (start_at, end_at);
CREATE INDEX idx_terms_term_code ON tb_terms(term_code);

CREATE TABLE tb_term_products (
                                  id      UUID PRIMARY KEY,
                                  term_id UUID NOT NULL REFERENCES tb_terms(id),
                                  product VARCHAR(100) NOT NULL
);

CREATE INDEX idx_tb_term_products_product_term_id ON tb_term_products (product, term_id);

CREATE TABLE tb_customer_consents (
                                      id             UUID PRIMARY KEY,
                                      cpf            VARCHAR(64) NOT NULL,
                                      term_code      VARCHAR(100) NOT NULL,
                                      term_id        UUID NOT NULL,
                                      opt_in         BOOLEAN NOT NULL,
                                      accepted_at    TIMESTAMPTZ NOT NULL,
                                      expires_at     TIMESTAMPTZ,
                                      audit_details  JSONB NOT NULL
);

CREATE INDEX idx_customer_consent_cpf ON tb_customer_consents (cpf);

CREATE INDEX idx_customer_consent_expires_at ON tb_customer_consents (expires_at);

CREATE TABLE tb_outbox_events (
                                  event_id       UUID PRIMARY KEY,
                                  aggregate_type VARCHAR(100) NOT NULL,
                                  aggregate_id   VARCHAR(100) NOT NULL,
                                  topic_name     VARCHAR(200) NOT NULL,
                                  payload        JSONB NOT NULL,
                                  trace_context  VARCHAR(64),
                                  created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO tb_terms (
    id,
    term_code,
    version,
    title,
    is_mandatory,
    validity_days,
    revoke_previous_versions,
    content_type,
    content_summary,
    content_text,
    content_url,
    start_at,
    end_at,
    requires_post_processing,
    template_id
)
VALUES
    (
        '357ae8f8-7880-4d36-bfb4-28ea637d1a29',
        'GENERAL_CREDIT_TERMS',
        '1.0',
        'Termos e Condições',
        TRUE,
        NULL,
        FALSE,
        'PDF',
        'Consentimento geral dos produtos de crédito',
        NULL,
        'https://fintech-hml.vivo.com.br/files/termo-geral-de-credito.pdf',
        '2026-07-22 21:02:23.443014+00',
        NULL,
        TRUE,
        '6bc2e827b97babb0166adf6c8d17a58143d5dde19b5e41c3902e98403c518f5a'
    ),
    (
        '42f8e9c1-3b6a-4d5e-9f7c-8b9e6d7a8c9e',
        'COMMUNICATION_TERMS',
        '1.0',
        'Termos e Condições de Comunicação',
        FALSE,
        NULL,
        FALSE,
        'PDF',
        'Consentimento geral de comunicação',
        NULL,
        'https://fintech-hml.vivo.com.br/files/termo-de-comunicacao.pdf',
        '2026-07-22 21:02:23.443014+00',
        NULL,
        FALSE,
        '74262c6a5ba5aebc77f587ebd2fcb440dcbc6e59f6936f2cd6b3dbfd13f091a0'
    ),
    (
        'f2b02f36-0194-4951-95aa-50b2efdcf1b7',
        'DATAPREV_CONSENT',
        '1.0',
        'Autorização de Consulta Vínculos DATAPREV',
        TRUE,
        30,
        TRUE,
        'PDF',
        'Consentimento obrigatorio Dataprev',
        NULL,
        'https://fintech-hml.vivo.com.br/files/termo-de-autorizacao-dataprev.pdf',
        '2026-07-22 21:02:23.443014+00',
        NULL,
        TRUE,
        '8c6d8985dcb239b59ab8077f5476e06dd7b499c9cc5fff4e307e1d20bb9668fe'
    )
    ON CONFLICT (id) DO NOTHING;

INSERT INTO tb_term_products (id,term_id, product)
VALUES
    (
        '4e4f61f9-1dfc-4314-b98b-f6cbf6727311',
        '357ae8f8-7880-4d36-bfb4-28ea637d1a29',
        'CREDITO_PESSOAL'
    ),
    (
        '332c7a68-71b1-437c-95c6-fb5c239f8fd5',
        '357ae8f8-7880-4d36-bfb4-28ea637d1a29',
        'ADIANTAMENTO_FGTS'
    ),
    (
        '2341c9e2-5f3b-4d1a-8c7e-9f6d5a4b3c2e',
        '357ae8f8-7880-4d36-bfb4-28ea637d1a29',
        'CREDIARIO'
    ),
    (
        '988f7e1a-2b3c-4d5e-9f6a-7b8c9d0e1f2a',
        '42f8e9c1-3b6a-4d5e-9f7c-8b9e6d7a8c9e',
        'COMMUNICATION_SERVICE'
    ),
    (
        '8c6d8985-dcb2-39b5-9ab8-077f5476e06d',
        'f2b02f36-0194-4951-95aa-50b2efdcf1b7',
        'CONSIGNADO_DATAPREV'
    )
    ON CONFLICT DO NOTHING;
