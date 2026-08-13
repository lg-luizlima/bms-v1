ALTER TABLE tb_terms ADD COLUMN title VARCHAR(255);

-- Backfill das linhas seed (V2/V4) com os títulos usados como exemplo em
-- get_consents_arquiteture.md (Cenário 2 - "Com consentimentos pendentes").
UPDATE tb_terms SET title = 'Autorização de Consulta Vínculos DATAPREV'
WHERE id = 'f2b02f36-0194-4951-95aa-50b2efdcf1b7';

UPDATE tb_terms SET title = 'Política de Privacidade Crédito'
WHERE id = '357ae8f8-7880-4d36-bfb4-28ea637d1a29';

-- Fallback de segurança para qualquer outra linha pré-existente sem título.
UPDATE tb_terms SET title = content_summary WHERE title IS NULL;

ALTER TABLE tb_terms ALTER COLUMN title SET NOT NULL;
