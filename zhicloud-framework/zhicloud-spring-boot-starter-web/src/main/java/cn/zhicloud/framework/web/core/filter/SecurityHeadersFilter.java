package cn.zhicloud.framework.web.core.filter;

import cn.hutool.core.util.StrUtil;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 安全响应头 Filter
 *
 * 补充 Spring Security {@code headers()} 配置未覆盖的响应头：
 * 1. X-XSS-Protection（Spring Security 6.x 已移除内置支持）
 * 2. Referrer-Policy
 * 3. Permissions-Policy
 * 4. 兜底设置 X-Content-Type-Options / X-Frame-Options / HSTS / CSP（仅在未设置时补齐）
 *
 * 排除 Actuator 与 Swagger/Knife4j 路径，避免影响其页面加载。
 *
 * @author 智云
 */
public class SecurityHeadersFilter extends OncePerRequestFilter {

    /**
     * 需要排除的 URI 前缀
     *
     * 1. Actuator 监控端点
     * 2. Swagger / Knife4j / OpenAPI 文档
     */
    private static final String[] IGNORE_URIS = {
            "/actuator/",
            "/swagger-ui",
            "/swagger-resources",
            "/v3/api-docs",
            "/doc.html",
            "/webjars/"
    };

    /**
     * 安全响应头默认值
     */
    private static final String HEADER_X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String VALUE_X_CONTENT_TYPE_OPTIONS = "nosniff";

    private static final String HEADER_X_FRAME_OPTIONS = "X-Frame-Options";
    private static final String VALUE_X_FRAME_OPTIONS = "DENY";

    private static final String HEADER_X_XSS_PROTECTION = "X-XSS-Protection";
    private static final String VALUE_X_XSS_PROTECTION = "1; mode=block";

    private static final String HEADER_HSTS = "Strict-Transport-Security";
    private static final String VALUE_HSTS = "max-age=31536000; includeSubDomains";

    private static final String HEADER_REFERRER_POLICY = "Referrer-Policy";
    private static final String VALUE_REFERRER_POLICY = "strict-origin-when-cross-origin";

    private static final String HEADER_PERMISSIONS_POLICY = "Permissions-Policy";
    private static final String VALUE_PERMISSIONS_POLICY = "geolocation=(), microphone=(), camera=()";

    private static final String HEADER_CSP = "Content-Security-Policy";
    private static final String VALUE_CSP = "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; "
            + "style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; "
            + "font-src 'self' data:; connect-src 'self' https:; frame-ancestors 'none'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        // 在调用后续链之前设置响应头，确保在响应提交前生效
        setHeaderIfAbsent(response, HEADER_X_CONTENT_TYPE_OPTIONS, VALUE_X_CONTENT_TYPE_OPTIONS);
        setHeaderIfAbsent(response, HEADER_X_FRAME_OPTIONS, VALUE_X_FRAME_OPTIONS);
        setHeaderIfAbsent(response, HEADER_X_XSS_PROTECTION, VALUE_X_XSS_PROTECTION);
        setHeaderIfAbsent(response, HEADER_HSTS, VALUE_HSTS);
        setHeaderIfAbsent(response, HEADER_REFERRER_POLICY, VALUE_REFERRER_POLICY);
        setHeaderIfAbsent(response, HEADER_PERMISSIONS_POLICY, VALUE_PERMISSIONS_POLICY);
        // CSP 仅对 HTML 响应设置（非 HTML 响应如 JSON/API 无需 CSP）
        // 此处先设置兜底 CSP，若 Spring Security 已设置则会跳过
        setHeaderIfAbsent(response, HEADER_CSP, VALUE_CSP);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 排除 Actuator 与 Swagger/Knife4j 路径，避免影响其页面加载
        String requestURI = request.getRequestURI();
        return StrUtil.startWithAny(requestURI, IGNORE_URIS);
    }

    /**
     * 设置响应头（仅在未设置时补齐，避免覆盖 Spring Security 已设置的值）
     */
    private void setHeaderIfAbsent(HttpServletResponse response, String name, String value) {
        if (response.getHeader(name) == null) {
            response.setHeader(name, value);
        }
    }

}
