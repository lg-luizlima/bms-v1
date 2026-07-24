INSERT INTO tb_terms (id, product, term_code, version, is_mandatory, validity_days, revoke_previous_versions, content_type, content_summary, content_text, content_url, start_at, end_at)
VALUES
('357ae8f8-7880-4d36-bfb4-28ea637d1a29', 'CONSIGNADO_DATAPREV', 'DATAPREV_OPTIONAL_CONSENT', '1.0', false, 30, false, 'TEXTO', 'Consentimento opcional', NULL, NULL, '2026-07-22 21:02:23.443014+00', NULL),
('f2b02f36-0194-4951-95aa-50b2efdcf1b7', 'CONSIGNADO_DATAPREV', 'DATAPREV_CONSENT', '1.0', true, 30, true, 'TEXTO', 'Consentimento obrigatorio Dataprev', NULL, NULL, '2026-07-22 21:02:23.443014+00', NULL)
ON CONFLICT (id) DO NOTHING;
