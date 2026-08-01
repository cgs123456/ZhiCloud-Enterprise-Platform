-- ======================== AI Prompt 模板（ai_prompt_template）建表脚本 ========================
-- 作者：yudao
-- 说明：用于管理可复用的 Prompt 模板，支持变量占位符 {variableName} 渲染

-- ----------------------------
-- AI Prompt 模板表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `ai_prompt_template` (
    `id` BIGINT NOT NULL COMMENT '编号',
    `name` VARCHAR(128) NOT NULL COMMENT '模板名称',
    `code` VARCHAR(64) NOT NULL COMMENT '模板编码',
    `category` VARCHAR(32) NOT NULL COMMENT '分类（SYSTEM/CHAT/RAG/AGENT/TRANSLATION/SUMMARIZATION/CODE）',
    `content` TEXT NOT NULL COMMENT '模板内容（含变量占位符 {variableName}）',
    `variables` VARCHAR(512) COMMENT '变量列表（JSON 格式）',
    `description` VARCHAR(512) COMMENT '描述',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态（0 开启 1 停用）',
    `remark` VARCHAR(512) COMMENT '备注',
    `creator` VARCHAR(64) COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT 0 COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`)
) COMMENT 'AI Prompt 模板表';

-- ----------------------------
-- 唯一索引：code 在租户内唯一
-- ----------------------------
CREATE UNIQUE INDEX `uk_airag_prompt_code` ON `ai_prompt_template` (`code`, `tenant_id`);
