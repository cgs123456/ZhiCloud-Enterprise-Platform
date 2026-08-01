package cn.iocoder.yudao.module.ai.mcp.tools;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.lang.reflect.Method;

import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.FORBIDDEN;
import static cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants.UNAUTHORIZED;

/**
 * MCP Tool 权限校验 AOP 切面
 *
 * SubTask 7.4b：拦截 {@link McpToolRequiresPermission} 标记的 @Tool 方法，
 * 在执行前校验当前登录用户是否具备所需权限码。
 *
 * 校验链路：
 *  1. 通过 {@link SecurityFrameworkUtils#getLoginUserId()} 获取当前调用者（来自 SecurityContextHolder，
 *     MCP 请求经过 yudao-security 的 TokenAuthenticationFilter 后会注入登录用户）
 *  2. 未登录 → 抛 UNAUTHORIZED（401）
 *  3. 已登录但缺少权限 → 抛 FORBIDDEN（403）
 *
 * 注意：SecurityFrameworkService 通过 {@link Lazy} 注入避免循环依赖，
 * 实际使用时由 Spring 容器代理。
 *
 * @author 芋道源码
 */
@Aspect
@Configuration
@Slf4j
public class McpToolSecurityAspect {

    @Autowired(required = false)
    @Lazy
    private SecurityFrameworkService securityFrameworkService;

    /**
     * 拦截所有带 {@link McpToolRequiresPermission} 注解的方法
     */
    @Around("@annotation(cn.iocoder.yudao.module.ai.mcp.tools.McpToolRequiresPermission)")
    public Object aroundMcpTool(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取当前调用者
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        if (userId == null) {
            log.warn("[MCP Tool 权限校验] 未登录调用 MCP 工具：{}", joinPoint.getSignature().toShortString());
            throw new ServiceException(UNAUTHORIZED);
        }

        // 2. 解析所需权限码
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        McpToolRequiresPermission annotation = method.getAnnotation(McpToolRequiresPermission.class);
        String permission = annotation.value();

        // 3. 校验权限（P0-3 修复：fail-closed 策略，SecurityFrameworkService 缺失时拒绝而非放行）
        if (securityFrameworkService == null) {
            log.error("[MCP Tool 权限校验] SecurityFrameworkService 未注入，拒绝执行（fail-closed）：{}",
                    joinPoint.getSignature().toShortString());
            throw new ServiceException(FORBIDDEN);
        }
        if (!securityFrameworkService.hasPermission(permission)) {
            log.warn("[MCP Tool 权限校验] 用户 userId={} 缺少权限 {}，工具：{}",
                    userId, permission, joinPoint.getSignature().toShortString());
            throw new ServiceException(FORBIDDEN);
        }

        // 4. 执行业务方法
        return joinPoint.proceed();
    }

}
