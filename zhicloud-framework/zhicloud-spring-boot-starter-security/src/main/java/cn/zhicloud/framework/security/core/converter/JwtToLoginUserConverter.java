package cn.zhicloud.framework.security.core.converter;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.security.config.SecurityProperties;
import cn.zhicloud.framework.security.core.LoginUser;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * JWT → LoginUser 转换器
 *
 * 将外部 IdP（Keycloak/Auth0 等）签发的 JWT claims 转换为项目内部 LoginUser，
 * 使其与现有 {@code @PreAuthorize("@ss.hasPermission('xxx')")} 权限体系兼容。
 *
 * 设计要点：
 * 1. 用户 ID 从 claim（默认 sub）提取，映射到 LoginUser.id
 * 2. 租户 ID 从 claim（默认 tenant_id）提取，映射到 LoginUser.tenantId
 * 3. 授权范围从 claim（默认 scope）提取，映射到 LoginUser.scopes，兼容 @ss.hasScope('xxx')
 * 4. 权限校验仍走数据库（PermissionCommonApi），JWT 仅负责身份认证
 */
public class JwtToLoginUserConverter {

    private final SecurityProperties.OAuth2 oauth2Config;

    public JwtToLoginUserConverter(SecurityProperties.OAuth2 oauth2Config) {
        this.oauth2Config = oauth2Config;
    }

    /**
     * 将 JWT 转换为 LoginUser
     *
     * @param jwt 外部 IdP 签发的 JWT
     * @return 项目内部 LoginUser 对象
     */
    public LoginUser convert(Jwt jwt) {
        LoginUser loginUser = new LoginUser();

        // 1. 用户 ID
        Object userIdClaim = jwt.getClaim(oauth2Config.getUserIdClaim());
        if (userIdClaim instanceof Number) {
            loginUser.setId(((Number) userIdClaim).longValue());
        } else if (userIdClaim instanceof String && StrUtil.isNotBlank((String) userIdClaim)) {
            loginUser.setId(Long.valueOf((String) userIdClaim));
        }

        // 2. 租户 ID
        Object tenantIdClaim = jwt.getClaim(oauth2Config.getTenantIdClaim());
        if (tenantIdClaim instanceof Number) {
            loginUser.setTenantId(((Number) tenantIdClaim).longValue());
        } else if (tenantIdClaim instanceof String && StrUtil.isNotBlank((String) tenantIdClaim)) {
            loginUser.setTenantId(Long.valueOf((String) tenantIdClaim));
        }

        // 3. 授权范围（scopes），兼容空格分隔字符串和列表两种格式
        List<String> scopes = extractScopes(jwt.getClaim(oauth2Config.getAuthoritiesClaim()));
        loginUser.setScopes(scopes);

        // 4. 过期时间
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt != null) {
            loginUser.setExpiresTime(expiresAt.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }

        return loginUser;
    }

    /**
     * 从 claim 中提取 scopes 列表
     *
     * 兼容两种格式：
     * - 空格分隔字符串（标准 OAuth2 scope）："read write admin"
     * - 字符串列表（部分 IdP）：["read", "write", "admin"]
     */
    @SuppressWarnings("unchecked")
    private List<String> extractScopes(Object claimValue) {
        if (claimValue == null) {
            return Collections.emptyList();
        }
        if (claimValue instanceof List) {
            return (List<String>) claimValue;
        }
        if (claimValue instanceof String) {
            String str = (String) claimValue;
            if (StrUtil.isBlank(str)) {
                return Collections.emptyList();
            }
            return Arrays.asList(str.split("\\s+"));
        }
        // Keycloak 的 realm_access.roles 格式：{realm_access: {roles: [xxx]}}
        if (claimValue instanceof Map) {
            Object roles = ((Map<String, Object>) claimValue).get("roles");
            if (roles instanceof List) {
                return (List<String>) roles;
            }
        }
        return Collections.emptyList();
    }

}
