-- V196 验证：qms_document 唯一键改为 (tenant_id, doc_no, version)

SELECT CASE WHEN COUNT(*) = 0 THEN 'V196_OLD_UK_REMOVED PASS'
            ELSE CONCAT('V196_OLD_UK_REMOVED FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'qms_document'
   AND index_name = 'uk_tenant_doc_no';

SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V196_NEW_UK_PRESENT PASS'
            ELSE CONCAT('V196_NEW_UK_PRESENT FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'qms_document'
   AND index_name = 'uk_tenant_doc_no_version';

SELECT CASE WHEN COUNT(*) = 3 THEN 'V196_NEW_UK_COLUMNS PASS'
            ELSE CONCAT('V196_NEW_UK_COLUMNS FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'qms_document'
   AND index_name = 'uk_tenant_doc_no_version'
   AND column_name IN ('tenant_id', 'doc_no', 'version');
