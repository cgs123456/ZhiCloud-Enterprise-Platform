# 变更记录

只记录使用者可感知的变更；逐条审计台账与证据见 `docs/enterprise-platform/`。

## 未发布（2026-10-20）

### 交付

- 单机 Compose 改为「空库 + 迁移 + 引导管理员」：新增 `db-migration` 与 `bootstrap-admin` 一次性服务，`server` 关闭自动迁移；不再挂载历史 initdb SQL（与平台 SQL 已不同步且不含任何账号）。
- 新增 V218 Flowable 引擎表迁移（幂等），prod 禁用自动 DDL 时不再缺表。
- 修复 prod profile 的 5 个启动阻塞：`autoconfigure.exclude` 覆盖语义、`wx`/`miniapp` 占位、Flowable 表、AI-RAG 默认关闭、引导管理员口令长度。
- 前端生产构建改为同源（`VITE_BASE_URL=''`），登录页预填租户名与管理员账号。
- 引导管理员口令窗口收敛为 12–16 位：脚本要求 ≥12，登录接口限制 ≤16，超长会入库但登不进。

### 修复（14 项业务缺陷）

- 月度考勤页切换标签卡死（布尔字典误用整数解析产生 NaN 键）。
- `crm_admin` 角色菜单为空（V216 补齐绑定，清理误绑）。
- 登录日志接口 401（axios 白名单误伤 `/system/login-log/page`）。
- 3 个菜单点击 404（前端路由重名）。
- MES / WMS PDA 后台页 404 / 501（调用了 APP 端接口）。
- IM 实时消息与 WebSocket 页握手失败（误用 refreshToken）。
- IoT 首页统计 500（接入 TDengine）。
- MES 物料清单（BOM）菜单空白（V217 修正组件路径大小写）。
- 公众号素材上传 401（上传组件携带 refreshToken）。
- CRM 商机状态分页 500。
- 公众号草稿/发布分页缺参 500。
- 前端未发送 `tenant-id` 请求头（`VITE_APP_TENANT_ENABLE` 未启用）。
- 路由错误处理对非 `Error` 值二次抛异常。
- 运费报表 `summary-method` 返回数字导致类型检查失败。

### 文档

- 按 GitHub README 写作规范重写根 README 与前端 README：首屏一句话、快速开始前置、正文控制在一屏可读，细节移交 `docs/`。
- 新增本文件、[CONTRIBUTING.md](CONTRIBUTING.md)、[SECURITY.md](SECURITY.md)；README 只保留链接。

## 2026-09 至 2026-10 摘要

- 商业域模块（Trade / Pay / Promotion / Member / Product）在 V140 整体移除（74 张表 + 3 个后端模块）。
- V115 软禁用全部公开种子账号并撤销其角色与 OAuth2 会话；登录凭据改由引导脚本 / K8s PreSync 生成。
- D 组重建 6 项：CRM 回款→ERP 收款联动、FMS 期初、WMS 库位维度流水、MES 产能并入工艺路线、CRM 电子签状态机（V213）、TMS↔WMS 签收全链。
- 迁移契约、错误码唯一性、@PreAuthorize、裸抛异常、事务原子性、重复建表等门禁进 CI。

> 更早的逐条记录（含失败原因与证据路径）见 `docs/enterprise-platform/handoff/uncommitted-review-groups-20260928.md`。
