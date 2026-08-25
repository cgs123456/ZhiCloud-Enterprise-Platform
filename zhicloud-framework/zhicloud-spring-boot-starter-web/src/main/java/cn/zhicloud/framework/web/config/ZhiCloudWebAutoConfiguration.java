package cn.zhicloud.framework.web.config;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.biz.infra.logger.ApiErrorLogCommonApi;
import cn.zhicloud.framework.common.enums.WebFilterOrderEnum;
import cn.zhicloud.framework.web.core.filter.CacheRequestBodyFilter;
import cn.zhicloud.framework.web.core.filter.DemoFilter;
import cn.zhicloud.framework.web.core.filter.SecurityHeadersFilter;
import cn.zhicloud.framework.web.core.handler.GlobalExceptionHandler;
import cn.zhicloud.framework.web.core.handler.GlobalResponseBodyHandler;
import cn.zhicloud.framework.web.core.util.WebFrameworkUtils;
import com.google.common.collect.Maps;
import jakarta.servlet.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.function.Predicate;

@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class)
@Slf4j
public class ZhiCloudWebAutoConfiguration {

    /**
     * 应用名
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * CORS 允许的源地址白名单，逗号分隔
     *
     * <p><b>安全默认值</b>：仅放行本机开发地址，<b>不再默认 {@code *}</b>。
     * {@code *} 与 {@code allowCredentials=true} 组合会让任意站点携带用户 Cookie 发起跨域请求
     * （CSRF / 凭据泄露），因此默认值收敛为 localhost。
     *
     * <p>生产环境必须显式配置具体域名白名单，例如：
     * {@code zhicloud.web.cors.allowed-origins: https://admin.example.com,https://app.example.com}
     */
    @Value("${zhicloud.web.cors.allowed-origins:http://localhost:[*],http://127.0.0.1:[*]}")
    private String corsAllowedOrigins;

