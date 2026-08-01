package cn.iocoder.yudao.module.ai.mcp.tools;

import java.util.function.Supplier;

/**
 * 租户感知的 MCP Tool 基类
 *
 * SubTask 7.4a：所有 MCP Tool 通过继承本基类获得在指定租户上下文执行业务逻辑的能力。
 *
 * 设计说明：
 *  - 业务模块（wms/mes/erp）的 Service 强依赖 TenantContextHolder 才能正确拼 SQL；
 *  - MCP 协议是 JSON-RPC over SSE/HTTP，不经过 yudao 的 TenantContextWebFilter，
 *    因此 Tool 方法必须显式接收 tenantId 参数，并在调用 Service 前设置上下文。
 *
 * 用法：
 * <pre>
 * &#64;Component
 * &#64;ConditionalOnBean(WmsInventoryService.class)
 * public class WmsMcpTools extends TenantAwareMcpTool {
 *     &#64;Tool(description = "...")
 *     public WmsInventoryRespVO getInventory(&#64;ToolParam(description = "租户编号") Long tenantId, ...) {
 *         return executeInTenant(tenantId, () -&gt; {
 *             // 调用 Service
 *         });
 *     }
 * }
 * </pre>
 *
 * @author 芋道源码
 */
public abstract class TenantAwareMcpTool {

    /**
     * 在指定租户上下文中执行业务逻辑（有返回值）
     *
     * @param tenantId 租户编号
     * @param supplier 业务逻辑
     * @param <T>      返回值类型
     * @return 业务返回值
     */
    protected <T> T executeInTenant(Long tenantId, Supplier<T> supplier) {
        return McpToolContextHelper.execute(tenantId, supplier);
    }

    /**
     * 在指定租户上下文中执行业务逻辑（无返回值）
     *
     * @param tenantId 租户编号
     * @param runnable 业务逻辑
     */
    protected void runInTenant(Long tenantId, Runnable runnable) {
        McpToolContextHelper.execute(tenantId, runnable);
    }

}
