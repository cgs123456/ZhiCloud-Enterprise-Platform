-- V150 验证：tms_shipment 表与全部字符列统一为 utf8mb4_unicode_ci
SELECT CASE WHEN
    (SELECT TABLE_COLLATION FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tms_shipment') = 'utf8mb4_unicode_ci'
AND (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tms_shipment'
        AND COLLATION_NAME IS NOT NULL
        AND COLLATION_NAME <> 'utf8mb4_unicode_ci') = 0
THEN 'V150_TMS_SHIPMENT_COLLATION PASS'
ELSE 'V150_TMS_SHIPMENT_COLLATION FAIL' END AS check_result;