    @Bean
    public WebMvcRegistrations webMvcRegistrations(WebProperties webProperties) {
        return new WebMvcRegistrations() {

            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
                // 实例化时就带上前缀
                mapping.setPathPrefixes(buildPathPrefixes(webProperties));
                return mapping;
            }

            /**
             * 构建 prefix → 匹配条件的映射
             */
            private Map<String, Predicate<Class<?>>> buildPathPrefixes(WebProperties webProperties) {
                AntPathMatcher antPathMatcher = new AntPathMatcher(".");
                Map<String, Predicate<Class<?>>> pathPrefixes = Maps.newLinkedHashMapWithExpectedSize(2);
                putPathPrefix(pathPrefixes, webProperties.getAdminApi(), antPathMatcher);
                putPathPrefix(pathPrefixes, webProperties.getAppApi(), antPathMatcher);
                return pathPrefixes;
            }

            /**
             * 设置 API 前缀，仅仅匹配 controller 包下的
             */
            private void putPathPrefix(Map<String, Predicate<Class<?>>> pathPrefixes, WebProperties.Api api, AntPathMatcher matcher) {
                if (api == null || StrUtil.isEmpty(api.getPrefix())) {
                    return;
                }
                pathPrefixes.put(api.getPrefix(), // api 前缀
                        clazz -> clazz.isAnnotationPresent(RestController.class)
                                && matcher.match(api.getController(), clazz.getPackage().getName()));
            }

        };
    }

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public GlobalExceptionHandler globalExceptionHandler(ApiErrorLogCommonApi apiErrorLogApi) {
        return new GlobalExceptionHandler(applicationName, apiErrorLogApi);
    }

    @Bean
    public GlobalResponseBodyHandler globalResponseBodyHandler() {
        return new GlobalResponseBodyHandler();
    }

    @Bean
    @SuppressWarnings("InstantiationOfUtilityClass")
    public WebFrameworkUtils webFrameworkUtils(WebProperties webProperties) {
        // 由于 WebFrameworkUtils 需要使用到 webProperties 属性，所以注册为一个 Bean
        return new WebFrameworkUtils(webProperties);
    }

    // ========== Filter 相关 ==========

    /**
     * 创建 CorsFilter Bean，解决跨域问题
     *
     * <p>安全策略：通过 {@code zhicloud.web.cors.allowed-origins} 配置白名单
     * <ul>
     *   <li>默认仅放行 localhost / 127.0.0.1（本机开发）</li>
     *   <li>生产环境必须配置具体域名，如
     *       {@code zhicloud.web.cors.allowed-origins: https://admin.example.com,https://app.example.com}</li>
     *   <li>若显式配置了通配 {@code *}，则强制关闭 {@code allowCredentials}——
     *       「任意源 + 携带凭据」等价于把会话 Cookie 交给全网，浏览器规范本身也禁止该组合，
     *       此处主动降级为不带凭据，避免出现「配置写了但静默失效 / 或被绕过」的灰区</li>
     *   <li>白名单为空时不注册任何 allowedOrigin，即默认拒绝跨域，而非默认放行</li>
     * </ul>
     */
    @Bean
    @RefreshScope // P1 Nacos: CORS config can be dynamically refreshed via Nacos
    @Order(value = WebFilterOrderEnum.CORS_FILTER) // 特殊：修复因执行顺序影响到跨域配置不生效问题
    public FilterRegistrationBean<CorsFilter> corsFilterBean() {
        // 创建 CorsConfiguration 对象
        CorsConfiguration config = new CorsConfiguration();
        // 解析白名单，支持逗号分隔的多域名
        boolean wildcard = false;
        int registered = 0;
        if (StrUtil.isNotBlank(corsAllowedOrigins)) {
            for (String origin : corsAllowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.contains("*")) {
                    wildcard = true;
                }
                config.addAllowedOriginPattern(trimmed);
                registered++;
            }
        }
        // 通配源禁止携带凭据；其余情况才允许 Cookie / Authorization 跨域
        config.setAllowCredentials(!wildcard);
        if (wildcard) {
            log.error("[corsFilterBean][检测到 zhicloud.web.cors.allowed-origins 配置为通配 *，已强制关闭 allowCredentials。"
                    + "生产环境请改为显式域名白名单]");
        }
        if (registered == 0) {
            log.warn("[corsFilterBean][未配置 zhicloud.web.cors.allowed-origins，已默认拒绝所有跨域请求]");
        }
        config.addAllowedHeader("*"); // 设置访问源请求头
        config.addAllowedMethod("*"); // 设置访问源请求方法
        config.setMaxAge(1800L); // 预检结果缓存 30 分钟，降低 OPTIONS 压力
        // 创建 UrlBasedCorsConfigurationSource 对象
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 对接口配置跨域设置
        return createFilterBean(new CorsFilter(source), WebFilterOrderEnum.CORS_FILTER);
    }

    /**
     * 创建 RequestBodyCacheFilter Bean，可重复读取请求内容
     */
    @Bean
    public FilterRegistrationBean<CacheRequestBodyFilter> requestBodyCacheFilter() {
        return createFilterBean(new CacheRequestBodyFilter(), WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER);
    }

    /**
     * 创建 DemoFilter Bean，演示模式
     */
    @Bean
    @ConditionalOnProperty(value = "zhicloud.demo", havingValue = "true")
    public FilterRegistrationBean<DemoFilter> demoFilter() {
        return createFilterBean(new DemoFilter(), WebFilterOrderEnum.DEMO_FILTER);
    }

    /**
     * 创建 SecurityHeadersFilter Bean，统一注入安全响应头
     *
     * 补充 Spring Security {@code headers()} 配置未覆盖的响应头：
     * X-XSS-Protection、Referrer-Policy、Permissions-Policy，并对未启用 Security 的模块兜底设置 CSP/HSTS 等。
     * 排除 Actuator 与 Swagger/Knife4j 路径，避免影响其页面加载。
     */
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        return createFilterBean(new SecurityHeadersFilter(), WebFilterOrderEnum.SECURITY_HEADERS_FILTER);
    }

    public static <T extends Filter> FilterRegistrationBean<T> createFilterBean(T filter, Integer order) {
        FilterRegistrationBean<T> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(order);
        return bean;
    }

    /**
     * 创建 RestTemplate 实例
     *
     * @param restTemplateBuilder {@link RestTemplateAutoConfiguration#restTemplateBuilder}
     */
    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

}
