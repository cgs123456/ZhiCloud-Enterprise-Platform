# OAuth2 Resource Server + Keycloak 对接指南

本文档介绍如何通过 Keycloak 作为外部身份提供商（IdP），为 zhicloud 平台接入 OAuth2 JWT Bearer Token 认证。

## 架构概览

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   前端应用    │────▶│  zhicloud 后端   │────▶│   Keycloak    │
│  (Bearer JWT) │     │ (Resource     │     │  (IdP / OP)   │
│              │     │   Server)     │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
                            │
                     ┌──────┴──────┐
                     │  zhicloud DB   │
                     │ (权限/用户)  │
                     └─────────────┘
```

### 认证流程

1. 用户通过 Keycloak 登录，获取 JWT Access Token
2. 前端携带 `Authorization: Bearer <jwt>` 请求 zhicloud 后端
3. zhicloud 后端（Resource Server）验证 JWT 签名（通过 JWK Set）
4. JWT claims 转换为内部 `LoginUser` 对象，注入 Spring Security 上下文
5. 权限校验仍走 zhicloud 数据库（`@ss.hasPermission('xxx')`）

### 与自研 Token 的并存策略

zhicloud 支持自研 Token 和 OAuth2 JWT **同时工作**：

- **TokenAuthenticationFilter**（自研 Token）：先执行，通过 `OAuth2TokenCommonApi.checkAccessToken` 校验
- **BearerTokenAuthenticationFilter**（OAuth2 JWT）：若自研 Token 校验失败（返回 null），由 Spring Security OAuth2 接管
- 两套认证使用相同的 `Authorization: Bearer xxx` Header，通过 Token 格式自动区分

过渡策略建议：
1. **阶段一**：保持 `oauth2.enabled=false`，所有请求走自研 Token
2. **阶段二**：启用 `oauth2.enabled=true`，外部 IdP 用户和内部用户并存
3. **阶段三**：逐步将内部用户迁移至 Keycloak，最终停用自研 Token

---

## 一、Keycloak Docker 部署

### 1.1 docker-compose 部署

创建 `docker-compose-keycloak.yml`：

```yaml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    container_name: keycloak
    environment:
      # 数据库配置（生产环境必须使用外部数据库）
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: ${KEYCLOAK_DB_PASSWORD}
      # Keycloak 管理员账号
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
      # 启用健康检查和指标
      KC_HEALTH_ENABLED: 'true'
      KC_METRICS_ENABLED: 'true'
      # 代理设置（若在反向代理后）
      KC_PROXY: edge
      KC_HOSTNAME: ${KEYCLOAK_HOSTNAME:-localhost}
    ports:
      - "8080:8080"
    command: start
    depends_on:
      - postgres

  postgres:
    image: postgres:16
    container_name: keycloak-postgres
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: ${KEYCLOAK_DB_PASSWORD}
    volumes:
      - keycloak_pgdata:/var/lib/postgresql/data

volumes:
  keycloak_pgdata:
```

### 1.2 启动

```bash
# 设置环境变量
export KEYCLOAK_DB_PASSWORD=your_db_password
export KEYCLOAK_ADMIN_PASSWORD=your_admin_password
export KEYCLOAK_HOSTNAME=keycloak.example.com

# 启动
docker-compose -f docker-compose-keycloak.yml up -d

# 验证
curl http://localhost:8080/health/ready
```

---

## 二、Realm / Client / Scope 配置

### 2.1 创建 Realm

1. 访问 `http://localhost:8080/admin`，使用管理员账号登录
2. 点击左上角下拉 → **Create Realm**
3. Realm name: `zhicloud`
4. Enabled: ON → **Create**

### 2.2 创建 Client

1. 进入 `zhicloud` Realm → **Clients** → **Create client**
2. 配置如下：

| 参数 | 值 | 说明 |
|------|-----|------|
| Client ID | `zhicloud-server` | zhicloud 后端作为 Resource Server |
| Client type | `OpenID Connect` | |
| Client authentication | ON | 启用机密客户端 |
| Authorization | OFF | 不使用 Keycloak 的授权服务 |
| Valid redirect URIs | `*` | 生产环境需收敛 |
| Web origins | `*` | CORS 配置 |

3. **Credentials** 标签页 → 复制 **Client secret**（后续配置用）

### 2.3 配置 Client Scopes

Keycloak 默认在 JWT 中包含 `sub`、`scope`、`realm_access.roles` 等 claim。

#### 自定义 tenant_id claim（可选）

若需要在 JWT 中携带租户 ID：

1. **Client scopes** → **Create client scope**
2. Name: `tenant_id`
3. Type: `Optional`
4. **Mappers** → **Add mapper** → **Hardcoded claim** 或 **User Attribute**
   - User Attribute: `tenant_id`
   - Token Claim Name: `tenant_id`
   - Claim JSON Type: `long`
5. 将此 scope 分配给 `zhicloud-server` Client（**Add to default scope**）

