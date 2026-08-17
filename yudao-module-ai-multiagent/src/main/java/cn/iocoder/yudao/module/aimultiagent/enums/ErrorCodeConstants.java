package cn.iocoder.yudao.module.aimultiagent.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * AI 多 Agent 编排错误码枚举类
 *
 * ai-multiagent 系统，使用 1-042-000-000 段（与 ai 模块 1-040、ai-rag 1-041 区分）
 *
 * @author yudao
 */
public interface ErrorCodeConstants {

    // ========== 拓扑配置 1-042-000-000 ==========
    ErrorCode TOPOLOGY_NOT_EXISTS = new ErrorCode(1_042_000_000, "多 Agent 拓扑不存在");
    ErrorCode TOPOLOGY_DISABLE = new ErrorCode(1_042_000_001, "多 Agent 拓扑({})已禁用");
    ErrorCode TOPOLOGY_WORKER_CONFIG_INVALID = new ErrorCode(1_042_000_002, "Worker 配置 JSON 格式不合法");
    ErrorCode TOPOLOGY_WORKER_NOT_REGISTERED = new ErrorCode(1_042_000_003, "Worker({})未在注册中心找到，请检查拓扑配置");

    // ========== 执行日志 1-042-001-000 ==========
    ErrorCode EXECUTION_LOG_NOT_EXISTS = new ErrorCode(1_042_001_000, "执行日志不存在");

    // ========== 编排执行 1-042-002-000 ==========
    ErrorCode EXECUTE_TOPOLOGY_NOT_EXISTS = new ErrorCode(1_042_002_000, "编排执行失败：拓扑配置不存在");
    ErrorCode EXECUTE_TOPOLOGY_DISABLED = new ErrorCode(1_042_002_001, "编排执行失败：拓扑配置已禁用");
    ErrorCode EXECUTE_LLM_NOT_READY = new ErrorCode(1_042_002_002, "编排执行失败：LLM 模型不可用，请先配置 AI 模型 API Key");
    ErrorCode EXECUTE_WORKER_NOT_FOUND = new ErrorCode(1_042_002_003, "编排执行失败：找不到 Worker({})");
    ErrorCode EXECUTE_DEPTH_EXCEEDED = new ErrorCode(1_042_002_004, "编排执行熔断：任务数({})超过最大调用深度({})");
    ErrorCode EXECUTE_TOKEN_BUDGET_EXCEEDED = new ErrorCode(1_042_002_005, "编排执行熔断：Token 消耗({})超过预算上限({})");
    ErrorCode EXECUTE_SUPERVISOR_PLAN_FAIL = new ErrorCode(1_042_002_006, "Supervisor 任务拆解失败");
    ErrorCode EXECUTE_SUPERVISOR_SUMMARIZE_FAIL = new ErrorCode(1_042_002_007, "Supervisor 结果汇总失败");
    ErrorCode EXECUTE_WORKER_FAIL = new ErrorCode(1_042_002_008, "Worker({})执行失败：{}");

}
