# 单客户私有化试点 · 全菜单全角色业务验收交接（2026-10-20）

> 口径：本文只记录**业务可达性**（全菜单、全角色能否真实跑通）与本轮修复。
> **生产就绪结论没有变化**：`production_ready=false`、`gate3_passed=false`、`independent_review=false`、
> `R-011=OPEN`、`R-029=OPEN`、**Go/No-Go = No-Go**；许可门禁 `decision=fail / blocking=true` 仍需法务处置。
> 验收报告与全部证据：`docs/enterprise-platform/evidence/production/business-pilot-menu-role-20261020/`。

---

> 配套文档：**私有化交付安装与启动手册**见 `docs/enterprise-platform/handoff/pilot-deployment-20261020.md`
> （必填环境变量、反向代理要求、prod 启动阻塞的修复清单与全新安装彩排证据）。

## 1. 本轮结论（一句话）

在本地沙箱上，**403 个菜单页路由 + 7 个角色/用户组合**全部可以真实加载并拿到后端数据；
过程中定位并修复了 **14 个真实业务缺陷**（含 1 个会把浏览器标签页卡死的死循环、1 个让角色菜单为空的种子数据缺陷、
2 个 404 菜单、1 个 401 误判、1 个 500 空指针、1 个 WebSocket 握手用错令牌、1 个菜单组件大小写不匹配），
剩余未闭环项集中在**部署侧配置**与**外部依赖**，不是代码缺陷。

最终一轮 403 页 smoke：**403/403 跑完，0 页布局未渲染、0 页空白页、0 个 HTTP 5xx、0 个接口返回非 0**；
剩余 console 噪音只来自未部署的外部服务（GoView / SkyWalking / Swagger / Druid）、沙箱代理不支持 WebSocket Upgrade，
以及导航时被取消的请求。

## 2. 修复清单（按严重度）

| 级别 | 缺陷 | 涉及文件 |
| --- | --- | --- |
| 阻塞 | 月度考勤页把浏览器卡死（布尔字典用 int 解析 → key=NaN → Vue 警告风暴） | `views/hrm/attendance/month/index.vue` |
| 阻塞 | `crm_admin` 角色菜单为空（V1.1 种子只绑了一个 MES 按钮） | 新增 `V216__crm_admin_role_menu.sql` + `V216_verify.sql` |
| 高 | 登录日志页 401（axios 白名单 `includes('/login')` 误伤 `/system/login-log/page`） | `config/axios/service.ts` |
| 高 | 三个菜单点开 404（路由名重名被 vue-router 顶掉） | `utils/routerHelper.ts`、`store/modules/permission.ts` |
| 高 | MES / WMS PDA 管理后台页 404 / 501（调用了 `/app-api` 的 APP 接口） | 新增 `MesProPdaFeedbackAdminController`、`WmsPdaAdminController` |
| 高 | IM 实时消息与 WebSocket 页连不上（握手传 refreshToken，后端只认访问令牌） | `views/im/home/store/websocketStore.ts`、`views/infra/webSocket/index.vue` |
| 高 | IoT 首页统计 500（TDengine 未接入） | 部署侧接入 TDengine（沙箱已接入，见 §3） |
| 中 | MES 物料清单（BOM）菜单点开空白（component 大小写不匹配） | 新增 `V217__mes_bom_menu_component_case.sql` + `V217_verify.sql` |
| 中 | 公众号素材上传 401（el-upload 带的是 refreshToken） | `views/mp/material/components/upload.ts` |
| 中 | CRM 商机状态分页 500 | `CrmBusinessStatusController` |
| 中 | 公众号草稿/发布分页缺参 500 | `MpDraftController`、`MpFreePublishController` |
| 中 | 前端不发 `tenant-id`，后端 400 | 4 个 `.env*` 打开 `VITE_APP_TENANT_ENABLE` |
| 低 | `router.onError` 把真实导航异常二次异常掩盖 | `router/index.ts` |
| 低 | 运费报表 `summary-method` 返回数字，Element Plus 只接受字符串（类型检查报错） | `views/tms/freight/report/index.vue` |

## 3. 部署侧必须配合的事项（否则部分功能在客户环境不可用）

1. **反向代理必须把 `/infra/ws` 按 WebSocket 转发**（`Upgrade`/`Connection` 头），且前端与接口**同源**：
   后端对带 `Origin` 的跨域握手直接 403，实测同源握手返回 101。
2. **启用 IoT 时序功能必须部署 TDengine**，并给该数据源单独配 Druid 校验语句
   （`spring.datasource.dynamic.datasource.tdengine.druid.validation-query=SELECT SERVER_STATUS()`）：
   全局默认的 `SELECT 1 FROM DUAL` 对 TDengine 不生效，会导致连接池初始化失败、**服务起不来**。
3. **生产 CORS 白名单**必须包含客户域名（`CORS_ALLOWED_ORIGINS`）：同源部署由 nginx 保持 Host 即可；
   跨域部署必须在 `application-prod.yaml` 的 `zhicloud.web.cors.allowed-origins` 里显式配置。
4. **GoView / 积木报表**是外部服务，未部署时报表菜单（`/report/go-view`、`/report/jmreport/*`）不可用。
5. **Swagger / Druid 控制台**路径取决于后端开关与配置。
6. **CRM 统计类页面按部门聚合，用户必须归属某个部门**；引导管理员模板默认把管理员挂到部门 103。
7. **租户租期**由客户设置；演示租户 121/122 的种子数据已过期（验收时临时改期后已恢复）。

## 4. 仍未关闭的生产化项（与上一轮一致，本轮未触碰）

- P1：`tenant_id` 索引缺口 60 处；P2：列类型漂移 110 处。
- G4–G8 未验证；独立复核未完成；`R-011` / `R-029` 仍 OPEN。
- 许可门禁：13 denied / 38 review_required / allowlist_hits=0 → `decision=fail`、`blocking=true`。
  私有化分发本身构成"分发"，法务义务不因本地沙箱通过而消失。

## 5. 变更清单（当前工作树未提交）

后端：`CrmBusinessStatusController`、`MpDraftController`、`MpFreePublishController`、
新增 `MesProPdaFeedbackAdminController`、`WmsPdaAdminController`；
迁移：新增 `V216`、`V217`（含验证 SQL），`scripts/production/migration-contract.json` 已登记（tracked=127）。

前端：`config/axios/service.ts`、`router/index.ts`、`store/modules/permission.ts`、`utils/routerHelper.ts`、
`views/im/home/store/websocketStore.ts`、`views/infra/webSocket/index.vue`、`views/mp/material/components/upload.ts`、
`views/hrm/attendance/month/index.vue`、`.env.dev/.env.prod/.env.stage/.env.test`。

门禁：`check_migration_contract.py` PASSED（tracked=127 scanned=223 findings=0）；
`sql_migration_lint.py` V216/V217 PASS；`check_duplicate_ddl.py` PASSED；前端 `vue-tsc --noEmit` 通过；
前端生产构建通过。
