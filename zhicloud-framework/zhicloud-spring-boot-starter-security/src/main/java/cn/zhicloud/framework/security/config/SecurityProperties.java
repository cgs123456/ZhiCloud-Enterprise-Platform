package cn.zhicloud.framework.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

@ConfigurationProperties(prefix = "zhicloud.security")
@Validated
@Data
public class SecurityProperties {

    /**
     * HTTP 请求时，访问令牌的请求 Header
     */
    @NotEmpty(message = "Token Header 不能为空")
    private String tokenHeader = "Authorization";
    /**
     * HTTP 请求时，访问令牌的请求参数
     *
     * 初始目的：解决 WebSocket 无法通过 header 传参，只能通过 token 参数拼接
     */
    @NotEmpty(message = "Token Parameter 不能为空")
    private String tokenParameter = "token";

    /**
     * mock 模式的开关
     */
    @NotNull(message = "mock 模式的开关不能为空")
    private Boolean mockEnable = false;
    /**
     * mock 模式的密钥
     *
     * 安全要求：必须显式配置，不提供默认值，防止 mock 模式误启用时被弱密钥绕过认证。
     * 仅当 {@link #mockEnable} 为 true 时需要配置，启动时由 {@code TokenAuthenticationFilter} 校验非空。
     */
    private String mockSecret;

    /**
     * 免登录的 URL 列表
     */
    private List<String> permitAllUrls = Collections.emptyList();

    /**
     * PasswordEncoder 加密复杂度，越高开销越大
     *
     * 安全要求：BCrypt strength 必须 ≥ 10，防止暴力破解（N5 修复）
     * 参考：https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html
     */
    private Integer passwordEncoderLength = 10;

    /**
     * TOTP 双因素认证配置
     *
     * 仅控制 TOTP 功能的总开关；具体是否需要 TOTP 验证仍取决于每个用户是否已绑定 TOTP。
     */
    private Totp totp = new Totp();

    /**
     * OAuth2 Resource Server 配置（对接外部 IdP 如 Keycloak/Auth0）
     *
     * 启用后支持 JWT Bearer Token 验证，与自研 Token 并存。
     * 默认关闭，按需启用。
     */
    private OAuth2 oauth2 = new OAuth2();

    /**
     * TOTP 双因素认证配置项
     */
    @Data
    public static class Totp {
        /**
         * 是否启用 TOTP 双因素认证功能（仅对已绑定 TOTP 的用户生效）
         */
        private Boolean enabled = true;
    }

    /**
     * OAuth2 Resource Server 配置项
     */
    @Data
    public static class OAuth2 {
        /**
         * 是否启用 OAuth2 Resource Server（默认关闭，按需启用）
         */
        private Boolean enabled = false;
        /**
         * JWT 签名 JWK Set URI
         *
         * 例如 Keycloak: http://keycloak:8080/realms/zhicloud/protocol/openid-connect/certs
         */
        private String jwkSetUri;
        /**
         * JWT Issuer URI（通过 OIDC 发现协议自动获取 JWK Set）
         *
         * 例如 Keycloak: http://keycloak:8080/realms/zhicloud
         */
        private String issuerUri;
        /**
         * JWT claims 中用户 ID 的字段名
         */
        private String userIdClaim = "sub";
        /**
         * JWT claims 中租户 ID 的字段名
         */
        private String tenantIdClaim = "tenant_id";
        /**
         * JWT claims 中权限列表的字段名
         *
         * 例如 scope（空格分隔字符串）或 realm_access.roles（Keycloak 角色列表）
         */
        private String authoritiesClaim = "scope";
    }
}