### 2.4 创建用户

1. **Users** → **Add user**
2. 填写 Username、Email，保存
3. **Credentials** 标签页 → 设置密码
4. **Role mappings** → 分配 `realm-admin` 或自定义角色

---

## 三、JWT Claims 结构

### 3.1 标准 JWT Claims

Keycloak 签发的 JWT Access Token 包含以下关键 claims：

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "iss": "http://keycloak:8080/realms/zhicloud",
  "exp": 1735689600,
  "iat": 1735686000,
  "scope": "openid profile email",
  "realm_access": {
    "roles": ["default-roles-zhicloud", "admin"]
  },
  "resource_access": {
    "zhicloud-server": {
      "roles": ["zhicloud-admin"]
    }
  }
}
```

### 3.2 zhicloud claims 映射

| JWT Claim | zhicloud LoginUser 字段 | 配置项 | 默认值 |
|-----------|---------------------|--------|--------|
| `sub` | `id` | `zhicloud.security.oauth2.user-id-claim` | `sub` |
| `tenant_id`（自定义） | `tenantId` | `zhicloud.security.oauth2.tenant-id-claim` | `tenant_id` |
| `scope` | `scopes` | `zhicloud.security.oauth2.authorities-claim` | `scope` |

### 3.3 权限校验机制

- **身份认证**：通过 JWT 签名验证（JWK Set）
- **权限校验**：通过 zhicloud 数据库的 `PermissionCommonApi`（`@ss.hasPermission('xxx')`）
- **Scope 校验**：通过 JWT 中的 scope claim（`@ss.hasScope('xxx')`）

> **注意**：OAuth2 用户必须在 zhicloud 系统用户表中存在（通过 `sub` 作为用户 ID 关联），否则 `hasPermission` 校验会因找不到用户而失败。

---

## 四、zhicloud 配置

### 4.1 添加依赖

在 `zhicloud-server/pom.xml` 中显式添加（security 模块中为 `optional=true`）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
```

### 4.2 application.yaml 配置

在 `application-prod.yaml`（或对应环境的配置文件）中：

```yaml
zhicloud:
  security:
    oauth2:
      enabled: ${OAUTH2_ENABLED:true}
      # 方式一：直接指定 JWK Set URI
      jwk-set-uri: ${OAUTH2_JWK_SET_URI:http://keycloak:8080/realms/zhicloud/protocol/openid-connect/certs}
      # 方式二：通过 OIDC 发现协议（与 jwk-set-uri 二选一）
      # issuer-uri: ${OAUTH2_ISSUER_URI:http://keycloak:8080/realms/zhicloud}
      user-id-claim: sub
      tenant-id-claim: tenant_id
      authorities-claim: scope
```

### 4.3 环境变量

```bash
OAUTH2_ENABLED=true
OAUTH2_JWK_SET_URI=http://keycloak:8080/realms/zhicloud/protocol/openid-connect/certs
# 或
OAUTH2_ISSUER_URI=http://keycloak:8080/realms/zhicloud
```

---

## 五、验证与测试

### 5.1 获取 JWT Token

```bash
# 通过 Keycloak Token 端点获取
curl -X POST http://localhost:8080/realms/zhicloud/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=zhicloud-server" \
  -d "client_secret=<your-client-secret>" \
  -d "username=admin" \
  -d "password=admin"
```

### 5.2 携带 JWT 请求 zhicloud API

```bash
curl -H "Authorization: Bearer <jwt-access-token>" \
  http://localhost:48080/admin-api/system/user/get?id=1
```

### 5.3 排查指南

| 问题 | 排查方向 |
|------|---------|
| 401 Unauthorized | 检查 JWT 是否过期、签名是否有效、JWK Set URI 是否可达 |
| 403 Forbidden | 检查 zhicloud 系统用户表中是否存在对应 `sub` 的用户 |
| JwtDecoder 初始化失败 | 检查 `jwk-set-uri` 或 `issuer-uri` 配置，确保 Keycloak 可达 |
| tenant_id 为空 | 检查 Keycloak Client Scope 是否配置了 tenant_id mapper |

---

## 六、其他 IdP 适配

本方案不绑定 Keycloak，支持任何兼容 OIDC 的外部 IdP：

| IdP | JWK Set URI | Issuer URI |
|-----|-------------|------------|
| Keycloak | `http://host/realms/{realm}/protocol/openid-connect/certs` | `http://host/realms/{realm}` |
| Auth0 | `https://{tenant}.auth0.com/.well-known/jwks.json` | `https://{tenant}.auth0.com/` |
| Authing | `https://{tenant}.authing.cn/oidc/jwks` | `https://{tenant}.authing.cn/oidc` |
| Okta | `https://{tenant}.okta.com/oauth2/default/v1/keys` | `https://{tenant}.okta.com/oauth2/default` |

只需修改 `jwk-set-uri` 或 `issuer-uri` 配置即可切换 IdP。
