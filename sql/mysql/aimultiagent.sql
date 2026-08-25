-- ======================== 多 Agent 编排模块（aimultiagent）建表脚本 ========================
-- 作者：zhicloud
-- 说明：包含拓扑配置表和执行日志表

-- ----------------------------
-- 多 Agent 拓扑配置表
-- ----------------------------
CREATE TABLE IF NOT EXISTS aimultiagent_topology (
    id BIGINT PRIMARY KEY COMMENT '主键',
    name VARCHAR(255) NOT NULL COMMENT '拓扑名称',
    description TEXT COMMENT '描述',
    supervisor_system_prompt TEXT NOT NULL COMMENT 'Supervisor 系统提示词',
    worker_config TEXT NOT NULL COMMENT 'Worker 配置 JSON（workerName, tools, systemPrompt）',
    max_depth INT DEFAULT 5 COMMENT '最大调用深度（防死循环）',
    max_token_budget INT DEFAULT 10000 COMMENT 'Token 预算上限',
    status TINYINT DEFAULT 0 COMMENT '状态（0启用 1停用）',
    version VARCHAR(32) DEFAULT 'v1' COMMENT '配置 schema 版本（如 v1）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT '多 Agent 拓扑配置表';

-- ----------------------------
-- 执行日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS aimultiagent_execution_log (
    id BIGINT PRIMARY KEY COMMENT '主键',
    topology_id BIGINT NOT NULL COMMENT '拓扑 ID',
    user_input TEXT NOT NULL COMMENT '用户输入',
    supervisor_plan TEXT COMMENT 'Supervisor 任务拆解 JSON',
    worker_results TEXT COMMENT 'Worker 执行结果 JSON',
    final_answer TEXT COMMENT '最终汇总答案',
    total_tokens INT DEFAULT 0 COMMENT '总 Token 消耗',
    actual_depth INT DEFAULT 0 COMMENT '实际调用深度',
    status TINYINT DEFAULT 0 COMMENT '执行状态（0进行中 1成功 2失败 3熔断）',
    error_msg TEXT COMMENT '错误信息',
    duration_ms BIGINT DEFAULT 0 COMMENT '执行耗时（毫秒）',
    trace_id VARCHAR(64) COMMENT '全链路追踪 ID（跨 Worker / Supervisor 排查）',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT DEFAULT 0 COMMENT '租户 ID'
) COMMENT '多 Agent 执行日志表';
