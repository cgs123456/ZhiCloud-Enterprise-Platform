package cn.zhicloud.module.ai.config.security;

import cn.zhicloud.framework.security.core.service.SecurityFrameworkService;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.ai.config.AiSecurityConfiguration;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 用量限流过滤器（SubTask 14.3）
 *
 * <p>基于 Servlet {@link Filter} 拦截 {@code /admin-api/ai/**} 路径，使用 Redis Lua 脚本
 * 实现按用户（每小时）/租户（每天）/IP（每分钟）三维度滑动窗口计数。
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>请求进入：先 GET 三个维度的当前计数，若任一超限返回 HTTP 429 + JSON 错误体。</li>
 *   <li>调用链执行：业务方法返回时通过响应头 {@code X-Token-Usage}（单位 tokens）回传本次用量。</li>
 *   <li>请求结束：用 Lua 脚本原子性地 INCRBY 三个计数器并设置 TTL。</li>
 * </ol>
 *
 * <h3>Redis Key 设计</h3>
 * <ul>
 *   <li>{@code ai:tok-limit:user:{userId}:{hour}} —— TTL 1 小时</li>
 *   <li>{@code ai:tok-limit:tenant:{tenantId}:{day}} —— TTL 1 天</li>
 *   <li>{@code ai:tok-limit:ip:{ip}:{minute}} —— TTL 1 分钟</li>
 * </ul>
 *
 * <h3>跳过策略</h3>
 * <p>{@code zhicloud.ai.security.token-limit.bypass-roles} 中配置的角色（默认 superadmin）跳过限流，
 * 通过 {@link SecurityFrameworkService#hasRole(String)} 判断。
 *
 * @author 智云
 */
@Component
@ConditionalOnProperty(prefix = "zhicloud.ai.security.token-limit", name = "enabled", havingValue = "true")
@Slf4j
public class TokenLimitFilter implements Filter {

    /**
     * 拦截路径前缀
     */
    private static final String PATH_PREFIX = "/admin-api/ai/";

    /**
     * 响应中 Token 用量 Header 名（单位 tokens）
     */
    private static final String HEADER_TOKEN_USAGE = "X-Token-Usage";

    /**
     * 超限错误 JSON 响应体
     */
    private static final String ERROR_BODY = "{\"code\":429,\"msg\":\"Token 用量超限\"}";

    /**
     * 用户维度 Key 前缀
     */
    private static final String KEY_USER = "ai:tok-limit:user:";

    /**
     * 租户维度 Key 前缀
     */
    private static final String KEY_TENANT = "ai:tok-limit:tenant:";

    /**
     * IP 维度 Key 前缀
     */
    private static final String KEY_IP = "ai:tok-limit:ip:";

    /**
     * 小时格式（用于用户维度 Key 后缀）
     */
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    /**
     * 天格式（用于租户维度 Key 后缀）
     */
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 分钟格式（用于 IP 维度 Key 后缀）
     */
    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /**
     * 进程内兜底限流的固定窗口长度（毫秒）：Redis 缺失或异常时使用
     */
    private static final long LOCAL_WINDOW_MILLIS = 60_000L;

    /**
     * 进程内兜底计数器的最大容量，超过时触发过期窗口清理，防止内存无限增长
     */
    private static final int MAX_LOCAL_WINDOWS = 100_000;

    /**
     * Lua 脚本：原子性 INCRBY + 首次设置 TTL
     *
     * <p>KEYS[1] = redis key
     * <p>ARGV[1] = 增量（tokens 数）
     * <p>ARGV[2] = TTL 秒数
     * <p>返回累加后的当前值
     */
    private static final DefaultRedisScript<Long> INCRBY_SCRIPT;

    static {
        INCRBY_SCRIPT = new DefaultRedisScript<>();
        INCRBY_SCRIPT.setScriptText(
                "local c = redis.call('INCRBY', KEYS[1], ARGV[1]) " +
                "if tonumber(c) == tonumber(ARGV[1]) then " +
                "  redis.call('EXPIRE', KEYS[1], ARGV[2]) " +
                "end " +
                "return c"
        );
        INCRBY_SCRIPT.setResultType(Long.class);
    }

    /**
     * 安全框架服务，用于跳过 superadmin 角色限流（可选，避免容器启动失败）
     */
    @Autowired(required = false)
    @Lazy
    private SecurityFrameworkService securityFrameworkService;

    /**
     * Redis 模板（zhicloud-spring-boot-starter-redis 自动配置，可选）
     */
    @Autowired(required = false)
    @Lazy
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 进程内固定窗口计数器（60s）：Redis 缺失或异常时的限流兜底。
     *
     * <p>Key 为 {@code user:{userId}} 或 {@code ip:{clientIp}}，
     * 兜底按请求数计数（Redis 正常时按 Token 数三维度计数）。
     */
    private final ConcurrentHashMap<String, LocalFixedWindow> localWindows = new ConcurrentHashMap<>();

    /**
     * AI 安全配置（直接注入 Properties Bean）
     */
    @Autowired
    private AiSecurityConfiguration aiSecurityConfiguration;

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("[TokenLimitFilter] 已启用 Token 限流：perUserHour={}, perTenantDay={}, perIpMinute={}",
                aiSecurityConfiguration.getTokenLimit().getPerUserHour(),
                aiSecurityConfiguration.getTokenLimit().getPerTenantDay(),
                aiSecurityConfiguration.getTokenLimit().getPerIpMinute());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
            chain.doFilter(request, response);
            return;
        }

        // 1. 仅拦截 /admin-api/ai/** 路径
        String requestUri = httpRequest.getRequestURI();
        if (requestUri == null || !requestUri.contains(PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        // 2. 获取上下文
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Long tenantId = TenantContextHolder.getTenantId();
        String clientIp = resolveClientIp(httpRequest);
        AiSecurityConfiguration.TokenLimit config = aiSecurityConfiguration.getTokenLimit();

        // 3. Redis 缺失时使用进程内固定窗口（60s）计数限流兜底，不再直接放行
        if (redisTemplate == null) {
            log.warn("[TokenLimitFilter] RedisTemplate 未配置，降级为进程内固定窗口限流 uri={}", requestUri);
            if (!localTryAcquire(userId, clientIp, config)) {
                writeTooManyRequests(httpResponse);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 4. superadmin 角色跳过限流
        if (isBypassRole(userId, config.getBypassRoles())) {
            chain.doFilter(request, response);
            return;
        }

        // 5. 预检查：三个维度当前是否已超限；Redis 异常时降级为进程内固定窗口兜底
        String hourKey = buildUserKey(userId, clientIp);
        String dayKey = buildTenantKey(tenantId, clientIp);
        String minuteKey = buildIpKey(clientIp);

        try {
            if (isExceeded(hourKey, config.getPerUserHour())
                    || isExceeded(dayKey, config.getPerTenantDay())
                    || isExceeded(minuteKey, config.getPerIpMinute())) {
                writeTooManyRequests(httpResponse);
                return;
            }
        } catch (Exception ex) {
            log.warn("[TokenLimitFilter] Redis 预检查异常，降级为进程内固定窗口限流 uri={} userId={}", requestUri, userId, ex);
            if (!localTryAcquire(userId, clientIp, config)) {
                writeTooManyRequests(httpResponse);
                return;
            }
            chain.doFilter(request, response);
            return;
        }

        // 6. 执行业务链
        chain.doFilter(request, response);

        // 7. 链路执行完成后，从响应头读取本次 Token 用量并累加
        long tokenUsage = parseTokenUsage(httpResponse);
        if (tokenUsage <= 0) {
            return;
        }

        try {
            // 用户维度（1 小时）
            if (hourKey != null) {
                redisTemplate.execute(INCRBY_SCRIPT,
                        java.util.Collections.singletonList(hourKey),
                        tokenUsage, 3600L);
            }
            // 租户维度（1 天）
            if (dayKey != null) {
                redisTemplate.execute(INCRBY_SCRIPT,
                        java.util.Collections.singletonList(dayKey),
                        tokenUsage, 86400L);
            }
            // IP 维度（1 分钟）
            if (minuteKey != null) {
                redisTemplate.execute(INCRBY_SCRIPT,
                        java.util.Collections.singletonList(minuteKey),
                        tokenUsage, 60L);
            }
        } catch (Exception ex) {
            log.error("[TokenLimitFilter] Redis 计数失败 uri={} userId={} tokenUsage={}",
                    requestUri, userId, tokenUsage, ex);
        }
    }

    /**
     * 判断当前用户是否在跳过限流的角色列表中
     *
     * @param userId      用户 ID
     * @param bypassRoles 跳过角色（逗号分隔）
     * @return 是否跳过
     */
    private boolean isBypassRole(Long userId, String bypassRoles) {
        if (userId == null || securityFrameworkService == null || bypassRoles == null || bypassRoles.isEmpty()) {
            return false;
        }
        for (String role : bypassRoles.split(",")) {
            String trimmed = role.trim();
            if (!trimmed.isEmpty() && securityFrameworkService.hasRole(trimmed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构造用户维度 Key：{@code ai:tok-limit:user:{userId}:{hour}}
     * <p>用户 ID 为空时回退到 IP 维度（避免匿名用户共享计数）
     */
    private String buildUserKey(Long userId, String clientIp) {
        String hour = LocalDateTime.now().format(HOUR_FMT);
        if (userId != null) {
            return KEY_USER + userId + ":" + hour;
        }
        // 匿名用户用 IP 维度计数（与 IP 维度共用同一 Key 后缀为 hour，避免重复计数干扰）
        return KEY_USER + "anon:" + (clientIp == null ? "unknown" : clientIp) + ":" + hour;
    }

    /**
     * 构造租户维度 Key：{@code ai:tok-limit:tenant:{tenantId}:{day}}
     * <p>租户 ID 为空时返回 null（不参与限流）
     */
    private String buildTenantKey(Long tenantId, String clientIp) {
        if (tenantId == null) {
            return null;
        }
        String day = LocalDate.now().format(DAY_FMT);
        return KEY_TENANT + tenantId + ":" + day;
    }

    /**
     * 构造 IP 维度 Key：{@code ai:tok-limit:ip:{ip}:{minute}}
     */
    private String buildIpKey(String clientIp) {
        if (clientIp == null || clientIp.isEmpty()) {
            return null;
        }
        String minute = LocalDateTime.now().format(MINUTE_FMT);
        return KEY_IP + clientIp + ":" + minute;
    }

    /**
     * 检查指定 Key 当前累计值是否超限
     *
     * <p>Redis 异常时向上抛出，由调用方降级为进程内固定窗口兜底限流。
     */
    private boolean isExceeded(String key, long limit) {
        if (key == null || limit <= 0) {
            return false;
        }
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return false;
        }
        long current = parseLong(value);
        return current >= limit;
    }

    /**
     * 进程内固定窗口（60s）计数限流兜底：Redis 缺失或异常时使用。
     *
     * @param userId    当前登录用户 ID（可能为 null）
     * @param clientIp  客户端 IP
     * @param config    Token 限流配置
     * @return true 表示放行；false 表示超限应拒绝
     */
    private boolean localTryAcquire(Long userId, String clientIp, AiSecurityConfiguration.TokenLimit config) {
        // 用户维度：小时配额折算到 60s 窗口；匿名 / IP 维度：直接复用每分钟配额
        long limit = userId != null
                ? Math.max(1L, config.getPerUserHour() / 60)
                : Math.max(1L, config.getPerIpMinute());
        String key = userId != null ? "user:" + userId : "ip:" + (clientIp == null ? "unknown" : clientIp);
        // 防止 Map 无限增长：超过容量阈值时清理已过期的窗口
        if (localWindows.size() > MAX_LOCAL_WINDOWS) {
            localWindows.entrySet().removeIf(entry -> entry.getValue().isExpired());
        }
        return localWindows.computeIfAbsent(key, k -> new LocalFixedWindow(LOCAL_WINDOW_MILLIS)).tryAcquire(limit);
    }

    /**
     * 进程内固定窗口计数器：按窗口起始时间分桶，窗口滚动时计数清零
     */
    private static final class LocalFixedWindow {

        /** 窗口长度（毫秒） */
        private final long windowMillis;
        /** 当前窗口起始时间戳 */
        private long windowStart;
        /** 窗口内累计计数 */
        private long count;

        private LocalFixedWindow(long windowMillis) {
            this.windowMillis = windowMillis;
        }

        /**
         * 计数 +1，返回是否仍在限额内
         */
        private synchronized boolean tryAcquire(long limit) {
            long start = System.currentTimeMillis() - System.currentTimeMillis() % windowMillis;
            if (start != windowStart) {
                windowStart = start;
                count = 0;
            }
            count++;
            return count <= limit;
        }

        /**
         * 当前窗口是否已过期（供清理使用）
         */
        private synchronized boolean isExpired() {
            long start = System.currentTimeMillis() - System.currentTimeMillis() % windowMillis;
            return start != windowStart;
        }
    }

    /**
     * 从响应头 {@code X-Token-Usage} 解析本次 Token 用量
     */
    private long parseTokenUsage(HttpServletResponse response) {
        String header = response.getHeader(HEADER_TOKEN_USAGE);
        if (header == null || header.isEmpty()) {
            return 0L;
        }
        return parseLong(header);
    }

    /**
     * 写入 HTTP 429 + JSON 错误体
     */
    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429); // HTTP 429 Too Many Requests（jakarta.servlet 未提供常量）
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(ERROR_BODY);
        response.getWriter().flush();
    }

    /**
     * 解析客户端 IP（优先取 X-Forwarded-For / X-Real-IP）
     */
    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            int comma = ip.indexOf(',');
            return comma > 0 ? ip.substring(0, comma).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 安全解析 Long，失败返回 0
     */
    private long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

}
