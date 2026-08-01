-- ======================== 质量管理系统（QMS）回滚脚本 ========================
-- 作者：yudao
-- 说明：回滚 QMS 模块建表脚本（qms.sql），删除 5 张表

DROP TABLE IF EXISTS qms_inspection_record;
DROP TABLE IF EXISTS qms_inspection_order;
DROP TABLE IF EXISTS qms_inspection_item;
DROP TABLE IF EXISTS qms_eight_d_report;
DROP TABLE IF EXISTS qms_capa_document;
