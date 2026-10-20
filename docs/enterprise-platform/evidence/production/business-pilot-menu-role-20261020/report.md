# 单客户私有化试点 · 全菜单全角色业务验收（2026-10-20）

> **口径声明**：本文件记录的是**本地沙箱**（单机 Docker + 本地构建产物）上的业务可达性证据。
> 它**不等于生产就绪**：`production_ready=false`、`gate3_passed=false`、`independent_review=false`、
> `R-011=OPEN`、`R-029=OPEN`、`Go/No-Go=No-Go` 仍然成立；许可门禁仍为 `decision=fail / blocking=true`，
> 私有化分发需要法务单独处置。本文件的结论只回答一个问题：**单客户私有化试点口径下，
> 全菜单、全角色是否都能真实跑通**，以及还剩哪些需要部署侧/外部依赖配合的事项。

---

## 0. 验收口径

| 维度 | 口径 |
| --- | --- |
| 目标场景 | 单客户私有化试点 |
| 验收标准 | 全菜单（412 个菜单页）+ 全角色（种子与试点自定义角色）可跑通 |
| 判定单位 | 菜单页：浏览器真实加载 + 页面内 API 真实返回；角色：权限接口真实返回菜单与权限 |
| 证据级别 | 本地沙箱（MySQL 8.4 / Redis / RabbitMQ / TDengine 容器 + 本地构建 jar + 本地构建前端） |
| 不覆盖 | 生产拓扑、备份回滚演练、容量与压测、独立复核、许可合规 |

---

## 1. 验收环境

| 组件 | 版本/配置 |
| --- | --- |
| 后端 | `zhicloud-server.jar`（本地 `-pl zhicloud-server -am package`，V216/V217 迁移随包发布） |
| 数据库 | MySQL 8.4 容器 `zc-live-mysql`（`127.0.0.1:55101/zhicloud_sandbox_v140`） |
| 缓存/队列 | Redis `127.0.0.1:16380`、RabbitMQ `127.0.0.1:56721` |
| IoT 时序库 | TDengine 容器 `zc-live-tdengine`（`127.0.0.1:16041`，库 `ruoyi_vue_pro`） |
| 前端 | 本地 `build:prod` 产物 `dist-prod`，由本地静态服务 + `/admin-api` 反向代理承载（保持 Host，等价 nginx `proxy_set_header Host $host`） |
| 登录态 | 沙箱 mock token（`test-only-for-local<userId>`）+ 租户头，**仅作沙箱证据，不作生产认证证据** |

---

## 2. 覆盖口径

| 层 | 数量 | 采集方式 |
| --- | --- | --- |
| 菜单总量 | 2022（目录 56 / 菜单页 412 / 按钮 1554） | `system_menu` |
| 页面路由 smoke | 403 个 type=2 菜单页（另有 7 个"目录型"菜单页走父级重定向分支，见 §4.4） | Playwright + 生产构建前端 |
| API smoke | 325 个有主列表端点的页面 | 直接打后端接口 |
| 角色矩阵 | 7 个用户/角色组合 | `GET /system/auth/get-permission-info` |
| 菜单 ↔ 前端视图 | 405 个有 component 的菜单页逐个比对 | `import.meta.glob` 键集合比对 |

---

## 3. 本轮定位并修复的真实业务缺陷

> 下列问题都会让"菜单点开就是坏页 / 权限菜单为空 / 页面报错"，属于阻塞试点的真实缺陷，不是工程洁癖问题。

