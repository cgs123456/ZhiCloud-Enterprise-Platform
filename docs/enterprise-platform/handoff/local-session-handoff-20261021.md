# HANDOFF · ZhiCloud 交接文档

> 更新：2026-10-21 13:03（第三轮）。本文已归档到仓库 `docs/enterprise-platform/handoff/`，作为本地清理前的最后交接记录。
> 口径：以下事实来自本会话实跑；推测和未验证项单独标注。生产结论仍为 **No-Go**，不得把本文当成放行证据。

---

## 0. 关键位置

| 内容 | 路径 / 值 |
|---|---|
| 项目仓库（git） | `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2` |
| 远端 | `https://github.com/cgs123456/ZhiCloud-Enterprise-Platform`（master） |
| 最新提交 | `f1a5995b8 fix(test): 对齐引导管理员口令长度校验`（= `origin/master`，GitHub Actions 全绿） |
| 本轮提交 | `dacbd3637` ERP 统计/演示总价；`149a13043` OA/TMS/QMS 列表；`f12267a1c` 证据/截图/No-Go；`0c4361c43` 微信超时记录；`a34c5acc1` README 同步；`45931a1b5` 仓库清理；`6a3693260` 裸抛门禁 82；`f1a5995b8` 测试口令长度修复 |
| 工作区根 | `C:\Users\Administrator\Desktop\zhicloud` |
| 临时脚本/日志 | `Temp\`（`zc_ablate_start.ps1`、`zc_ablate_afterfix.log`、`take_readme_shots.py`、`api_smoke.py`、`verify_smoke_fails.py`） |
| 业务证据 | `zhicloud-audit-src2\docs\enterprise-platform\evidence\production\business-pilot-data-20261021\` |
| No-Go 处置 | `zhicloud-audit-src2\docs\enterprise-platform\handoff\no-go-disposition-20261021.md` |
| 许可证复核 | `zhicloud-audit-src2\docs\enterprise-platform\evidence\production\license-gate-recheck-20261021.json` |
| 截图画廊 | `zhicloud-audit-src2\docs\screenshots.md` + `docs\images\screenshots\`（8 张） |
| 风险登记 | `zhicloud-audit-src2\docs\enterprise-platform\12-风险登记册.md`（R-011 / R-029） |

---

## 1. 当前任务与结论

本轮按顺序处理四件事：

1. **TDengine 502**：已恢复并验证启动链路，但只做了启动前 preflight，**没有根治 adapter 静默退出**。
2. **弱数据页面**：已补 MES / ERP / HRM / QMS 演示数据，8 张 README 截图已重拍。
3. **47 个 API smoke 失败**：已逐项核实，45 个是 smoke 缺父单/上下文参数，2 个是微信公众号外部 AppID 问题，**0 个真实后端缺陷**。
4. **仓库级 No-Go**：已形成可交法务/运维/DBA 的处置清单；**没有、也不能在沙箱翻绿**。

另完成仓库清理：删除无引用旧截图、前端模板 `.image/` 与构建日志；审计证据和历史 handoff 保留。

统一状态不变：

```text
production_ready=false
gate3_passed=false
independent_review=false
R-011=OPEN
R-029=OPEN
Go/No-Go=No-Go
```

---

## 2. 已完成的内容

### 2.1 TDengine 502（恢复，未根治）

- 502 链路：`taosadapter`（容器 6041 / 宿主 16041）静默退出 → 后端 `TDengineTableInitRunner` 连接失败 → `errorCode 8989` → 应用 `System.exit(1)` → 48080 不再监听 → 前端代理报 502。
- 本次修法：`Temp\zc_ablate_start.ps1` 在启动后端前检查/拉起 adapter；后端 11:20:13 `Started`（351.9s），48080 监听，启动后等待 70s 复查无 `TDengineTableInitRunner` / `errorCode 8989`。
- 仍缺：adapter 为什么静默退出（OOM / 崩溃 / 外部 kill 未证实）；没有运行时守护或独立容器。当前只是“启动时确保它在”，不是“运行中死了自动拉起”。
- 临时恢复命令（容器重建/重启后可能需要）：

```bash
docker exec -d zc-live-tdengine taosadapter -c /etc/taos/taosadapter.toml
```

### 2.2 演示数据（租户 9001，仅演示）

| 模块 | 结果 |
|---|---|
| MES | 最近 7 天 `mes_pro_feedback`（`status=4`），生产趋势每天有值 |
| ERP | 今日/昨日/本月 `erp_sale_out`、`erp_purchase_in` 演示单据 |
| HRM | 2026-10 月度考勤 `total=798`；员工 925000 的 2026-11 工资条非空 |
| QMS | `qms_audit_plan` 计划 `total=7` |

SQL 已入库：`business-pilot-data-20261021\seed_visual_data.sql`、`seed_salary_slip.sql`、`backfill_erp_total_price.sql`。
**禁止把租户 9001 的演示数据导入生产。**

### 2.3 ERP 数据/统计修复

- `demo-data/generators/generate_business_documents.py`：销售订单/出库/退货、采购订单/入库/退货 6 类单据补写 `total_price`；重新生成 schema/SQL/summary。
- 存量库执行 `backfill_erp_total_price.sql`，把 `total_price IS NULL` 回填为 `total_amount`。
- `ErpSaleStatisticsController` / `ErpPurchaseStatisticsController`：`today/month/year` 补结束上界，避免未来日期单据被计入。
- 修复后实测：

```json
/erp/sale-statistics/summary     {"todayPrice":12800.0,"yesterdayPrice":9600.0,"monthPrice":299686.65,"yearPrice":1609707.93}
/erp/purchase-statistics/summary {"todayPrice":6800.0,"yesterdayPrice":5200.0,"monthPrice":233593.83,"yearPrice":2318209.21}
```

### 2.4 API smoke（47 个失败已核实）

| 结论 | 数量 | 说明 |
|---|---:|---|
| `OK_WITH_CONTEXT` | 45 | 补父单/上下文参数后 `code=0` |
| `EXTERNAL_WECHAT_CONFIG` | 2 | `/mp/free-publish/page`、`/mp/draft/page`：微信返回 AppID 无效；本地无有效凭据 |
| `REAL_FAIL` | 0 | 无真实 500/404/非参数校验失败 |

复跑结果：

```text
pages_with_primary_list=325 pass=323 fail=0 external=2
```

明细：`business-pilot-data-20261021\smoke_fail_verification.md` / `.csv`；复跑脚本：同目录 `api_smoke.py`、`verify_smoke_fails.py`。

`external=2` 不是“通过”，只是本地无法完成微信外部调用；拿到有效 AppID/密钥后必须复测。
复跑中曾出现一次 `/mp/draft/page` 返回 `code=500`（微信 `获取accessToken超时`，`WxRuntimeException`），随后单独重试 3 次和全量复跑均恢复为 `1006007000` / `323/0/2`；这是外部超时未映射为业务错误码的健壮性问题，建议后续单独处理。

### 2.5 README 截图与 UI 修正

- 8 张截图重拍，视口 1920×1080，内容区裁剪 1706×880：
  `erp-dashboard`、`mes-dashboard`、`wms-overview`、`qms-ncr`、`crm-customer-analysis`、`hrm-employee`、`oa-reimburse`、`tms-shipment`。
- `/qms/ncr` 路由已验证可加载。
- 修正长枚举标签/技术 ID 列导致的横向挤压：
  - OA 报销：表单/表头标签缩短，操作列 140→120；
  - TMS 运单：表单/表头标签缩短；
  - QMS NCR：列表移除检验单/产品/供应商/工单 4 个技术 ID 列（详情页仍保留数据）。
- `docs/screenshots.md` 已改为 1920×1080 口径。

### 2.6 门禁与构建

本轮提交后已跑：

```bash
python scripts/check_error_codes.py                              # OK
python scripts/check_missing_preauthorize.py --include-app        # OK，534 个 Controller
python scripts/scan_tx.py                                        # OK，0 处缺 @Transactional
python scripts/check_electronic_signature.py                     # OK，8 个敏感操作全覆盖
python scripts/production/check_migration_contract.py            # PASSED tracked=128 scanned=224 findings=0
git diff --check                                                 # OK
```

前端 `pnpm build:local` 通过；后端 Maven package 在本轮控制器修改后通过。`auto-components.d.ts` 是 Vite 自动生成副作用，已还原，未纳入提交。

CI 修复（2026-10-21）：

- `6a3693260`：`Bare-Throw Exception Gate` 因 `BootstrapAdminRunner` 新增 1 处引导 CLI 豁免变成 82 > 81；复核确认与既有豁免同类后，把上限上调到 82 并写明理由。
- `f1a5995b8`：4 个 `BootstrapAdminRunnerTest` 仍使用 23–28 位测试口令，触发新加的 16 位登录上限；改为 12–16 位，并补“超长口令拒绝”用例；同步修正 `init_bootstrap_admin.py` 默认 12 字节/16 字符的过期注释。
- GitHub Actions `39543856837` 全绿：build 12m36s、frontend 3m16s、V77 48s、Windows launcher 28s；Maven `clean verify` 与 Security Check 均通过。
- 非阻断告警仍在：Node 20 / `actions/setup-java@v4` 弃用、`maven-version` 是 setup-java 的无效输入；`NVD_API_KEY` 未配置，OWASP 扫描按设计跳过。

### 2.7 No-Go 处置（未关闭）

已新增 `no-go-disposition-20261021.md`，并重跑许可证门禁：

```text
decision=fail  blocking=true
components=862  denied=13  review_required=38
runtime_blocked=38  allowlist_hits=0
```

- 51 项处置台账仍全部 `PENDING`；13 项 `deny_until_approved`（BouncyCastle ×4、jai-imageio、junrar、mysql-connector-j、Oracle ×4、mxparser、jdom2）。
- 法务/授权人必须逐项填写 `approve/reject/replace`；工程侧不得把 `deny` 改成 `permissive`，不得预填 allowlist。
- `R-011`（CRM 回款并发）和 `R-029`（BPM/Flowable 事务一致性）仍需生产等价 MySQL、物理主从/读写分离、故障注入、生产规模并发和对账证据；H2、隔离 MySQL、单机容器 GTID 近似都不能关闭。
- 其余 G0-G8（真实 Secret/TLS、K8s/部署、可观测性/SLO、合规、备份/DR、六条主流程生产验收）仍未闭环。

---

## 3. 当前环境状态（2026-10-21 13:03，项目全部关闭）

| 组件 | 状态 |
|---|---|
| 后端 | 已停止（原 pid `25020`，`48080` 不再监听）；日志保留在 `Temp\zc_ablate_afterfix.log` |
| 前端 | 已停止（原 Vite pid `44612`，`80` 不再监听） |
| MySQL | `zc-live-mysql` Exited (0)；业务库 `zhicloud_sandbox_v140` 与数据卷 `zc-live-mysql-data` 保留 |
| TDengine | `zc-live-tdengine` Exited (143)；容器保留 |
| 其他 | `zc-live-redis`、`zc-live-rabbit` Exited (0)；`zc-live-server` 早在 11 天前已 Exited (137)，本次未动；8 个 `amz-*` 容器仍 Up，未动 |

2026-10-21 13:03 已关闭前后端进程及全部 `zc-live-*` 容器；只停止，不删除容器和数据卷。`amz-*` 全程未动。

本地 mock token：`test-only-for-local900100`，tenant：`9001`。这是本地测试凭据，不是生产 Secret。

**不要删除/重建 `zc-live-*` 容器及其数据卷；不要动 `amz-*` 容器。**

---

## 4. 下一步（按优先级）

1. **许可证**：法务/授权人逐项处置 51 项；工程按 `reject/replace` 移除依赖、重新生成 inventory，再跑 `check_license_policy.py`。审批前 allowlist 保持为空。
2. **R-011 / R-029**：拿到生产等价 MySQL、物理主从/真实读写分离、故障注入窗口、生产规模数据和财务/BPM 对账口径后，按 `no-go-disposition-20261021.md` §4 执行。不要用沙箱结果替代。
3. **TDengine 根治**：给 adapter 加 healthcheck + 外部重启（autoheal/supervisor），或拆独立容器；若要改 `TDengineTableInitRunner` 的 fail-fast 语义，必须单独评审。
4. **微信外部验证**：提供有效公众号 AppID/密钥后复测 `/mp/free-publish/page`、`/mp/draft/page`。
5. **其余 G0-G8**：按 `05-生产就绪门禁清单.md` 和 `10-最终Review与交付审计.md` 推进；独立复核人未指定。

---

## 5. 恢复/复跑命令

```bash
# 容器（若已停）
docker start zc-live-mysql zc-live-redis zc-live-rabbit zc-live-tdengine
docker exec -d zc-live-tdengine taosadapter -c /etc/taos/taosadapter.toml

