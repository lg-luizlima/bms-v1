CREATE TABLE tb_term_products (
    term_id  UUID NOT NULL REFERENCES tb_terms(id),
    product  VARCHAR(100) NOT NULL,
    PRIMARY KEY (term_id, product)
);

INSERT INTO tb_term_products (term_id, product)
SELECT id, product FROM tb_terms
ON CONFLICT DO NOTHING;

ALTER TABLE tb_terms DROP COLUMN product;

ALTER TABLE tb_customer_consents ALTER COLUMN expires_at DROP NOT NULL;
