package cn.iocoder.yudao.module.ai.mcp.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP Tool 权限校验注解
 *
 * SubTask 7.4b：标记 MCP Tool 方法所需的权限码，由 {@link McpToolSecurityAspect} 拦截校验。
 *
 * 校验流程：
 *  1. 从 {@link org.springframework.security.core.context.SecurityContextHolder} 提取当前登录用户
 *  2. 调用 {@link cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService#hasPermission(String)}
 *     判断是否拥有指定权限
 *  3. 未登录或权限不足，抛出 {@link cn.iocoder.yudao.framework.common.exception.ServiceException}，
 *     返回 403 错误码给 MCP 客户端
 *
 * 使用方式：
 * <pre>
 * &#64;Tool(description = "...")
 * &#64;McpToolRequiresPermission("wms:inventory:query")
 * public WmsInventoryRespVO getInventory(...) { ... }
 * </pre>
 *
 * 注意：与 yudao 后端 RBAC 权限码保持一致，方便后台统一管理。
 *
 * @author 芋道源码
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpToolRequiresPermission {

    /**
     * 权限码
     *
     * 例如：wms:inventory:query、erp:stock:query
     */
    String value();

}
