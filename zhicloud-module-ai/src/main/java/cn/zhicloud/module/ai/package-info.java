/**
 * AI 模块：人工智能（ChatClient/MCP 工具/工作流/RAG/ChatMemory/AI 安全审计）
 *
 * <p>Spring Modulith 模块声明（A3）。依赖：system/infra/erp/mes/wms/crm/qms。
 *
 * <p>本模块作为 AI Native 的核心，对外提供：
 * <ul>
 *   <li>Spring AI ChatClient bean</li>
 *   <li>MCP Server / Client 能力</li>
 *   <li>业务模块 @Tool 暴露（WmsMcpTools/MesMcpTools/ErpMcpTools）</li>
 *   <li>ChatMemory 会话记忆</li>
 *   <li>TinyFlow AI 工作流</li>
 * </ul>
 *
 * @author zhicloud
 */
@org.springframework.modulith.ApplicationModule(displayName = "AI 人工智能模块")
package cn.zhicloud.module.ai;