| # | 现象 | 根因 | 修复 | 证据 |
| --- | --- | --- | --- | --- |
| 1 | `crm_admin` 角色登录后菜单为空 | V1.1 种子把 role 3 只绑定了 MES 按钮 `5811` | 新增迁移 **V216**：删除误绑 + 递归授予整棵 CRM 菜单树（119 个节点） | `V216_verify.sql` 5/5 PASS；角色矩阵 `crm_admin` 顶级菜单 1、菜单节点 33、权限 102 |
| 2 | 登录日志页 401「账号未登录」 | 前端 axios 白名单用 `url.includes('/login')` 判断免鉴权，`/system/login-log/page` 被误判成登录接口 | `service.ts` 改为按**路径结尾**匹配白名单 | `GET /admin-api/system/login-log/page` → `code=0` |
| 3 | 质量文件、CRM 产品统计、WMS 基础数据菜单点开是 404 | 菜单缺 `componentName`，路由名按 path 兜底后**重名**；vue-router 同名路由会互相顶掉 | `routerHelper.ts` 新增 `ensureUniqueRouteNames`：显式 `componentName` 优先保留，兜底名冲突时按完整路径改名 | 运行时路由表 557 条、**0 重名**；`/qms/document`、`/crm/statistics/product`、`/wms/md` 均存在 |
| 4 | MES PDA 报工记录页 404 | 后端控制器放在 `controller.app`（前缀 `/app-api`），管理后台按 `/admin-api` 调用 | 新增 `MesProPdaFeedbackAdminController`（只读当前用户报工记录） | `GET /admin-api/mes-api/pro/feedback/my-feedback-list` → `code=0` |
| 5 | WMS PDA 作业页 501「WMS 已禁用」（误导） | 同上，管理后台调了 APP API；因为没有 admin 控制器，落到了 `DefaultController` 的"模块未启用"兜底 | 新增 `WmsPdaAdminController`（只读待执行任务） | `GET /admin-api/wms/pda/task/list` → `code=0`，9 条任务 |
| 6 | IoT 首页统计 500「系统异常」 | IoT 时序接口走 TDengine，未配置数据源时 `@DS("tdengine")` 静默回落主库，查 MySQL 的 `device_message` 表 | 沙箱接入 TDengine 数据源（`ruoyi_vue_pro`），启动 Runner 建 `device_message` 超级表 | `GET /admin-api/iot/statistics/get-summary` → `code=0`；按日统计 → `code=0` |
| 7 | IM 实时消息、基础设施 WebSocket 页连不上 | 握手用的是 **refreshToken**，后端 `/infra/ws` 走 `TokenAuthenticationFilter` 只认访问令牌 → 必然 401 | 两处改用 `getAccessToken()` | 裸握手 `GET /infra/ws?token=<访问令牌>` → **101 Switching Protocols** |
| 8 | 公众号素材上传 401 | `el-upload` 不走 axios 拦截器，代码里带的是 refreshToken | 改为 `getAccessToken()` | 代码路径复核（上传为外部依赖，见 §5） |
| 9 | MES 物料清单（BOM）菜单点开是空白页 | 菜单 `component` 写成 `mes/md/bom/index`，前端实际视图是 `views/mes/md/Bom/index.vue`（**大小写不一致**，`import.meta.glob` 区分大小写） | 新增迁移 **V217**：改这一条菜单的 component | 菜单 ↔ 视图比对：405 个菜单页 **全部有对应视图** |
| 10 | CRM 商机状态分页 500 | `creator='demo-seed'` 解析不到用户时空指针 | `CrmBusinessStatusController` 空值安全处理并保留原 creator | 复测 `code=0` |
| 11 | 公众号草稿/发布分页 500 | 缺 `accountId` 未做校验提示 | `MpDraftController`、`MpFreePublishController` 补 `@Valid` | 复测 `code=400 请求参数不正确:公众号账号的编号不能为空` |
| 12 | 前端所有请求 401/跨域失败 | `VITE_APP_TENANT_ENABLE` 未开启，axios 不发 `tenant-id`，后端返回 400；`router.onError` 又把二次异常掩盖 | 4 个 `.env*` 打开租户开关；`router.onError` 空值安全 | `/system/user` 正常加载（63 侧边栏、接口全 `code=0`） |
| 13 | **月度考勤菜单把浏览器标签页卡死**（页面永远渲染不完，只能强关） | 字典 `hrm_attendance_yes_no` 的值是 `true`/`false`，页面却用 `getIntDictOptions` 解析 → `parseInt('true')` = **NaN** → `el-option` 的 key 全是 NaN → Vue 每次渲染都刷 "invalid key (NaN)" 警告，形成渲染-警告自反馈死循环（单页产生 2000+ 条警告） | 页面改用 `getBoolDictOptions`（值本身就是布尔）；并全量扫描"布尔字典 × `getIntDictOptions` 调用点"，确认无第二处 | 修复后该页 0 警告、正常渲染；扫描结果：无其它同类问题 |
| 14 | WMS ABC 分析页每次进入都弹一个红色参数错误 | 页面挂载即调用"触发分析"接口，而该接口必填起止日期；未选日期时后端返回 400（页面虽有兜底回退到"取报告"，但错误提示已经弹出） | 未选日期时直接取报告接口，不再调用分析接口 | 进入页面 0 错误提示，报告数据正常展示 |

