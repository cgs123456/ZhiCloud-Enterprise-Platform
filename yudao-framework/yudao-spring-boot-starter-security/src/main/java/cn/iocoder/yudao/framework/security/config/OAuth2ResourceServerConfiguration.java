package cn.iocoder.yudao.framework.security.config;

import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.security.core.converter.JwtToLoginUserConverter;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;

/**
 * OAuth2 Resource Server 配置
 *
 * 当 {@code yudao.security.oauth2.enabled=true} 时生效，
 * 支持对接外部 IdP（Keycloak/Auth0/Authing 等）。
 *
 * 设计要点：
 * 1. 通过 {@code @ConditionalOnClass} 确保仅在 OAuth2 依赖存在时加载，兼容 optional 依赖
 * 2. 通过 {@code @ConditionalOnProperty} 实现按需启用，默认关闭
 * 3. JWT claims 通过 {@link JwtToLoginUserConverter} 转换为 {@code LoginUser}，
 *    与现有 {@code @PreAuthorize("@ss.hasPermission('xxx')")} 权限体系兼容
 * 4. 自研 Token 和 OAuth2 JWT 可并存（TokenAuthenticationFilter 先处理，失败后由 OAuth2 接管）
 */
@Configuration
@ConditionalOnClass(JwtDecoder.class)
@ConditionalOnProperty(prefix = "yudao.security.oauth2", name = "enabled", havingValue = "true")
public class OAuth2ResourceServerConfiguration {

    @Resource
    private SecurityProperties securityProperties;

    /**
     * JWT 解码器
     *
     * 优先使用 jwkSetUri（直接指定 JWK Set 端点），
     * 其次使用 issuerUri（通过 OIDC 发现协议自动获取）。
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecurityProperties.OAuth2 oauth2 = securityProperties.getOauth2();
        if (StrUtil.isNotBlank(oauth2.getJwkSetUri())) {
            return NimbusJwtDecoder.withJwkSetUri(oauth2.getJwkSetUri()).build();
        }
        if (StrUtil.isNotBlank(oauth2.getIssuerUri())) {
            return NimbusJwtDecoder.withIssuerLocation(oauth2.getIssuerUri()).build();
        }
        throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, "OAuth2 Resource Server 启用时，必须配置 yudao.security.oauth2.jwk-set-uri 或 issuer-uri");
    }

    /**
     * JWT → LoginUser 转换器
     *
     * 将外部 IdP 的 JWT claims 转换为项目内部的 LoginUser，兼容现有权限体系。
     */
    @Bean
    public JwtToLoginUserConverter jwtToLoginUserConverter() {
        return new JwtToLoginUserConverter(securityProperties.getOauth2());
    }

    /**
     * JWT → Authentication 转换器
     *
     * 将 JWT 转换为 Spring Security Authentication：
     * - principal 使用 {@link JwtToLoginUserConverter} 转换为 LoginUser（而非默认的 Jwt）
     * - authorities 从配置的 claim（默认 scope）提取，前缀为 SCOPE_
     *
     * 注意：JwtAuthenticationConverter 不支持 setPrincipalConverter（该 API 在部分版本不存在），
     * 因此直接实现 {@link Converter}，使用 {@link UsernamePasswordAuthenticationToken}
     * 三参构造器设置 LoginUser 为 principal（与 {@code SecurityFrameworkUtils.buildAuthentication} 一致）。
     */
    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(
            JwtToLoginUserConverter converter) {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName(securityProperties.getOauth2().getAuthoritiesClaim());
        return jwt -> {
            LoginUser loginUser = converter.convert(jwt);
            Collection<? extends GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
            return new UsernamePasswordAuthenticationToken(loginUser, jwt, authorities);
        };
    }

    /**
     * HttpSecurity 定制器
     *
     * 将 OAuth2 Resource Server 配置注入 SecurityFilterChain。
     * 使用 {@link HttpSecurityCustomizer}（声明 throws Exception）而非 Spring Security 的 Customizer，
     * 因为 {@code HttpSecurity.oauth2ResourceServer()} 声明了 throws Exception。
     * 该 Bean 类型不含 OAuth2 类，可安全注入到 {@link YudaoWebSecurityConfigurerAdapter}。
     */
    @Bean
    public Customizer<HttpSecurity> oauth2ResourceServerCustomizer(
            JwtDecoder jwtDecoder,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter) {
        return http -> {
            try {
                http.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter)));
            } catch (Exception e) {
                throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, e.getMessage(), e);
            }
        };
    }

}
