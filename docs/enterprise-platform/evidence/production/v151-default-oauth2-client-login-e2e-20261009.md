# V151 default OAuth2 客户端：迁移实库执行 + 管理员登录端到端证据

- 生成时间：2026-10-09（Asia/Singapore）
- 结论性质：**隔离环境实库执行 + 真实登录链路证据**；不是生产等价验证
- 相关交接：`docs/enterprise-platform/handoff/uncommitted-review-groups-20260928.md` §12.163
- 相关迁移：`zhicloud-server/src/main/resources/db/migration/V151__default_oauth2_client_bootstrap.sql`
- 相关验证：`zhicloud-server/src/main/resources/db/verification/V151_verify.sql`

## 1. 缺陷与复现口径

`AdminAuthServiceImpl.createTokenAfterLoginSuccess()` 硬编码
`oauth2TokenService.createAccessToken(userId, userType, OAuth2ClientConstants.CLIENT_ID_DEFAULT /* "default" */, null)`；
`OAuth2TokenServiceImpl.createAccessToken()` 第一步即 `oauth2ClientService.validOAuthClientFromCache("default")`，
客户端不存在时抛 `OAUTH2_CLIENT_NOT_EXISTS`（错误码 `1002020000`，msg「OAuth2 客户端不存在」）。

而 V1.1 的 `system_oauth2_client` 记录区在提交 `63f99a812`（drop V1.1 seed credentials）后为 `BEGIN; COMMIT;`，
各方言安装脚本同样为空 → 全新库该表为空，任何账号密码登录都失败。

修复前实测（本机隔离库 `zhicloud_platform`）：

| 观测 | 值 |
|---|---|
| `SELECT COUNT(*) FROM system_oauth2_client` | 0 |
| `POST /admin-api/system/auth/login`（tenant-id:1） | `code=1002020000, msg="OAuth2 客户端不存在"` |

## 2. 环境

| 项 | 值 |
|---|---|
| MySQL | `mysql:8.4`，镜像 ID `sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d`，server `8.4.11` |
| 后端运行时 | `maven:3.9.11-eclipse-temurin-21`，`openjdk 21.0.9 2025-10-21 LTS (Temurin-21.0.9+10)` |
| 后端 jar | `zhicloud-server/target/zhicloud-server.jar`，SHA-256 `D58FF7B1CABA54FD6CC2419782474175E4F80336F967C4038C3DF201074199C9` |
| 后端 profile | `local`（`--spring.profiles.active=local`，`--zhicloud.security.mock-enable=true`） |
| 源码版本 | `master@f6b89039573ffd9f3c3e0bb02f6244f711303fa0` + 本轮未提交改动 |
| 容器 | `zc-live-mysql`(55101) / `zc-live-redis`(16380) / `zc-live-rabbit`(56721) / `zc-live-server`(48080,48090)，均为本机隔离，未触碰 `amz-*` / `ta-*` |

## 3. Flyway 真实应用（非手工执行）

`docker restart zc-live-server` 后容器内 Flyway 日志（摘录）：

```
Successfully validated 159 migrations (execution time 00:00.541s)
Migrating schema `zhicloud_platform` to version "151 - default oauth2 client bootstrap"
| V151_DEFAULT_CLIENT_READY PASS |
| V151_DEFAULT_CLIENT_UNIQUE PASS |
Successfully applied 1 migration to schema `zhicloud_platform`, now at version v151 (execution time 00:00.074s)
```

即：V151 由 Flyway 通道在 MySQL 8.4.11 上执行成功，`flyway_schema_history` 出现 `version=151, success=1`。

## 4. V151_verify.sql 断言结果（全部 PASS）

```
| V151_FLYWAY_APPLIED PASS |
| V151_DEFAULT_CLIENT_USABLE PASS |
| V151_DEFAULT_CLIENT_UNIQUE PASS |
| V151_NO_ANONYMOUS_GRANT PASS |
| V151_SECRET_ROTATED PASS |
```

## 5. 幂等复跑

对同一库再次执行 V151 正文（`mysql < V151...sql`）：

```
BEFORE=43|0|c3cea7c257f5d2c2e6aaf5d691d73f700cc4490655643f4b6bf5f6c9d8ba3a3f|["password","authorization_code","implicit","refresh_token"]
AFTER =43|0|c3cea7c257f5d2c2e6aaf5d691d73f700cc4490655643f4b6bf5f6c9d8ba3a3f|["password","authorization_code","implicit","refresh_token"]
IDEMPOTENT_PASS (row unchanged on re-run)
```

（`43` 为自增 id；签名含 `status` 与 Secret 的 SHA-256，重跑后完全一致，未新增行、未改写 Secret。）

## 6. 管理员登录端到端（真实 48080）

账号 `zcadmin`（`system_users.id=225, tenant_id=1, status=0, role_id=1`），口令由
`scripts/production/init_bootstrap_admin.py --password-bytes 12` 渲染（明文仅存本机 `Temp/`，不入库、不提交）。

| 步骤 | 请求 | 结果 |
|---|---|---|
| 登录 | `POST /admin-api/system/auth/login`，`tenant-id: 1` | `HTTP 200`，`code=0`，`userId=225`，返回 `accessToken` / `refreshToken` / `expiresTime`（令牌值已脱敏） |
| 权限信息 | `GET /admin-api/system/auth/get-permission-info`，`Authorization: Bearer <accessToken>` | `HTTP 200`，`code=0`，`user=zcadmin`，`roles=1`，`permissions=1232` |
| 刷新令牌 | `POST /admin-api/system/auth/refresh-token?refreshToken=<refreshToken>` | `HTTP 200`，`code=0`，返回新 `accessToken`，`userId=225`（同用户） |

登录响应示例（**令牌已替换为 `<redacted>`**）：

```json
{"code":0,"msg":"","data":{"userId":225,"accessToken":"<redacted>","refreshToken":"<redacted>","expiresTime":1791533846802}}
```

`expiresTime` 与 `access_token_validity_seconds=1800` 一致（登录时刻 +1800s）。

## 7. 新启动窗口的非阻断告警（登记，不作为结论）

- `SkillCatalogValidator`：`TenantContextHolder 不存在租户编号`，WARN 后「技能目录校验异常，跳过」，启动继续（AI 模块，非登录链路）。
- `ClassPathMapperScanner`：跳过若干 `MapperFactoryBean`（重复扫描告警）。
- `Flyway`：`MySQL 8.4 is newer than this version of Flyway`，仅建议升级。
- `MeterRegistry`：Gauge 重复注册告警。
- 以上均未阻断启动，`Started ZhiCloudServerApplication` + `项目启动成功！` 均出现。

## 8. 边界（不得外推）

- 本证据来自**隔离 Docker + 本机 MySQL/Redis/RabbitMQ**，不是生产等价环境；未覆盖生产拓扑、故障注入、规模、真实网关与 TLS。
- 令牌与管理员口令**不落库、不入库、不提交**；本文件已脱敏。
- 未在本机直连 JVM 运行（本机 JDK 存在 `Unable to establish loopback connection` / `WEPollSelectorImpl` 环境缺陷，与业务代码无关），故改用容器内 JVM。
- 本证据只证明「default 客户端缺失导致登录不可用」这一条被修复，不代表 Issue #1 全部闭环，也不改变
  `production_ready=false`、`gate3_passed=false`、`independent_review=false`、`Go/No-Go=No-Go`。