> 排查方法备注：这类"页面卡死"用 CDP `Debugger.pause` 能直接抓到主线程栈（停在 `console.warn → stringifyConsoleArg`），
> 再切到 dev server 跑一次即可拿到未压缩的函数名与组件栈。
>
> 校验口径备注：MySQL 默认排序规则 `utf8mb4_unicode_ci` **大小写不敏感**，`component = 'mes/md/Bom/index'` 会把
> 小写旧值也判成相等。V217 的校验 SQL 已改用 `BINARY` 比较，否则"大小写已修正"是假通过。

---

## 4. 验收结果

### 4.1 页面路由 smoke（403 个菜单页）

- 结果见 `ui-smoke-results.csv`（逐页记录：最终 URL、布局是否渲染、正文长度、console 错误、API 错误码、5xx、耗时）。
- **403/403 全部跑完**：0 页布局未渲染、0 页导航异常、0 页空白页、0 个 HTTP 5xx。
- 剩余非零项只剩三类，均**不是产品代码缺陷**：
  1. **外部依赖未部署**：GoView（`/report/go-view`）、SkyWalking（`/infra/monitors/skywalking` → 502）、Swagger/Druid 控制台（`/infra/swagger`、`/infra/monitors/druid` → 404）。
  2. **沙箱代理限制**：`/infra/websocket` 的 WebSocket 走的是本地静态代理（不支持 Upgrade 转发）；后端侧已用裸握手验证为 101。
  3. **良性噪音**：导航时被取消的请求（`PAGEERROR canceled`）。
- 唯一的 API 非 0（`/wms/abc-analysis` 缺起止日期的 400）已按 §3 #14 修掉。
- 仍有一处**外观级**噪音：`/ai/mind-map` 的 markmap 在零尺寸容器里首次布局会产生 `translate(NaN,NaN)` 的 SVG 警告（主图最终渲染正常）；该页还依赖外部大模型服务，沙箱未接入，生成链路未验证。

### 4.1.1 修复后定点抽查（最终构建产物）

| 路由 | 结果 |
| --- | --- |
| `/qms/document`（质量文件，修复前 404） | 渲染正常，0 错误 |
| `/crm/statistics/product`（产品统计，修复前 404） | 渲染正常，0 错误 |
| `/wms/md`（WMS 基础数据，修复前整棵子树被顶掉） | 渲染正常，0 错误 |
| `/mes/bom`、`/mes/bom-detail`、`/mes/bom-substitute`（V217 修大小写） | 渲染正常，0 错误 |
| `/hrm/attendance/month`（修复前卡死浏览器） | 渲染正常，0 警告，接口 `code=0` |
| `/wms/abc-analysis`（修复前进页面弹 400） | 渲染正常，0 错误提示 |
| `/system/log/login-log`（修复前 401） | 渲染正常，接口 `code=0` |
| `/tms/freight/freight-report`（类型修正后的运费报表） | 渲染正常，0 错误 |

### 4.2 角色矩阵（`role-matrix.txt`）

| 角色 | 用户 | 租户 | code | 顶级菜单 | 权限 |
| --- | --- | --- | --- | --- | --- |
| 无角色 | 990000 | 1 | 0 | 0 | 0 |
| 普通角色 common | 990002 | 1 | 0 | 7 | 153 |
| CRM 管理员 crm_admin | 990003 | 1 | 0 | 1（CRM 系统） | 102 |
| 租户管理员 tenant_admin | 990109 | 121 | 0 | 3 | 132 |
| 租户管理员 tenant_admin | 990111 | 122 | 0 | 3 | 132 |
| 测试数据权限 test-dp | 990155 | 1 | 0 | 3 | 69 |
| 超级管理员 super_admin | 900100 | 9001 | 0 | 29 | 1646 |

> 角色 109/111 的租户 121/122 种子数据已过期（`2026-07-10`、`2023-04-29`）。验收时**临时**把过期时间改到未来后复测，
> 测完已恢复原值。**这不是代码缺陷**：私有化交付的引导管理员模板（`sql/mysql/zhicloud_platform_bootstrap.sql`）
> 会把管理员挂到部门 103，租户租期由客户自行设置。

