/**
 * MCP Tool 暴露与安全增强包
 *
 * <h3>架构决策</h3>
 * <p>不在业务模块（wms/mes/erp）的 Service 上直接加 {@code @Tool} 注解，避免业务模块反向依赖 Spring AI。
 * 取而代之，在 yudao-module-ai 模块中创建独立的 Tool 包装类：
 * <ol>
 *   <li>yudao-module-ai 的 pom.xml 添加对 wms/mes/erp 的 {@code <optional>true</optional>} 依赖；</li>
 *   <li>每个 Tool 类用 {@code @ConditionalOnBean} 守护，仅当对应 Service Bean 存在时才加载；</li>
 *   <li>Tool 方法通过显式 {@code tenantId} 参数 + {@link cn.iocoder.yudao.module.ai.mcp.tools.McpToolContextHelper}
 *       设置多租户上下文。</li>
 * </ol>
 *
 * <h3>子任务拆分</h3>
 * <ul>
 *   <li>SubTask 7.4a：多租户上下文传递 —— {@link cn.iocoder.yudao.module.ai.mcp.tools.McpToolContextHelper}
 *       + {@link cn.iocoder.yudao.module.ai.mcp.tools.TenantAwareMcpTool} 基类</li>
 *   <li>SubTask 7.4b：权限校验 —— {@link cn.iocoder.yudao.module.ai.mcp.tools.McpToolRequiresPermission}
 *       + {@link cn.iocoder.yudao.module.ai.mcp.tools.McpToolSecurityAspect} AOP 拦截</li>
 *   <li>SubTask 7.4c：审计日志 —— {@link cn.iocoder.yudao.module.ai.mcp.tools.McpToolAuditLogAspect}
 *       拦截所有 {@link org.springframework.ai.tool.annotation.Tool} 方法，记录调用时间、调用者、参数、返回值摘要、耗时</li>
 * </ul>
 *
 * <h3>Tool 类清单</h3>
 * <ul>
 *   <li>{@link cn.iocoder.yudao.module.ai.mcp.tools.WmsMcpTools} - WMS 库存查询（SKU 数量、仓库数量、分页）</li>
 *   <li>{@link cn.iocoder.yudao.module.ai.mcp.tools.MesMcpTools} - MES 工单查询 + 物料库存查询</li>
 *   <li>{@link cn.iocoder.yudao.module.ai.mcp.tools.ErpMcpTools} - ERP 库存 + 采购订单 + 销售订单查询</li>
 * </ul>
 *
 * <h3>使用约束</h3>
 * <ul>
 *   <li>{@code @Tool} 方法返回简化 VO（不暴露 DO 的 creator/updater/tenantId/deleted 等敏感字段）；</li>
 *   <li>每个 {@code @Tool} 方法第一个参数固定为 {@code Long tenantId}（带 {@code @ToolParam}）；</li>
 *   <li>{@code @Tool} 方法描述需为中英文，方便 LLM 调用。</li>
 * </ul>
 */
package cn.iocoder.yudao.module.ai.mcp.tools;
