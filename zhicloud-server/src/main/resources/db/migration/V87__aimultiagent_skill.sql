-- ======================== 多 Agent 编排：两级技能目录（Skill Group + Skill） ========================
-- 作者：zhicloud
-- 说明：P1-B。技能两级结构：Level-1 技能分组（业务域）→ Level-2 具体技能（绑定 WorkerToolExecutor 工具名）。
--       Worker 启动时由 SkillCatalogValidator 校验 getSupportedTools() 与本目录的一致性（缺失仅告警，不阻断）。
--       skill.config_json 中的 endpoint_url（如有）在写入时经 MultiAgentSsrfGuard 做 SSRF 校验。
-- 约定：沿用 aimultiagent_* 表通用列（creator/create_time/updater/update_time/deleted/tenant_id）。
--       超级管理员自动拥有全部菜单，无需补 system_role_menu。

-- ----------------------------
-- Level-1：技能分组表
-- ----------------------------
CREATE TABLE IF NOT EXISTS aimultiagent_skill_group (
    id BIGINT PRIMARY KEY COMMENT '主键',
    code VARCHAR(64) NOT NULL COMMENT '分组编码（wms/qms/procurement/sales/report/ai_rag_eval）',
    name VARCHAR(64) NOT NULL COMMENT '分组名称',
    description VARCHAR(255) COMMENT '分组描述',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0开启 1关闭）',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT '多 Agent 技能分组（Level-1）';

CREATE UNIQUE INDEX uk_skill_group_code ON aimultiagent_skill_group(code);

-- ----------------------------
-- Level-2：技能表
-- ----------------------------
CREATE TABLE IF NOT EXISTS aimultiagent_skill (
    id BIGINT PRIMARY KEY COMMENT '主键',
    group_id BIGINT NOT NULL COMMENT '所属分组 aimultiagent_skill_group.id',
    code VARCHAR(64) NOT NULL COMMENT '技能编码（组内唯一，如 wms:receipt_order_list）',
    name VARCHAR(64) NOT NULL COMMENT '技能名称',
    tool_name VARCHAR(64) COMMENT '绑定的 WorkerToolExecutor 工具名（为空表示纯提示词技能，无真实工具）',
    description VARCHAR(255) COMMENT '技能描述',
    config_json TEXT COMMENT '技能配置 JSON（可选 endpoint_url，写入时做 SSRF 校验）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0开启 1关闭）',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT '多 Agent 技能（Level-2）';

CREATE UNIQUE INDEX uk_skill_code ON aimultiagent_skill(code);
CREATE INDEX idx_skill_group ON aimultiagent_skill(group_id);
CREATE INDEX idx_skill_tool_name ON aimultiagent_skill(tool_name);

-- ----------------------------
-- 种子数据：Level-1 分组（与 SkillCategory 枚举保持一致）
-- ----------------------------
INSERT IGNORE INTO aimultiagent_skill_group (id, code, name, description, status, sort, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
    (1, 'wms', '仓储物流', 'WMS 仓储物流模块技能', 0, 1, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (2, 'qms', '质量检测', 'QMS 质量检测模块技能', 0, 2, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (3, 'procurement', '采购', '采购域技能', 0, 3, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (4, 'sales', '销售', '销售域技能', 0, 4, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (5, 'report', '报告生成', '纯提示词报告生成技能', 0, 5, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (6, 'ai_rag_eval', 'RAG评估', 'AI RAG 评估模块技能', 0, 6, 'admin', NOW(), 'admin', NOW(), 0, 0);

-- ----------------------------
-- 种子数据：Level-2 技能（绑定现有 WorkerToolExecutor 工具名）
-- ----------------------------
INSERT IGNORE INTO aimultiagent_skill (id, group_id, code, name, tool_name, description, status, sort, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
    (1, 1, 'wms:receipt_order_list', '收货单列表', 'wms_receipt_order_list', '查询 WMS 收货单真实数据', 0, 1, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (2, 1, 'wms:merchant_list', '商户列表', 'wms_merchant_list', '查询 WMS 商户真实数据', 0, 2, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (3, 1, 'wms:inventory_list', '库存列表', 'wms_inventory_list', '查询 WMS 库存真实数据', 0, 3, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (4, 1, 'wms:shipment_order_list', '发货单列表', 'wms_shipment_order_list', '查询 WMS 发货单真实数据', 0, 4, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (5, 2, 'qms:inspection_order_list', '质检单列表', 'qms_inspection_order_list', '查询 QMS 质检单真实数据', 0, 1, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (6, 6, 'ai_rag_eval:dataset', '评估数据集管理', NULL, 'RAG 评估数据集管理（纯提示词技能）', 0, 1, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (7, 6, 'ai_rag_eval:batch', '批量评估执行', NULL, 'RAG 批量评估执行（纯提示词技能）', 0, 2, 'admin', NOW(), 'admin', NOW(), 0, 0),
    (8, 6, 'ai_rag_eval:aggregate', '结果聚合对比', NULL, 'RAG 评估结果聚合对比（纯提示词技能）', 0, 3, 'admin', NOW(), 'admin', NOW(), 0, 0);

-- ============================================================
-- 技能目录页面，挂载 AI 大模型 2758 下
-- 按钮权限与 MultiAgentSkillController 注解保持一致
-- ============================================================
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (6300, '技能目录', '', 2, 8, 2758, 'multiagent-skill', 'ep:tools', 'ai/multiagent/skill', 'AiMultiAgentSkill', 0, b'1', b'1', b'1', 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (6301, '技能目录查询', 'aimultiagent:skill:query', 3, 1, 6300, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time, updater, update_time, deleted)
VALUES (6302, '技能目录管理', 'aimultiagent:skill:manage', 3, 2, 6300, '', '', '', 0, 'admin', NOW(), 'admin', NOW(), 0);