### 4.3 迁移验证

- `V216_verify.sql`：5/5 PASS（Flyway 记录、误绑清理、CRM 根菜单、菜单树完整、无重复绑定）。
- `V217_verify.sql`：3/3 PASS。
- `scripts/production/check_migration_contract.py`：PASSED（tracked=127 scanned=223 findings=0）。
- `scripts/production/sql_migration_lint.py`：V216 / V217 均 PASS。

### 4.4 7 个"容器型"菜单页

`API 日志 / 审批中心 / 租户管理 / 文件管理 / OAuth 2.0 / 报表管理 / 邮箱管理` 在 `system_menu` 里是 type=2 且
`component` 为空，但都带子菜单（如"报表管理"下挂报表设计器/大屏设计器/仪表盘设计器）。前端对"带子节点的菜单"
按目录处理（父级重定向到第一个子页），因此**不是坏页**，无需修数据。

---

## 5. 仍未闭环 / 需要部署侧配合（不属于代码缺陷，但影响"能不能真跑"）

| 项 | 说明 | 责任方 |
| --- | --- | --- |
| WebSocket 必须同源 | 后端 `/infra/ws` 拒绝跨域握手（带 Origin 的握手返回 403）；前端与接口需同域，nginx 需为 `/infra/ws` 配 `Upgrade`/`Connection` 转发 | 部署（反向代理） |
| TDengine 是 IoT 前置依赖 | 启用 IoT 时序功能必须部署 TDengine；且 Druid 的全局校验语句 `SELECT 1 FROM DUAL` 对 TDengine 不生效，必须给该数据源单独配 `validation-query=SELECT SERVER_STATUS()`，否则连接池初始化失败、服务起不来 | 部署（配置） |
| GoView / 积木报表 | `/report/go-view`、`/report/jmreport/*` 依赖外部服务（`VITE_GOVIEW_URL` 等），沙箱未部署 | 部署（外部服务） |
| Swagger / Druid 控制台 | 菜单页指向文档与监控控制台路径，取决于后端 `springdoc`/Druid 是否开启与路径配置 | 部署（开关） |
| CRM 统计依赖部门 | 统计类页面按部门聚合，**用户必须归属某个部门**；演示管理员已补部门（测试数据），引导管理员模板默认部门 103 | 交付配置 |
| 租户租期 | 演示租户 121/122 已过期，验收时临时改期；交付前需与客户确认租期 | 交付配置 |
| 许可门禁 | 13 denied / 38 review_required / allowlist_hits=0，决策 `fail` 且 `blocking=true` | 法务 |

## 6. 仍然开放的生产化项（沿用上一轮结论，未因本轮业务修复而关闭）

- P1：`tenant_id` 索引缺口 60 处（性能风险，未在本轮处理）。
- P2：列类型漂移 110 处。
- G4–G8 未验证、独立复核未完成、`R-011=OPEN`、`R-029=OPEN`、**Go/No-Go = No-Go**。

---

## 7. 证据文件清单

| 文件 | 内容 |
| --- | --- |
| `ui-smoke-results.csv` | 403 个菜单页的路由 smoke 逐页结果 |
| `ui-smoke-summary.txt` | 失败项分类汇总 |
| `role-matrix.txt` | 7 个角色/用户的权限接口结果 |
| `api-smoke-results.csv` | 325 个页面的接口级 smoke 结果 |
| `menu-coverage-summary.txt` | 菜单/控制器/E2E 覆盖矩阵摘要 |
| `menu-coverage.csv` | 菜单 ↔ 前端组件 ↔ 权限 ↔ 后端端点 ↔ E2E 覆盖矩阵 |
| `verify-v216-api.txt` | 本轮 12 项关键接口复测输出 |
| `verify-v216-sql.txt` | `V216_verify.sql` 执行结果 |
| `verify-v217-sql.txt` | `V217_verify.sql` 执行结果 |
| `ws-handshake-probe.txt` | `/infra/ws` 握手探针（访问令牌 → 101） |
| `menu-component-check.txt` | 405 个菜单页 ↔ 前端视图比对结果 |
| `dict-int-bool-scan.txt` | 布尔字典 × `getIntDictOptions` 调用点全量扫描 |
| `ui-smoke-run.log` | 最终一次 403 页 smoke 的运行日志 |
