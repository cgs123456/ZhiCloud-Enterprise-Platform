-- ============================================================
-- V13: QMS 模块业务表二级索引补充（P0-5）
-- ============================================================
-- 背景：qms.sql 与 qms_ext.sql 中 11 张业务表完全无二级索引。
-- 覆盖索引：
--   1. (tenant_id, deleted)         —— 多租户 + 软删除复合过滤
--   2. (create_time)                —— 按创建时间范围查询
--   3. (code/order_no/capa_no/ncr_no/fmea_no/study_no, tenant_id) UNIQUE —— 单据号唯一约束
--   4. (status)                     —— 业务单据状态过滤
--   5. (type) / (supplier_id) / (product_id) / (work_order_id) / (inspection_order_id) 等外键索引
-- 复用幂等存储过程 p_add_index_if_not_exists / p_add_unique_if_not_exists
-- ============================================================

DROP PROCEDURE IF EXISTS p_add_index_if_not_exists;
DELIMITER $$
CREATE PROCEDURE p_add_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS p_add_unique_if_not_exists;
DELIMITER $$
CREATE PROCEDURE p_add_unique_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) AND EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE KEY `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- 1. 基础 QMS（qms.sql 4 张）
-- ============================================================
-- qms_inspection_item
CALL p_add_index_if_not_exists('qms_inspection_item', 'idx_qms_inspection_item_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_inspection_item', 'idx_qms_inspection_item_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_inspection_item', 'idx_qms_inspection_item_status', 'status');
CALL p_add_index_if_not_exists('qms_inspection_item', 'idx_qms_inspection_item_type', 'type');
CALL p_add_index_if_not_exists('qms_inspection_item', 'idx_qms_inspection_item_method', 'method');
CALL p_add_unique_if_not_exists('qms_inspection_item', 'uk_qms_inspection_item_code_tenant', 'code, tenant_id');

-- qms_inspection_order
CALL p_add_index_if_not_exists('qms_inspection_order', 'idx_qms_inspection_order_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_inspection_order', 'idx_qms_inspection_order_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_inspection_order', 'idx_qms_inspection_order_status', 'status');
CALL p_add_index_if_not_exists('qms_inspection_order', 'idx_qms_inspection_order_type', 'type');
CALL p_add_index_if_not_exists('qms_inspection_order', 'idx_qms_inspection_order_supplier_id', 'supplier_id');
CALL p_add_index_if_not_exists('qms_inspection_order', 'idx_qms_inspection_order_work_order_id', 'work_order_id');
CALL p_add_index_if_not_exists('qms_inspection_order', 'idx_qms_inspection_order_product_id', 'product_id');
CALL p_add_index_if_not_exists('qms_inspection_order', 'idx_qms_inspection_order_batch_no', 'batch_no');
CALL p_add_index_if_not_exists('qms_inspection_order', 'idx_qms_inspection_order_inspect_time', 'inspect_time');
CALL p_add_unique_if_not_exists('qms_inspection_order', 'uk_qms_inspection_order_no_tenant', 'order_no, tenant_id');

-- qms_inspection_record
CALL p_add_index_if_not_exists('qms_inspection_record', 'idx_qms_inspection_record_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_inspection_record', 'idx_qms_inspection_record_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_inspection_record', 'idx_qms_inspection_record_order_id', 'order_id');
CALL p_add_index_if_not_exists('qms_inspection_record', 'idx_qms_inspection_record_item_id', 'item_id');
CALL p_add_index_if_not_exists('qms_inspection_record', 'idx_qms_inspection_record_result', 'result');
CALL p_add_index_if_not_exists('qms_inspection_record', 'idx_qms_inspection_record_inspect_time', 'inspect_time');

-- qms_capa_document
CALL p_add_index_if_not_exists('qms_capa_document', 'idx_qms_capa_document_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_capa_document', 'idx_qms_capa_document_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_capa_document', 'idx_qms_capa_document_status', 'status');
CALL p_add_index_if_not_exists('qms_capa_document', 'idx_qms_capa_document_source', 'source');
CALL p_add_index_if_not_exists('qms_capa_document', 'idx_qms_capa_document_due_date', 'due_date');
CALL p_add_unique_if_not_exists('qms_capa_document', 'uk_qms_capa_document_no_tenant', 'capa_no, tenant_id');

-- ============================================================
-- 2. 扩展 QMS（qms_ext.sql 7 张）
-- ============================================================
-- qms_ncr_document
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_status', 'status');
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_source', 'source');
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_inspection_order_id', 'inspection_order_id');
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_product_id', 'product_id');
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_supplier_id', 'supplier_id');
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_work_order_id', 'work_order_id');
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_defect_level', 'defect_level');
CALL p_add_index_if_not_exists('qms_ncr_document', 'idx_qms_ncr_document_disposition', 'disposition');
CALL p_add_unique_if_not_exists('qms_ncr_document', 'uk_qms_ncr_document_no_tenant', 'ncr_no, tenant_id');

-- qms_ncr_mrb_record
CALL p_add_index_if_not_exists('qms_ncr_mrb_record', 'idx_qms_ncr_mrb_record_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_ncr_mrb_record', 'idx_qms_ncr_mrb_record_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_ncr_mrb_record', 'idx_qms_ncr_mrb_record_ncr_id', 'ncr_id');
CALL p_add_index_if_not_exists('qms_ncr_mrb_record', 'idx_qms_ncr_mrb_record_mrb_date', 'mrb_date');

-- qms_fmea_document
CALL p_add_index_if_not_exists('qms_fmea_document', 'idx_qms_fmea_document_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_fmea_document', 'idx_qms_fmea_document_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_fmea_document', 'idx_qms_fmea_document_status', 'status');
CALL p_add_index_if_not_exists('qms_fmea_document', 'idx_qms_fmea_document_fmea_type', 'fmea_type');
CALL p_add_index_if_not_exists('qms_fmea_document', 'idx_qms_fmea_document_product_id', 'product_id');
CALL p_add_index_if_not_exists('qms_fmea_document', 'idx_qms_fmea_document_process_id', 'process_id');
CALL p_add_unique_if_not_exists('qms_fmea_document', 'uk_qms_fmea_document_no_tenant', 'fmea_no, tenant_id');

-- qms_fmea_item
CALL p_add_index_if_not_exists('qms_fmea_item', 'idx_qms_fmea_item_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_fmea_item', 'idx_qms_fmea_item_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_fmea_item', 'idx_qms_fmea_item_fmea_id', 'fmea_id');
CALL p_add_index_if_not_exists('qms_fmea_item', 'idx_qms_fmea_item_rpn', 'rpn');
CALL p_add_index_if_not_exists('qms_fmea_item', 'idx_qms_fmea_item_severity', 'severity');

-- qms_msa_study
CALL p_add_index_if_not_exists('qms_msa_study', 'idx_qms_msa_study_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_msa_study', 'idx_qms_msa_study_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_msa_study', 'idx_qms_msa_study_status', 'status');
CALL p_add_index_if_not_exists('qms_msa_study', 'idx_qms_msa_study_study_type', 'study_type');
CALL p_add_index_if_not_exists('qms_msa_study', 'idx_qms_msa_study_equipment_id', 'equipment_id');
CALL p_add_unique_if_not_exists('qms_msa_study', 'uk_qms_msa_study_no_tenant', 'study_no, tenant_id');

-- qms_msa_measurement
CALL p_add_index_if_not_exists('qms_msa_measurement', 'idx_qms_msa_measurement_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_msa_measurement', 'idx_qms_msa_measurement_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_msa_measurement', 'idx_qms_msa_measurement_study_id', 'study_id');
CALL p_add_index_if_not_exists('qms_msa_measurement', 'idx_qms_msa_measurement_appraiser_id', 'appraiser_id');

-- qms_electronic_signature_log
CALL p_add_index_if_not_exists('qms_electronic_signature_log', 'idx_qms_electronic_signature_log_tenant_deleted', 'tenant_id, deleted');
CALL p_add_index_if_not_exists('qms_electronic_signature_log', 'idx_qms_electronic_signature_log_create_time', 'create_time');
CALL p_add_index_if_not_exists('qms_electronic_signature_log', 'idx_qms_electronic_signature_log_user_id', 'user_id');
CALL p_add_index_if_not_exists('qms_electronic_signature_log', 'idx_qms_electronic_signature_log_signature_time', 'signature_time');
CALL p_add_index_if_not_exists('qms_electronic_signature_log', 'idx_qms_electronic_signature_log_operation_type', 'operation_type');

-- ============================================================
-- 清理存储过程
-- ============================================================
DROP PROCEDURE IF EXISTS p_add_index_if_not_exists;
DROP PROCEDURE IF EXISTS p_add_unique_if_not_exists;
