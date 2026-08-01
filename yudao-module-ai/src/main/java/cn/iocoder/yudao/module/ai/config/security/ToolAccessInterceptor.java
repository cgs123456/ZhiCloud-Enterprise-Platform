package cn.iocoder.yudao.module.ai.config.security;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.ai.config.AiSecurityConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * 工具调用白/黑名单 + 二次确认切面（SubTask 14.1）
 *
 * <p>基于 Spring AOP {@link Aspect} 拦截所有 {@link Tool} 注解方法，
 * 实现三段式校验：黑名单模式匹配 → 白名单放行 → 二次确认流程。
 *
 * <h3>校验流程</h3>
 * <ol>
 *   <li>解析工具名（优先 {@link Tool#name()}，否则用方法名）。</li>
 *   <li>黑名单 Ant 模式匹配，命中即抛 {@link GlobalErrorCodeConstants#FORBIDDEN}。</li>
 *   <li>白名单启用时，未登记工具拒绝调用，抛 FORBIDDEN。</li>
 *   <li>二次确认模式（如 {@code update*Inventory*}、{@code *Order*}）命中：
 *     <ul>
 *       <li>请求未携带 {@code X-Confirm-Token}：生成 UUID 存入 Redis（5 分钟 TTL），
 *           写回 {@code {"code":409,"msg":"需要二次确认","data":{"confirmToken":"xxx"}}} 响应。</li>
 *       <li>请求携带 {@code X-Confirm-Token}：与 Redis 校验，校验通过删除 token 并放行；否则拒绝。</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * <h3>引用参考</h3>
 * <p>实现风格参考 {@link cn.iocoder.yudao.module.ai.mcp.tools.McpToolSecurityAspect}，
 * 注入 {@link AiSecurityConfiguration} 读取规则，RedisTemplate/SecurityFrameworkService 通过
 * {@code @Autowired(required=false)} + {@code @Lazy} 注入保证容器启动安全。
 *
 * @author 芋道源码
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "yudao.ai.security.tool-access", name = "whitelist-enabled", havingValue = "true")
@Slf4j
public class ToolAccessInterceptor {

    /**
     * 二次确认请求头名
     */
    private static final String HEADER_CONFIRM_TOKEN = "X-Confirm-Token";

    /**
     * Redis Key 前缀：用于二次确认 token 存储
     */
    private static final String KEY_CONFIRM_TOKEN = "ai:tool-access:confirm:";

    /**
     * Ant 模式匹配器
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    /**
     * AI 安全配置
     */
    @Autowired
    private AiSecurityConfiguration aiSecurityConfiguration;

    /**
     * Redis 模板（可选；未配置时跳过二次确认 token 存储，直接拒绝）
     */
    @Autowired(required = false)
    @Lazy
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 拦截所有 {@link Tool} 注解方法
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object aroundTool(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 解析工具名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Tool toolAnnotation = method.getAnnotation(Tool.class);
        String toolName = (toolAnnotation != null && !toolAnnotation.name().isEmpty())
                ? toolAnnotation.name() : method.getName();

        AiSecurityConfiguration.ToolAccess config = aiSecurityConfiguration.getToolAccess();

        // 2. 黑名单校验（Ant 模式匹配命中即拒绝）
        if (matchesAny(toolName, config.getBlacklist())) {
            log.warn("[ToolAccess] 工具命中黑名单 toolName={} userId={} method={}",
                    toolName, SecurityFrameworkUtils.getLoginUserId(), joinPoint.getSignature().toShortString());
            throw new ServiceException(GlobalErrorCodeConstants.FORBIDDEN);
        }

        // 3. 白名单校验（启用时，未登记工具拒绝）
        if (config.isWhitelistEnabled()
                && !containsExact(toolName, config.getWhitelist())
                && !matchesAny(toolName, config.getWhitelist())) {
            log.warn("[ToolAccess] 工具未在白名单 toolName={} userId={}",
                    toolName, SecurityFrameworkUtils.getLoginUserId());
            throw new ServiceException(GlobalErrorCodeConstants.FORBIDDEN);
        }

        // 4. 二次确认模式校验
        if (matchesAny(toolName, config.getConfirmPatterns())) {
            handleConfirmFlow(toolName, config.getConfirmTokenTtlSeconds());
            // 校验通过（携带有效 X-Confirm-Token），继续执行
        }

        // 5. 放行
        return joinPoint.proceed();
    }

    /**
     * 二次确认流程
     *
     * @param toolName      工具名
     * @param ttlSeconds token 有效期（秒）
     */
    private void handleConfirmFlow(String toolName, long ttlSeconds) {
        // 4.1 读取当前请求的 X-Confirm-Token 头
        HttpServletRequest request = currentRequest();
        String confirmToken = (request != null) ? request.getHeader(HEADER_CONFIRM_TOKEN) : null;

        // 4.2 已携带 token：与 Redis 校验
        if (confirmToken != null && !confirmToken.isEmpty()) {
            if (validateAndConsumeToken(toolName, confirmToken)) {
                return; // 校验通过，放行
            }
            log.warn("[ToolAccess] 二次确认 token 无效或已使用 toolName={} token={}", toolName, confirmToken);
            throw new ServiceException(GlobalErrorCodeConstants.FORBIDDEN);
        }

        // 4.3 未携带 token：生成新 token 并写回响应
        String newToken = UUID.randomUUID().toString().replace("-", "");
        storeConfirmToken(toolName, newToken, ttlSeconds);
        log.info("[ToolAccess] 触发二次确认 toolName={} token={} userId={}",
                toolName, newToken, SecurityFrameworkUtils.getLoginUserId());

        if (!writeConfirmResponse(newToken)) {
            // HTTP 上下文不可用时（如内部 RPC 调用），降级抛 ServiceException
            throw new ServiceException(GlobalErrorCodeConstants.FORBIDDEN.getCode(),
                    "需要二次确认，confirmToken=" + newToken);
        }
        // 已写入响应，抛出静默异常中止调用链（防止 joinPoint.proceed 二次执行）
        throw new ConfirmAbortException();
    }

    /**
     * 存储 confirmToken 到 Redis
     */
    private void storeConfirmToken(String toolName, String token, long ttlSeconds) {
        if (redisTemplate == null) {
            log.warn("[ToolAccess] RedisTemplate 未配置，无法持久化 confirmToken toolName={}", toolName);
            return;
        }
        try {
            redisTemplate.opsForValue().set(KEY_CONFIRM_TOKEN + token, toolName,
                    java.time.Duration.ofSeconds(ttlSeconds));
        } catch (Exception ex) {
            log.error("[ToolAccess] 写入 Redis 失败 toolName={} token={}", toolName, token, ex);
        }
    }

    /**
     * 校验并消费 confirmToken（单次有效）
     *
     * @return true 校验通过；false token 不存在或已使用
     */
    private boolean validateAndConsumeToken(String toolName, String token) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            String key = KEY_CONFIRM_TOKEN + token;
            Object stored = redisTemplate.opsForValue().get(key);
            if (stored == null) {
                return false;
            }
            if (!toolName.equals(stored.toString())) {
                log.warn("[ToolAccess] confirmToken 工具不匹配 expected={} actual={}", toolName, stored);
                return false;
            }
            redisTemplate.delete(key); // 单次有效
            return true;
        } catch (Exception ex) {
            log.error("[ToolAccess] 校验 confirmToken 失败 toolName={} token={}", toolName, token, ex);
            return false;
        }
    }

    /**
     * 写入二次确认响应：{@code {"code":409,"msg":"需要二次确认","data":{"confirmToken":"xxx"}}}
     *
     * @return true 写入成功；false HTTP 上下文不可用
     */
    private boolean writeConfirmResponse(String confirmToken) {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return false;
        }
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return false;
        }
        HttpServletResponse response = servletAttrs.getResponse();
        if (response == null || response.isCommitted()) {
            return false;
        }
        String json = "{\"code\":409,\"msg\":\"需要二次确认\",\"data\":{\"confirmToken\":\""
                + confirmToken + "\"}}";
        try {
            response.setStatus(409);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(json);
            response.getWriter().flush();
            return true;
        } catch (IOException ex) {
            log.error("[ToolAccess] 写入二次确认响应失败 token={}", confirmToken, ex);
            return false;
        }
    }

    /**
     * 获取当前 HTTP 请求（可能为 null，例如异步调用或非 HTTP 上下文）
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    /**
     * Ant 模式匹配（任意一个模式命中即返回 true）
     */
    private boolean matchesAny(String name, java.util.List<String> patterns) {
        if (patterns == null || patterns.isEmpty() || name == null) {
            return false;
        }
        for (String pattern : patterns) {
            if (pattern == null || pattern.isEmpty()) {
                continue;
            }
            if (PATH_MATCHER.match(pattern, name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 精确匹配（用于白名单的精确名称匹配）
     */
    private boolean containsExact(String name, java.util.List<String> whitelist) {
        if (whitelist == null || whitelist.isEmpty()) {
            return false;
        }
        for (String item : whitelist) {
            if (name.equals(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 二次确认中止异常
     *
     * <p>已通过 {@link #writeConfirmResponse} 写入 HTTP 响应，需中止调用链避免 joinPoint.proceed 重复执行。
     * 由于 {@link ServiceException} 声明为 {@code final} 无法继承，本异常继承 {@link RuntimeException}；
     * 响应已提交时全局异常处理器不会再写响应，仅记录日志，故不会影响已返回的二次确认 JSON。
     * {@link #fillInStackTrace()} 返回 this 避免无谓的栈追踪开销。
     */
    private static class ConfirmAbortException extends RuntimeException {

        ConfirmAbortException() {
            super("Tool access requires confirmation (response already committed)");
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

}
