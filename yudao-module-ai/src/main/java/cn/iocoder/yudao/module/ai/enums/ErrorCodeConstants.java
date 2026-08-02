package cn.iocoder.yudao.module.ai.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * AI 错误码枚举类
 * <p>
 * ai 系统，使用 1-040-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== API 密钥 1-040-000-000 ==========
    ErrorCode API_KEY_NOT_EXISTS = new ErrorCode(1_040_000_000, "API 密钥不存在");
    ErrorCode API_KEY_DISABLE = new ErrorCode(1_040_000_001, "API 密钥已禁用！");
    ErrorCode API_CONFIG_PLACEHOLDER_NOT_RESOLVED = new ErrorCode(1_040_000_002, "AI 配置({})无法解析，请检查环境变量或配置项");

    // ========== API 模型 1-040-001-000 ==========
    ErrorCode MODEL_NOT_EXISTS = new ErrorCode(1_040_001_000, "模型不存在!");
    ErrorCode MODEL_DISABLE = new ErrorCode(1_040_001_001, "模型({})已禁用!");
    ErrorCode MODEL_DEFAULT_NOT_EXISTS = new ErrorCode(1_040_001_002, "操作失败，找不到默认模型");
    ErrorCode MODEL_USE_TYPE_ERROR = new ErrorCode(1_040_001_003, "操作失败，该模型的模型类型不正确");

    // ========== API 聊天角色 1-040-002-000 ==========
    ErrorCode CHAT_ROLE_NOT_EXISTS = new ErrorCode(1_040_002_000, "聊天角色不存在");
    ErrorCode CHAT_ROLE_DISABLE = new ErrorCode(1_040_000_003, "聊天角色({})已禁用!");

    // ========== API 聊天会话 1-040-003-000 ==========
    ErrorCode CHAT_CONVERSATION_NOT_EXISTS = new ErrorCode(1_040_003_000, "对话不存在!");
    ErrorCode CHAT_CONVERSATION_MODEL_ERROR = new ErrorCode(1_040_003_001, "操作失败，该聊天模型的配置不完整");

    // ========== API 聊天消息 1-040-004-000 ==========
    ErrorCode CHAT_MESSAGE_NOT_EXIST = new ErrorCode(1_040_004_000, "消息不存在!");
    ErrorCode CHAT_STREAM_ERROR = new ErrorCode(1_040_004_001, "对话生成异常!");

    // ========== API 绘画 1-040-005-000 ==========
    ErrorCode IMAGE_NOT_EXISTS = new ErrorCode(1_040_005_000, "图片不存在!");
    ErrorCode IMAGE_MIDJOURNEY_SUBMIT_FAIL = new ErrorCode(1_040_005_001, "Midjourney 提交失败!原因：{}");
    ErrorCode IMAGE_CUSTOM_ID_NOT_EXISTS = new ErrorCode(1_040_005_002, "Midjourney 按钮 customId 不存在! {}");

    // ========== API 音乐 1-040-006-000 ==========
    ErrorCode MUSIC_NOT_EXISTS = new ErrorCode(1_040_006_000, "音乐不存在!");

    // ========== API 写作 1-040-007-000 ==========
    ErrorCode WRITE_NOT_EXISTS = new ErrorCode(1_040_007_000, "作文不存在!");
    ErrorCode WRITE_STREAM_ERROR = new ErrorCode(1_040_07_001, "写作生成异常!");

    // ========== API 思维导图 1-040-008-000 ==========
    ErrorCode MIND_MAP_NOT_EXISTS = new ErrorCode(1_040_008_000, "思维导图不存在!");

    // ========== API 知识库 1-040-009-000 ==========
    ErrorCode KNOWLEDGE_NOT_EXISTS = new ErrorCode(1_040_009_000, "知识库不存在!");

    ErrorCode KNOWLEDGE_DOCUMENT_NOT_EXISTS = new ErrorCode(1_040_009_101, "文档不存在!");
    ErrorCode KNOWLEDGE_DOCUMENT_FILE_EMPTY = new ErrorCode(1_040_009_102, "文档内容为空!");
    ErrorCode KNOWLEDGE_DOCUMENT_FILE_DOWNLOAD_FAIL = new ErrorCode(1_040_000_004, "文件下载失败!");
    ErrorCode KNOWLEDGE_DOCUMENT_FILE_READ_FAIL = new ErrorCode(1_040_000_005, "文档加载失败!");

    ErrorCode KNOWLEDGE_SEGMENT_NOT_EXISTS = new ErrorCode(1_040_009_202, "段落不存在!");
    ErrorCode KNOWLEDGE_SEGMENT_CONTENT_TOO_LONG = new ErrorCode(1_040_009_203, "内容 Token 数为 {}，超过最大限制 {}");

    // ========== AI 工具 1-040-010-000 ==========
    ErrorCode TOOL_NOT_EXISTS = new ErrorCode(1_040_010_000, "工具不存在");
    ErrorCode TOOL_NAME_NOT_EXISTS = new ErrorCode(1_040_010_001, "工具({})找不到 Bean");

    // ========== AI 工作流 1-040-011-000 ==========
    ErrorCode WORKFLOW_NOT_EXISTS = new ErrorCode(1_040_011_000, "工作流不存在");
    ErrorCode WORKFLOW_CODE_EXISTS = new ErrorCode(1_040_011_001, "工作流标识已存在");

    // ========== AI Prompt 模板 1-040-012-000 ==========
    ErrorCode AI_PROMPT_TEMPLATE_NOT_EXISTS = new ErrorCode(1_040_012_000, "Prompt 模板不存在");
    ErrorCode AI_PROMPT_TEMPLATE_CODE_EXISTS = new ErrorCode(1_040_012_001, "Prompt 模板编码已存在");

    // ========== AI 调用保护 1-040-013-000 ==========
    ErrorCode AI_CALL_TIMEOUT = new ErrorCode(1_040_013_000, "AI 调用超时，请稍后再试");
    ErrorCode AI_CALL_CIRCUIT_OPEN = new ErrorCode(1_040_013_001, "AI 服务暂时不可用（熔断中），请稍后再试");

    // ========== AI SSRF 防护 1-040-014-000 ==========
    ErrorCode AI_SSRF_URL_INVALID = new ErrorCode(1_040_014_000, "URL 不合法或协议非 http/https：{}");
    ErrorCode AI_SSRF_RESOLVE_FAIL = new ErrorCode(1_040_014_001, "域名解析失败：{}");
    ErrorCode AI_SSRF_BLOCKED = new ErrorCode(1_040_014_002, "目标地址命中内网/黑名单，禁止访问：{}");

    // ========== AI NL2SQL 报表分析 1-040-015-000 ==========
    ErrorCode NL2SQL_LLM_UNAVAILABLE = new ErrorCode(1_040_015_000, "NL2SQL 无可用 LLM（ChatClient 与 AiModelService 均未注入）");
    ErrorCode NL2SQL_SAFETY_CHECK_FAILED = new ErrorCode(1_040_015_001, "SQL 安全校验失败：仅允许只读 SELECT 查询");
    ErrorCode NL2SQL_EXECUTE_FAILED = new ErrorCode(1_040_015_002, "SQL 执行失败或数据源未就绪");

    // ========== AI 预测性维护 1-040-016-000 ==========
    ErrorCode PREDICTIVE_DEVICE_NOT_EXISTS = new ErrorCode(1_040_016_000, "设备不存在或数据网关不可用");
    ErrorCode PREDICTIVE_DATA_UNAVAILABLE = new ErrorCode(1_040_016_001, "设备维护数据不可用（缺少历史 KPI 或维修记录）");
    ErrorCode PREDICTIVE_LLM_UNAVAILABLE = new ErrorCode(1_040_016_002, "预测性维护 LLM 不可用（ChatClient 与 AiModelService 均未注入）");

}
