ALTER TABLE tb_customer_consents
    ADD CONSTRAINT fk_customer_consent_term
        FOREIGN KEY (term_id)
            REFERENCES tb_terms(id);

CREATE INDEX idx_customer_consent_term_id
    ON tb_customer_consents(term_id);
