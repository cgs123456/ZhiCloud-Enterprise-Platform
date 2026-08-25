-- ============================================================
-- V5: QMS CAPA 全流程扩展（P0-4）
--
-- 为 qms_capa_document 表新增优先级、阶段、有效性验证相关字段，支持
-- CAPA 全流程状态机：已创建 → 根本原因分析 → 纠正措施 → 预防措施
--                → 有效性验证 → 已关闭
--
-- 字段映射：
--   priority             优先级（10 高 / 20 中 / 30 低）
--   stage                当前阶段（10-60，对应 CAPAStageEnum）
--   verification_result  验证结果（10 待验证 / 20 通过 / 30 不通过）
--   verification_comment 验证意见
--   verified_by          验证人
--   verified_time        验证时间
--
-- 兼容性：
--   1) 所有新增字段允许 NULL，不影响历史数据；
--   2) stage 默认值设为 10（CREATED），与 status=10(OPEN) 保持一致；
--   3) priority 默认值设为 20（MEDIUM）。
-- ============================================================

-- 1. 扩展字段（拆分为多个 ALTER 语句，避免同语句内 AFTER 依赖链问题）
ALTER TABLE qms_capa_document
    ADD COLUMN priority TINYINT DEFAULT 20 COMMENT '优先级（10 高 / 20 中 / 30 低）' AFTER source;

ALTER TABLE qms_capa_document
    ADD COLUMN stage TINYINT DEFAULT 10 COMMENT '当前阶段（10 已创建 / 20 根本原因分析 / 30 纠正措施 / 40 预防措施 / 50 有效性验证 / 60 已关闭）' AFTER priority;

ALTER TABLE qms_capa_document
    ADD COLUMN verification_result TINYINT COMMENT '有效性验证结果（10 待验证 / 20 通过 / 30 不通过）' AFTER status,
    ADD COLUMN verification_comment TEXT COMMENT '有效性验证意见' AFTER verification_result,
    ADD COLUMN verified_by VARCHAR(64) COMMENT '验证人' AFTER verification_comment,
    ADD COLUMN verified_time DATETIME COMMENT '验证时间' AFTER verified_by;

-- 2. 为现有 CAPA 数据回填默认阶段/优先级（依赖字段已添加）
UPDATE qms_capa_document
SET stage = 10,
    priority = 20
WHERE stage IS NULL;

-- 3. 字典初始化（仅在字典表存在的场景下执行）
-- 注意：使用 IF NOT EXISTS 防止重复插入；MySQL 8 支持 INSERT IGNORE 但不支持 ON DUPLICATE KEY UPDATE 配合 NOT EXISTS
INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('CAPA 优先级', 'qms_capa_priority', 0, 'admin', NOW(), 'admin', NOW(), 0, 'CAPA 文档优先级');

INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('CAPA 阶段', 'qms_capa_stage', 0, 'admin', NOW(), 'admin', NOW(), 0, 'CAPA 流程阶段');

INSERT IGNORE INTO system_dict_type (name, type, status, creator, create_time, updater, update_time, deleted, remark)
VALUES ('CAPA 验证结果', 'qms_capa_verification_result', 0, 'admin', NOW(), 'admin', NOW(), 0, 'CAPA 有效性验证结果');

-- 字典数据初始化（每条 INSERT 使用 NOT EXISTS 子查询保证可重入）
-- 优先级
INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '高', '10', 'qms_capa_priority', 0, 'admin', NOW(), 'admin', NOW(), 0, '高优先级'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_priority' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '中', '20', 'qms_capa_priority', 0, 'admin', NOW(), 'admin', NOW(), 0, '中优先级'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_priority' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '低', '30', 'qms_capa_priority', 0, 'admin', NOW(), 'admin', NOW(), 0, '低优先级'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_priority' AND value='30');

-- 阶段
INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '已创建', '10', 'qms_capa_stage', 0, 'admin', NOW(), 'admin', NOW(), 0, 'CAPA 已创建'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_stage' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '根本原因分析', '20', 'qms_capa_stage', 0, 'admin', NOW(), 'admin', NOW(), 0, '根本原因分析阶段'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_stage' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '纠正措施', '30', 'qms_capa_stage', 0, 'admin', NOW(), 'admin', NOW(), 0, '纠正措施阶段'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_stage' AND value='30');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 4, '预防措施', '40', 'qms_capa_stage', 0, 'admin', NOW(), 'admin', NOW(), 0, '预防措施阶段'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_stage' AND value='40');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 5, '有效性验证', '50', 'qms_capa_stage', 0, 'admin', NOW(), 'admin', NOW(), 0, '有效性验证阶段'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_stage' AND value='50');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 6, '已关闭', '60', 'qms_capa_stage', 0, 'admin', NOW(), 'admin', NOW(), 0, 'CAPA 已关闭'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_stage' AND value='60');

-- 验证结果
INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 1, '待验证', '10', 'qms_capa_verification_result', 0, 'admin', NOW(), 'admin', NOW(), 0, '待验证'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_verification_result' AND value='10');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 2, '通过', '20', 'qms_capa_verification_result', 0, 'admin', NOW(), 'admin', NOW(), 0, '验证通过'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_verification_result' AND value='20');

INSERT INTO system_dict_data (sort, label, value, dict_type, status, creator, create_time, updater, update_time, deleted, remark)
SELECT 3, '不通过', '30', 'qms_capa_verification_result', 0, 'admin', NOW(), 'admin', NOW(), 0, '验证不通过'
WHERE NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type='qms_capa_verification_result' AND value='30');

-- 4. CAPA 阶段流转菜单（仅注册，权限由前端按需分配）
INSERT IGNORE INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES ('CAPA 阶段流转', 'qms:capa:transition', 3, 10, 0, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