# 后端：启动 + 等 Started + 等 70s + 验证 48080，给足 timeout（约 6 分钟）
powershell -NoProfile -ExecutionPolicy Bypass -File Temp\zc_ablate_start.ps1 -Name afterfix
# 必须确认：48080 监听；日志无 TDengineTableInitRunner / errorCode 8989

# 前端：Vite 已在 80；若需重启，在 zhicloud-ui-admin-vue3 下执行 pnpm dev
# 截图（1920×1080，内容区 1706×880）
python Temp\take_readme_shots.py

# smoke
python Temp\api_smoke.py
```

---

## 6. 踩过的坑（更新版）

1. **`Started` / 健康 UP 不等于最终可用**：`TDengineTableInitRunner` 在 `Started` 之后执行；必须再等 70s，确认 48080 仍监听且日志无 `errorCode 8989`。
2. **TDengine adapter 会静默死**：容器 entrypoint 只在启动时拉起 `taosadapter`，死了不会自动重启；当前 preflight 只解决“启动时不在”，不解决“运行中死亡”。
3. **不要用 5191 旧静态服判断当前前端**：本轮前端是 Vite 80；旧 `ui_static_server.py` 的 5191 已不是当前入口。
4. **smoke 失败先看上下文参数**：45/47 是 detail/line/子表接口缺父单 ID；不要直接判后端缺陷。2 个微信接口是外部 AppID 问题。
5. **截图口径**：1920×1080 视口、裁剪 `(214,100,1920,980)` → 1706×880；长枚举标签/技术 ID 列会挤压表格，已按页面收敛。
6. **No-Go 不能靠文档翻绿**：许可证审批、生产等价证据、独立复核是外部输入；本文和 `no-go-disposition` 都只是处置说明。
7. **推送走代理**：直连 GitHub 可能被 reset；本次用 `git -c http.proxy=http://127.0.0.1:7897 -c https.proxy=http://127.0.0.1:7897 push origin master`，不改 git 配置。
8. **`docker stop` 默认 10s 可能把 MySQL 打成 exit 137**：用 `-t 120`，或在重启后再次 `stop -t 120` 拿到干净关闭。
9. **README 100 行红线**：加链接/空行时注意不要变成 101 行。
10. **`zc-live-*` / `amz-*` 容器不要重启/删除**；数据卷保留。
11. **新增 `@bare-throw-ignore` 必须同步 `MAX_EXEMPTIONS`**：门禁按总数卡上限，代码加了豁免但没改上限会直接 82 > 81；先复核理由再上调，不要绕过。
12. **引导管理员测试口令必须 12–16 位**：登录接口限制 4–16 位，`BootstrapAdminRunner` 已在写库前拦截超长口令；测试夹具用 23–28 位会误报 `expected: <0> but was: <1>`。
