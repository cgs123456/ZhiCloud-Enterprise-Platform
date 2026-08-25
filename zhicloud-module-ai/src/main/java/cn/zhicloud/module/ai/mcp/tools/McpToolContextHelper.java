package cn.zhicloud.module.ai.mcp.tools;

import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;

import java.util.function.Supplier;

/**
 * MCP Tool 多租户上下文助手
 *
 * SubTask 7.4a 方案 A：MCP Tool 参数显式传递 tenantId
 *
 * 由于 MCP 协议本身不感知 HTTP 请求上下文，zhicloud 的多租户拦截器无法直接拿到租户编号。
 * 因此约定：所有 MCP Tool 方法的第一个参数固定为 {@code Long tenantId}，
 * 通过本工具类显式设置到 {@link TenantContextHolder}，并在 finally 块中清理，
 * 保证 Service 层 SQL 自动拼接 tenant_id 条件。
 *
 * 设计要点：
 *  1. 保存旧租户编号，执行完后恢复（避免污染调用线程上下文）
 *  2. 旧租户编号为空时调用 clear() 清理，避免泄漏
 *  3. 提供 Supplier / Runnable 两种调用形式，方便有/无返回值场景
 *
 * @author 智云
 */
public final class McpToolContextHelper {

    private McpToolContextHelper() {
    }

    /**
     * 在指定租户上下文中执行（有返回值）
     *
     * @param tenantId 租户编号
     * @param supplier 业务逻辑
     * @param <T>      返回值类型
     * @return 业务返回值
     */
    public static <T> T execute(Long tenantId, Supplier<T> supplier) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(tenantId);
            return supplier.get();
        } finally {
            restoreTenantId(oldTenantId);
        }
    }

    /**
     * 在指定租户上下文中执行（无返回值）
     *
     * @param tenantId 租户编号
     * @param runnable 业务逻辑
     */
    public static void execute(Long tenantId, Runnable runnable) {
        Long oldTenantId = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(tenantId);
            runnable.run();
        } finally {
            restoreTenantId(oldTenantId);
        }
    }

    /**
     * 恢复旧租户上下文
     *
     * @param oldTenantId 旧租户编号（可为空）
     */
    private static void restoreTenantId(Long oldTenantId) {
        if (oldTenantId != null) {
            TenantContextHolder.setTenantId(oldTenantId);
        } else {
            TenantContextHolder.clear();
        }
    }

}
