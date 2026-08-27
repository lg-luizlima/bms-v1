ALTER TABLE tb_terms
    ADD COLUMN requires_post_processing BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN template_id VARCHAR(255);
