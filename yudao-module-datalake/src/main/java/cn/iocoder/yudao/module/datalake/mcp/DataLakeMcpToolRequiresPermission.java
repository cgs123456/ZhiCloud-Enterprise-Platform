package cn.iocoder.yudao.module.datalake.mcp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据湖仓 MCP Tool 权限校验注解
 *
 * <p>标记 MCP Tool 方法所需的权限码。
 *
 * <p>说明：yudao-module-ai 中已存在 {@code McpToolRequiresPermission} 注解及对应的
 * {@code McpToolSecurityAspect} 切面。由于本模块不依赖 yudao-module-ai（保持模块独立性），
 * 此处定义本模块专用的权限注解。当 yudao-module-ai 同时加载时，其切面仅拦截自身包路径下的注解，
 * 不会与本注解冲突。未来若将权限注解下沉到 yudao-common，可统一复用。
 *
 * <p>使用方式：
 * <pre>
 * &#64;Tool(description = "...")
 * &#64;DataLakeMcpToolRequiresPermission("datalake:query")
 * public List&lt;String&gt; listDataLakeTables(...) { ... }
 * </pre>
 *
 * @author yudao
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataLakeMcpToolRequiresPermission {

    /**
     * 权限码
     *
     * 例如：datalake:query、datalake:archive
     */
    String value();

}
