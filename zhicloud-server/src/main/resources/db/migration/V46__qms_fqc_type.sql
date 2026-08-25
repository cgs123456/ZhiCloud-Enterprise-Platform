-- ============================================================
-- V46: QMS 检验类型补全 - 新增 FQC 成品检验
--
-- 背景：原 qms_inspection_order.type 字段仅支持 IQC(10)/IPQC(20)/OQC(30)，
--      缺失 FQC 成品检验类型。为保持向后兼容（已有 OQC=30 数据），
--      新增 FQC 取值 35，不调整原有取值。
--
-- 说明：本脚本仅补充字段注释，不修改列结构与既有数据；
--      type 取值由应用层 InspectionTypeEnum 约束，无需 DDL 枚举改造。
-- ============================================================

ALTER TABLE `qms_inspection_order`
    MODIFY COLUMN `type` TINYINT NOT NULL COMMENT '检验类型（10 IQC 来料检验 20 IPQC 过程检验 35 FQC 成品检验 30 OQC 出货检验）';

ALTER TABLE `qms_inspection_item`
    MODIFY COLUMN `type` TINYINT NOT NULL COMMENT '检验类型（10 IQC 来料检验 20 IPQC 过程检验 35 FQC 成品检验 30 OQC 出货检验）';