DROP INDEX IF EXISTS idx_customer_consent_cpf_hash;

ALTER TABLE tb_customer_consents
    RENAME COLUMN cpf_hash TO cpf;

CREATE INDEX idx_customer_consent_cpf
    ON tb_customer_consents (cpf);