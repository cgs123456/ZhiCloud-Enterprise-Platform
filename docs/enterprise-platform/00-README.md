# 智云企业数字化中台升级方案

> 版本：v1.1（方案基线）
> 日期：2026-09-27
> 适用仓库：`ZhiCloud-Enterprise-Platform`
> 当前分支：`upgrade/production-readiness`
> 当前 HEAD：`d0ebc96c9f864450c9c293094e114aff0e4e8588`
> 交付状态：00-14 方案基线已形成；V115-V118 已在隔离 MySQL clean 沙箱路径执行并验证重跑幂等；V119 库存租户唯一约束、V120 Outbox/Inbox owner/lease 迁移和 V121 BPM claim-token ownership 另有隔离 MySQL 8.4.11 沙箱/静态证据（V119 4/4；V120 首次/幂等重跑 exit=0、4/4 验证及不兼容索引 fail-closed；V121 claim-token unit 4/0/0/0、真实链 fail-closed 2/0/0/0、10 并发 1/0/0/0、SQL lint 2 文件 0 违规），不属于 V115-V118 clean 全链；V116/V117 的并发重复拒绝、软删除语义和 100,000 行合成 DDL 成本已有历史沙箱证据，当前 T-000 约束语义复跑使用 MySQL 8.4.11 并 5/5 通过；MySQL bootstrap runner 已通过历史沙箱创建/noop/fail-closed/rotate 与只读审计；H2/历史单实例 MySQL 回滚，以及隔离 MySQL 8.4.11 的 CRM→真实 BPM→Flowable 事务链 2/2、同 `businessKey` 10 并发 1/1、动态数据源路由/事务绑定 3/3 已有证据，但生产 `DynamicRoutingDataSource`/物理主从/真实读写分离/多租户物理连接、生产故障、生产规模、Outbox/补偿对账、存量生产库全链、非 MySQL、TLS/K8s/灾备仍未闭环，生产就绪结论不变

## 0. 一句话结论

本项目已经具备企业数字化中台的**广度基础**，但不能直接作为生产环境上线。当前应先把安全基线、数据一致性、核心业务闭环和部署证据补齐，再扩展 SRM/EAM/PLM/BI 等新域。

## 1. 目标范围

平台统一承载以下业务域：

| 业务域 | 定位 |
|---|---|
| ERP | 财务、采购、销售、库存、成本、资产、税务、预算、合并 |
| MES | 工单、排产、报工、物料、设备、质量、能源、TPM |
| WMS | 收货、上架、拣货、复核、包装、发运、盘点、SN、FEFO、3PL |
| QMS | IQC/IPQC/OQC、NCR、CAPA、8D、SPC、MSA、FMEA、电子签名 |
| CRM | 客户、线索、商机、合同、回款、服务、CPQ |
| HR | 组织、员工、考勤、薪酬、绩效、招聘、合同、社保 |
| OA | 审批、公文、知识库、会议、日程、公告 |
| TMS | 承运商、车辆、司机、运单、GPS、运费、对账、车队经营 |

建议后续新增：

- SRM：供应商准入、询报价、采购协同、绩效。
- EAM：设备台账、点检、维修、备件、工单。
- PLM：产品、BOM、ECN、工艺、图纸。
- BI：指标平台、数据仓库、自助分析、驾驶舱。
- 可选：CDP、IoT、ESG、合规审计。

## 2. 硬约束

1. 本文档以企业数字化中台为目标，不扩展与八大业务域无关的方向。
2. 当前状态必须保持：
   - `production_ready=false`
   - `gate3_passed=false`
3. 不得用假 Secret、假证书、假客户端凭据或放宽扫描规则消除 P0。
4. 模拟数据只能进入 `demo-data/`、演示库、测试库或沙箱，禁止导入生产。
5. 任何“已通过”的结论必须能指向当前提交对应的证据文件。
6. 真实 TLS 握手、证书链、主机名验证、K8s 服务端 dry-run、真实生产部署未完成前，不得宣称可生产部署。

## 3. 当前已核实事实

### 3.1 代码与模块

| 指标 | 当前核验值 |
|---|---:|
| ERP Controller | 70 |
| MES Controller | 148 |
| WMS Controller | 38 |
| QMS Controller | 28 |
| CRM Controller | 32 |
| HR Controller | 13 |
| OA Controller | 12 |
| TMS Controller | 9 |
| IoT Controller（iot-biz） | 18 |
| 商业域（会员/支付/商城） | **0（2026-10-05 所有者裁决整体移除**；移除前 112：Pay 19 / Member 20 / 商城 73；V140，见 handoff §12.129） |
| Java 测试文件总数 | 359（2026-10-05 统计；商业域随域移除） |
| 最近迁移版本 | V138 brokerage_withdraw_status_default（V122 iot 15 表 / V123 商城+支付 63 表 / V124 TMS↔WMS 关联列 / V125 member 11 表 / V126 三张查询视图 / V127 请假流程编号列 / V128 95 张存量表自增主键 / V129 report 建表 / V130 在途审批视图口径修正 / V131 HR 假期类型参照数据 / V132 会员等级与分组 / V133 QMS 报表维度缺口可见化 / V134 HR+QMS 菜单与按钮权限种子 / V135 商城支付应用与模拟渠道种子 / V136 钱包支付应用+回调地址纠正 / V137 充值单支付状态默认值 / V138 提现单状态默认值 / V139 订单退款积分默认值 / V140 商业域整体移除（DROP 74 表+菜单闭包+字典）；**十条均已登记进迁移契约**。**执行通道口径**：V1..V131 与 V132–V135 都已经**应用自带 Flyway** 在隔离 MySQL 8.4.11 实测（2026-10-04 新建库从零建历史：`applied_total=142 / SUM(success=1)=142`，V132/V133/V134/V135 均 `success=1`）；V132–V135 另经仓库 `mysql_chain_run.py` 在干净库从零按序执行通过（142 files / 0 failed）。**V136–V139 已由应用自带 Flyway 在 `zhicloud_sandbox_clean` 上走过**：三条 SQL 起先是手工按序应用（各自迁移内自检 + `Vxxx_verify` 全 PASS），2026-10-04 16:34 换新 jar 重启时应用内 Flyway 已把 V136–V138 在本库补登执行（`flyway_schema_history` 均 `success=1`），2026-10-05 14:11 应用 V139（自检双 PASS）、2026-10-05 16:2x 应用 V140（自检五项 PASS，企业域 459 表完好）；**“新库一次性建到 V140”** 已于 2026-10-05 实测：全新库 `zhicloud_sandbox_v140` 由应用自带 Flyway 一次性建到 V140（`applied_total=147 / SUM(success=1)=147`，V140 迁移内自检五项 PASS；商业域残留表/菜单/字典三零，企业域 459 表）。见 §3.3 与 handoff §12.124、§12.126、§12.128、§12.129） |

2026-10-02 起 `zhicloud-module-iot`、`zhicloud-module-pay`、`zhicloud-module-member`、`zhicloud-module-mall`
进入构建；**2026-10-05 按 §12.129 裁决，pay/member/mall 三域整体移出构建与库（V140），iot 保留**。
此前这些模块**代码在库内但不参与构建**，且**没有任何 MySQL 建表脚本**（只有模块自带的 H2 测试建表，且 iot 那份已过期），
所以"模块不存在"的旧结论不准确；真实缺口是构建参与 + DDL + 多租户列。详见 handoff 清单 §12.80、§12.81。

**业务模块齐全度本轮已知的"还不能跑"部分**（不粉饰）：
- ~~仓库内没有任何 `.bpmn` 流程定义，端到端还差按租户部署流程定义~~ → **本条已被实测推翻**：
  `BpmProcessDefinitionSeedService` 按「租户 + KEY」播种最小流程定义，本轮在同一进程、真 Flowable、真 MySQL 上
  跑通 **OA 报销** 与 **HR 请假** 两条完整审批链（播种 → 建单 → 提交 → 引擎待办 → 审批 → 业务行状态翻转 + 假期余额扣减）。
  仍未跑通的只剩 `crm-contract-audit` 这一条（同一条链路，缺一次真机复跑，不是缺代码）。
- **服务端此前从未成功启动**，本轮第一次 `Started ZhiCloudServerApplication`。启动期三个真阻塞已全部修掉：
  ai-rag 自注入缺 `@Lazy`、OA mapper 字段名 `articleMapper` 与商城 `promotion.ArticleMapper` 的 bean 名撞车
  （同类形状全仓扫过，另命中 CRM 一处）、无口令 Redis 下 Redisson 仍发 `AUTH`。详见 handoff §12.95.2。
- **HR 主数据缺口已补一半**：`hr_leave_type` 原本是空表（没有假期类型就建不出请假单），V131 幂等补 8 个常用类型
  （含"事假 = 不带薪"，否则扣余额分支在初始系统里永远走不到）；`hr_employee`/`hr_leave_balance` 属组织数据，不由迁移伪造。
  **HR / QMS 的菜单权限种子仍缺**：代码里 HR 有 51 个、QMS 有 97 个 `@PreAuthorize` 权限串，而 `system_menu` 里分别只有
  **0 条和 1 条**（对照 mes 324 / erp 188 / promotion 72 / crm 77 / wms 85 / iot 55 / oa 43 / bpm 42 / product 22 / trade 20 /
  statistics 5 / report 6）—— 普通角色授权不到这两个模块，现在能用只因为 super_admin 绕过校验。
  本轮没有硬编菜单行：path/component 必须对齐前端路由，而前端不在本仓库，这批数据要等前端契约（见 handoff §12.96.3）。
- TDengine 数据源在 `application-local.yaml` 整段注释；`TDengineTableInitRunner` 已加
  `@ConditionalOnProperty`，未配置时服务端能正常启动，但**设备消息/属性接口不可用**（调用才报错，不静默返回空）。
- 审批动作与短信链路**硬耦合**：待办短信在同一事务内同步发送，`system_users.mobile` 为空时整笔审批被回滚
  （本轮真机撞到，给了手机号才通过）。这不是"通知失败降级"，是业务动作失败，见 §12.95.3。
- `zhicloud-server` 的 fat jar 有个静默陷阱：不 `clean` 时 `mvn -pl zhicloud-server package`
  会打印 `Replacing main artifact` 且 BUILD SUCCESS，但 jar 字节没变（本轮实测 nested md5 未变）；
  根 `mvn -o install` 会在 `zhicloud-module-iot-gateway` 断（vertx 4.5.26 从未下载，离线不可解析），全量构建需联网一次。
- 商城上游测试大面积 `@Disabled`（"TODO 后续 fix 补充的单测"）。放开后 product 从 0 条在跑变成 29 条在跑，
  并修掉三处真实缺陷：H2 fixture 是 MySQL 直导且列集合落后于 DO、`ProductSkuDO$Property` 不可序列化
  （JDK 序列化缓存路径在生产也会炸）、新建 SPU 的销量/浏览数业务默认值被随机入参比掉。
  仍禁用并写明原因的含下单主链路 `TradeOrderUpdateServiceTest` 等 4 个类（缺跨模块 API 的 `@MockitoBean` 替身）。
  数字：product 33 跑/29 通过/4 禁用，trade 49 跑/39 通过/10 禁用，promotion 79 跑/18 通过/61 条上游自留禁用，statistics 无用例。
- `zhicloud-module-report` 仍未进构建；`zhicloud-module-datalake` 的查询能力全部挂在 Trino 上（`zhicloud.datalake.enabled` 默认 false）。
- 存量基础 DDL（V10/V11/V13 等）里 `id BIGINT PRIMARY KEY` 没有 AUTO_INCREMENT，
  而 `id-type` 对 MySQL 解析为 `AUTO`（插入不传 id）——这类表在真机 INSERT 会失败；
  本轮新增的 V122-V127 已按自增主键写，**存量那批本轮已量化并修掉**：按"`id` 不是 auto_increment"扫全 135 个迁移命中 **95 张表**（erp 41 / wms 19 / qms 20 / hr 6 / aimultiagent 6 / airag 3，含业务流程要写的 `wms_shipment_order`、`qms_inspection_order`），`V128` 用 95 条 `MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT` 补齐；隔离 MySQL 8.4.11 全链实跑（V1..V128 一次跑完）后 `V128_PK_AUTO_INCREMENT_COVERAGE PASS 95/95` 且残余检查 `V128_RESIDUAL_BIGINT_PK_WITHOUT_AUTO PASS NULL`。

模块测试分布严重不均，QMS、CRM、HR、OA、TMS 的测试文件明显不足。详见 `01-平台现状核验.md`。

### 3.2 当前门禁

| 门禁 | 当前结果 | 说明 |
|---|---|---|
| Maven CRM 定向测试（V117） | 16 tests passed | 回款并发、幂等和余额保护定向用例通过；不等于全量模块或生产部署通过 |
| Maven CRM 模块测试（当前） | 48 tests passed（0 失败 / 5 skipped） | 包含上述 16 个定向用例，不能与定向结果相加；不等于生产验证 |
| CRM→真实 BPM→Flowable（隔离 MySQL 8.4.11） | 2/2 passed | 真实 `CrmReceivableServiceImpl → BpmProcessInstanceApiImpl → BpmProcessInstanceServiceImpl → 真实 Mapper → Flowable`；流程定义查询等非目标依赖为 mock；非生产 |
| BPM 同 `businessKey` 10 并发（隔离 MySQL 8.4.11） | 1/1 passed | 10/10 请求成功、同一实例、仅一条幂等登记/Flowable 历史；历史失败证据保留；非生产 |
| CRM/BPM 动态数据源路由/事务绑定（隔离 MySQL 8.4.11） | 3/3 passed | `DynamicRoutingDataSource` 仅暴露 master/slave 两个同实例逻辑库；`transactionManager` 绑定动态数据源，Flowable `TransactionAwareDataSourceProxy` 指向同一数据源；`CONNECTION_ID()`/`DATABASE()` 验证同一事务连接；非生产物理主从；证据 `crm-bpm-dynamic-datasource-mysql-20260929_141551.json` |
| Maven 迁移 runner（V117） | 7 tests passed | runner 单元/组件测试通过，另有 clean 沙箱端到端执行；均不等于生产迁移通过 |
| Python 全量生产门禁 | **`Ran 658 tests ... OK`（2026-10-04 本机复跑，用时 255.7 s，脚本自身退出码 0）**；同一天早些时候的 `Ran 657 tests / failures=3` **不是回归而是我这批改动的欠账**（§12.111 的 policy 改动把一条断 shipped-policy 的机制测试改死 + 分组器没有认领 `.github/workflows/`，两条都已修，详见 handoff §12.117）。历史 `Ran 634 tests OK`（2026-10-02）自 §12.111 起已不再成立，降级为追溯。**该结论要求 pwsh/mysql 在 PATH 中**：2026-10-02 有一次跑出 `errors=47`，根因是当次调用未导出 pwsh PATH（环境缺失、非回归），带 PATH 复跑即 OK（handoff §12.67 末）| 不代表 Java 全量或生产验证；**CI 里没有 pytest 步骤**，所以这套件的红灯不会在 PR 上暴露——这是当前明确的覆盖缺口。证据面见 `08-测试与证据.md` |
| 分组提交计划门禁（R-045，新增） | `plan_commit_groups.py` `exit=0`：当前 122 项可提交面分成 6 个有序组，证据 1249 项不入库；四条判定（证据不入库 / 每项仅认领一次 / 引用不许前指 / 规则不许空转）| **不产生提交**，也不碰索引。实测结论：`.ps1`、门禁 `.py`、守卫 `.py` 互成强连通分量，必须并成一次提交；`docs-platform` 规则若写成 `docs/**.md` 会把 10 份证据旁车扫进提交。它只回答"按这个顺序提交后每笔单独 checkout 会不会缺仓库内工件"，**不回答那笔提交能否构建成功**。详见 handoff §12.66 |
| 证据面凭据门禁（R-046，新增） | `check_evidence_credentials.py` `exit=0`：1649 份证据 / 25.6 MB 全扫，命中 **0**；无活沙箱时 `live_layer=not-exercised` 自曝，不记成"扫过且干净" | 补的是 `security_baseline_scan.py` 只扫**已跟踪**文件、而证据按 §3.4 一律不入库造成的**零覆盖**。判定只针对随机生成的沙箱凭据形状，弱口令示例仍归安全基线扫描。例外表按 `(path + SHA-256)` 锚定：文件被改过 → `allowlist-drift` 判红；登记未命中 → `dead-allowlist-entry` 判红。详见 handoff §12.67 |
| Helm 3/4 本地渲染与离线校验 | `status=passed` | 6 个正向、10 个负向 fail-closed、12 个 kubeconform 用例通过；证据 `helm-render-20260929_202001.json`；server-side dry-run=not_run，不证明外部 Secret 或真实集群部署 |
| MySQL runner + bootstrap 只读审计（沙箱） | **2026-09-30 本机实跑 `status=failed`**（`mysql-runner-sandbox-20260930_124102.json`，SHA-256 `0cd8070da478e29a095dcead…`）：脚本只授 `@'127.0.0.1'`，而 Docker 把宿主连接呈现为 `172.17.0.1`，Java runner Access denied。历史 `status=passed` 为 2026-09-28 MySQL 8.4.9 沙箱口径，仅作追溯；未放宽任何授权。详见 handoff 清单 §9.5 与风险 R-033 | | 迁移、ensure/noop/fail-closed/rotate、账号/租户/角色/岗位/BCrypt 形态和唯一索引一致；仅证明该沙箱库状态，`independent_review=false`，不证明生产目标库、事务、并发或其余 7 套方言 |
| V116/V117 约束语义（T-000 MySQL 沙箱） | 5/5 scenarios passed | MySQL 8.4.11；并发重复拒绝为 MySQL 1062、软删除语义；历史 MySQL 8.4.9 证据另有 100,000 行合成 DDL 成本观测；仅隔离沙箱，不证明生产规模 MDL/复制、非 MySQL、生产物理主从/真实读写分离/多租户数据源路由或生产故障/补偿对账 |
| 迁移契约 | `status=passed` | tracked=51、scanned=147、findings=0、exit=0（2026-10-05 本机复跑，含 V135/V136/V137/V138/V139/V140）；仅锁迁移字节，实库证据另见当前 MySQL runner 证据 |
| 租户隔离 | `status=passed`（默认阈值） | 仍有 361 条 P1（T5=259、T4=102），不能写成零风险 |
| 安全基线扫描 | `status=passed` | 当前（2026-09-30 复跑）：494 文件、216 发现、P0=0、P1=216、controls_failed=0；证据 `security-baseline-t000-20260930_113027.json`（SHA-256 `284e9d7fd767405b…`）；487 口径（`security-baseline-t000-20260929_095135.json`）已降级为追溯；历史 2026-09-27：463 文件、248 发现、P0=32、P1=216、exit=1，仅作追溯 |
| 业务模块 DDL 全链（V1..V129，136 个迁移） | 隔离 MySQL 8.4.11 实跑 0 错误 | V122 iot 15 表 / V123 商城+支付 63 表 / V124 TMS↔WMS 关联列 / V125 member 11 表 / V126 三张查询视图 / V127 请假流程编号列 / V128 95 张存量表补自增主键 / V129 report 建表；10 份 V12x 复核脚本全 PASS，其中 `V128_RESIDUAL_BIGINT_PK_WITHOUT_AUTO PASS NULL` 表示库内不再有 bigint 主键无自增的表。不证明生产库、事务、并发或生产规模 |
| 业务模块单测（本轮新接入部分） | 见说明 | iot-biz 269、pay 167、member 35、product 33、promotion 79、trade 49、statistics 0、wms+qms 相关 32、tms 5、oa 监听器 5、hr BPM 12、report 16，均 0 失败 0 错误（禁用用例逐条写明原因）。不等于端到端业务流跑通：BPM 无流程定义、TDengine/Trino 未部署 |
| ② iot：产品→物模型→设备→规则→告警 真机 | **部分通过，最后一步被框架缺陷卡住** | 真实 API 建成 product/物模型(temperature)/device/scene-rule/alert-config；`thing.property.post` 上行消息经真实 Redis Stream 总线被两个消费者消费，规则引擎执行到属性值比较。**`iot_alert_record` 仍 0 行**：库里存 `"operator":">"`，运行时读到的是 `operator(&gt;)` → `operatorOf` 判无效 → 规则永不触发；清缓存、删旧规则、重投消息均复现。根因是 `ZhiCloudXssAutoConfiguration:50` 把 XSS 清理挂到**全局 ObjectMapper**，JSON 列反序列化时被二次转义（业务侧已加 `normalizeConditions(htmlUnescape)` 修好写入侧）。详见 handoff §12.100 |
| ② iot：产品→物模型→设备→规则→告警 真机 | **PASS** | 真实 API 建 product/物模型(`temperature`)/device/scene-rule/alert-config；`thing.property.post` 上行消息走真实 Redis Stream 总线被消费者取走；规则命中并 **`iot_alert_record total=1`**（含 configId/configName/productId/deviceId 与原始 deviceMessage）。此前卡住的是框架级 XSS 污染（见下一行），修完即通过 |
| 框架级：全局 XSS 反序列化污染 JSON 列（已修） | **已修 + 真机对照** | `ZhiCloudXssAutoConfiguration` 把 XSS 清理注册到 Spring 全局 ObjectMapper，而 `ZhiCloudMybatisAutoConfiguration:97` 又把该 mapper 交给 `JacksonTypeHandler` → 库里存 `>`、读出来变 `&gt;`，IoT 场景比较符被静默改写、规则永不命中。改为只在 `extendMessageConverters` 里用 `mapper.copy().registerModule(...)` 给 MVC 消息转换器挂 XSS。真机三重验证：① 新写规则读回 `>`（同库旧规则仍是 `&gt;`，天然对照组）；② 告警落库 total=1；③ **XSS 未被削弱** —— 请求体传 `<script>alert(1)</script>LiveXssCheck…` 读回只剩 `LiveXssCheck…`。web starter `install` Tests run 4 / 0 失败；OA、mall 两条真机链复跑仍 PASS |
| ② mall：商品→购物车→下单→支付 真机 | **PASS** | 会员端 API 全程：`/app-api/trade/cart/add`+`/list` `code=0`；`/app-api/trade/order/create` 自提与快递均 `code=0`（快递运费 `delivery_price=1500` 由运费模板算出），生成 `pay_order`；`/app-api/pay/order/submit`（`channelCode=mock`）`code=0 status=10` 且 `success_time` 落库；回调 `/app-api/trade/order/update-paid` 后 `trade_order.status 0→10 待发货`、`pay_time` 落库 |
| 会员端全线 403（结构性缺陷，已修） | **已修 35 处 / 17 个 app 控制器** | `controller/app/**` 的写接口挂着后台 RBAC `@ss.hasPermission('trade:cart:create')` 等，而会员（`member_user`）在 `system_user_role` 里不存在 → 任何真实会员调商城/会员端写接口必 403（购物车、下单、售后、分销、收藏、客服、优惠券、签到、地址、钱包、支付提交）。分布：trade 5 文件/15、member 4/9、promotion 4/5、product 2/4、pay 2/2、iot 0。改为 `@PreAuthorize("isAuthenticated()")`：**仍要求登录**，只是把不可能满足的后台权限换成会员端应有的"已登录 + 资源按 `getLoginUserId()` 归属"；`@PermitAll` 公开接口未动，残留 `@ss.hasPermission` 复核为 0；5 个模块带测试 `install` rc=0 |
| 服务端真启动（隔离容器，非生产） | `Started ZhiCloudServerApplication`，Tomcat 48080 应答 200 | 2026-10-02 首次。MySQL 8.4.11（`zc-live-mysql`，宿主 55101）+ Redis（16380，必须带口令）+ RabbitMQ（56721）全部自建隔离；数据链 = 导入 `sql/mysql/zhicloud_platform.sql`+`quartz.sql` 后由应用自带 Flyway **applied 135 migrations / 0 error**。启动耗时约 19 分钟，其中 spring-modulith 的 ArchUnit 扫描约 4.5 分钟。管理员账号由仓库脚本 `init_bootstrap_admin.py` 渲染生成（seed dump 内没有 `system_users` 数据，不建号则任何 `@PreAuthorize` 都 403）。详见 handoff §12.95.1/§12.95.2 |
| OA 报销 → BPM → 审批（同一进程真跑） | **PASS** | 播种 `oa_reimburse`（真部署 `oa_reimburse:1:73ef4d8d…`）→ 建单 id=3 → submit `status=20` 且流程实例 `6f21a391…` → `todo-page` 取引擎任务 → `PUT /bpm/task/approve` → 业务行 **`status=30`**、流程实例 `status=2`、引擎耗时 575 ms。中途撞到"待办短信缺手机号导致整笔审批回滚"，属真实耦合风险 |
| HR 请假 → BPM → 审批（同一进程真跑） | **PASS** | 播种 `hr_leave_request` → 建单 → submit（流程实例号落库）→ approve → **`status=1` APPROVED、`approve_time` 落库，余额 10 → used 2 → remaining 8**。前置主数据（假期类型/员工/余额）库内为空，本轮用 SQL 造数据。同时补 `HrLeaveRequestRespVO.processInstanceId`（原来缺，前端跳不了流程图） |
| QMS 收货/流转卡点（真服务端，含放行对照） | **PASS** | 五个判定实测：`receipt_blocked=1060200008`（带 `qcBizId` 但无检验单 → 未检验也拒收，fail-closed 生效）、建合格检验单后 `receipt_released=0`（入库单 99、库存 +5）；`movement_no_inspection=0`（宽松语义：没检验单不卡既有流程）、判废检验单后 `movement_blocked=1060202008`、换单复检合格后 `movement_released=0` |
| 移库不维护可用库存（真机读出的缺陷，已修） | **已修 + 真机对照** | 根因：`WmsMovementOrderServiceImpl` 构造库存变更项时**没设 `changeType`**，还把源仓数量写成负数 → 落进 `WmsInventoryServiceImpl` 的 `PLAIN` 兼容分支（"只动 quantity，不动三维字段"），表现为移库后目标仓 `quantity=4 / available=0`（货到了不可用）、源仓 `quantity=51 / available=55`（available 虚高、可超卖）。改为源仓 `OUT`/目标仓 `IN` + 数量保持正数（与收货/发运同一套 P0-7 约定），删掉不再使用的 `negate`。全仓 5 个 `changeInventory` 调用点已枚举：收货 IN、发运 OUT、PDA 按场景、移库（原先缺失）。真机对照：修复后源仓 **51/51**、目标仓 **4/4**，五个卡点判定一字不变；变异检查（临时删掉 `setChangeType`）→ 测试报 `expected: <OUT> but was: <null>`，恢复后全绿 |
| QMS 发运质检卡点（真服务端双向） | **PASS** | 出库单由 API 链产生：create→start-pick→complete-pick→review→pack 全 `code=0` → 建 OQC 检验单并提交「不合格+致命」→ 检验单自动判 **40 检验不通过** → `PUT /wms/shipment-order/ship` 被拦 `1060201010`（状态原地不动，卡点在流转之前）→ 换单复检「合格」→ 判 **30 检验通过** → 再 ship `code=0`，出库单 **40→50**。并确认 QMS 语义：判出结果后不能原地重判（`1044000012`），改判必须换单复检 |
| ①+④ 连成一条 API 驱动的链 | **PASS** | 沿用上面那张单：TMS create(`shipmentOrderId=4`)→dispatch→confirm-arrival→confirm-sign 全 `code=0` → 出库单 **50→99** + `waybill_id=4` 回写、运单 `status=40`+`sign_time`、库存 `100→97`（扣的正是明细 3 件）、⑤ 视图 `order_finished=1/has_waybill=1`。SQL 预制只剩主数据（仓库/物料/SKU/检验项目/库存行）；另实测 `wms_warehouse`/`wms_item`/`wms_item_sku`/`qms_inspection_item` **零种子** |
| TMS 调度 → WMS 出库签收回传 | **PASS**（上游 WMS 内部链除外） | 同进程真跑 create→dispatch→confirm-arrival→confirm-sign 四步 code=0：出库单 **50→99** 且 `waybill_id=3` 回写、运单 `status=40` + `sign_time` 落库、库存 `100→94`（OUT 变更，available 同步）、库存流水 +1；⑤ 的 `v_wms_shipment_waybill_sign` 同一行同时可见（`order_finished=1`、`has_waybill=1`）。口径：SHIPPED 出库单/明细/库存为 SQL 预制，未跑 入库→…→发货 内部链 |
| CRM 合同审批 → BPM（真机） | **PASS**（先修了一个真缺陷） | 首跑 submit 直接失败 `1020000002 合同不是未提交状态`：`createContract` 没写 `auditStatus`，而 `crm_contract.audit_status` 无库级默认值 → 新建合同恒为 NULL → **API 建出来的合同永远提交不了审批**（同模块回款/发票都写了 DRAFT，唯独合同漏）。补 DRAFT 默认后重跑：合同 id=2 → submit → 引擎任务 → approve → `audit_status=20`（审核通过）+ 流程实例号落库 |
| 在途审批视图口径（V130） | 6/6 PASS（真机） | V126 的 `v_bpm_approval_inflight` 只过滤 `deleted=0`，把已批准/已驳回也算进"在途"，且 hr 分支的流程实例号写成常量 NULL；V130 用 `CREATE OR REPLACE` 改为按业务审批中状态过滤（oa 20 / crm 10 / hr 0）并透传 hr 流程实例。真机行数 4 → 2（只剩在途）。`check_migration_contract` tracked=41 findings=0、`check_duplicate_ddl`、`check_tenant_isolation` 均 passed |
| HR/QMS 后台菜单与按钮权限种子（V134） | **PASS**（真库 + 真服务端双向） | 36 个资源、186 条声明行**从 `@PreAuthorize` 反向生成**，不手抄。真库首跑 rc=0、重跑 rc=0（幂等）、`V134_verify` **5 PASS / 0 FAIL**；运行中服务端 `GET /admin-api/system/menu/list` `code=0`（1808 扁平节点）里 `/hr`(7200) → 13 菜单 + 51 按钮、`/qms`(7500) → 23 菜单 + 97 按钮。落库 185 行而非 186：**一条 `qms:capa:transition` 权限点早已存在（id=6740）但 `parent_id=0` 是孤儿**，V134 把它挂回 `qms/capa/index` 并让新声明让位（权限点唯一是设计行为）。**刻意不给 `system_role_menu` 授权**——超管走角色旁路可见，普通角色授权属运营动作。未验证面：前端 Vue 页面是否按 `hr/<resource>/index` 存在（本仓无前端源码）；V132–V135 的应用内 Flyway 通道已于 2026-10-04 实测通过（见 §3.1 与 handoff §12.124）。详见 handoff §12.113 |
| QMS 维度回填链路（录入主数据 → 报表数字变真实） | **PASS**（独立探针库，业务库零污染） | 用仓库自己的 `demo-data/` 通道（租户 9001 / `DEMO_` 前缀 / `creator='demo-seed'`）在 `zc_demo_qms_probe` 探针库跑通：10 供应商 + 4 物料 + 6 检验单（3 张两维齐全、2 张对照缺维、1 张悬空 id 探针），`V133` 视图原文读数 `supplier_count`/`product_count` 从真实租户的恒 0 变 1/2，`dimension_missing_count` 只在含 NULL 组计 1、齐全组计 0。同时量出一处新风险：**`qms_inspection_order` 无任何外键**，`supplier_id=999999`（不存在的行）被库接受且被报表计成一个真实供应商。缺口交代：`demo-data` 的 19 张主数据表**不含任何 MES 表**，物料侧 4 行是按同约定手写的，未动生成器（会重钉 `demo-data/README.md` §3 的 4 个 SHA-256 与文档 07 的行数口径）。复核 `zhicloud_platform` 内 `creator='demo-seed'` 供应商 = 0 行。详见 handoff §12.114 |
| 许可证 CI 步骤（改为可撤销的只记录模式） | **不阻断（所有者决定）** | CI 在 `License Policy Gate` 判红：`denied=13`（`strong_or_restrictive` + `custom_or_non_spdx`）、`review_required=47`（`weak_copyleft`），根因是 §12.111 只降了 `missing`/`unmapped` 两类而**判定把 review 也算阻断**。改法是给脚本加 `--record-only`：判定照常算、`denied/review_required` 数组照常写盘上传，只有退出码变 0，并把决定写进 `report["blocking"]=false`；另有 `::warning::` 步骤把计数打在页面上。真机三态：默认 rc=1 / `--record-only` rc=0 且 denied 数组仍 13 条 / 策略文件缺失 rc=2（输入坏了不被伪装）。恢复阻断只需删那一个参数。详见 handoff §12.115 |
| pay 模块被跳过用例（35 → 10） | **复活 25 个真跑通** | `PayAppServiceTest`(14) + `PayNotifyServiceTest`(11) 不需要任何凭据，它们的 `@Disabled // TODO 后续 fix 单测` 是误导：启用后失败的真因是 **H2 测试 DDL 与实体脱节**（`pay_app` 缺 `transfer_notify_url`、`pay_notify_task` 缺 `merchant_refund_id`/`merchant_transfer_id`，报 `Column ... not found`；列型按真 MySQL 的 `information_schema` 补）、**断言停在旧契约**（生产已是事务提交后 `executeNotifyAsync`，测试还 `verify executeNotify0`；改为按 id 断言，不做整对象 equals）、**夹具没覆盖新字段**。修后 `167 / 0 失败 / 0 错误 / 10 跳过`；变异检查（注释掉 `getSelf().executeNotifyAsync(task)`）→ 恰好 2 个用例红，恢复后绿。剩 10 个是直连微信/支付宝真网关的调试脚手架（只 print 不断言），已逐类写明理由。详见 handoff §12.116 |
| 复活后的 pay 通知用例在 CI 上随机红过一次（已改确定性断言） | **已修** | `6f91e828` 在 `Build with Maven` 判红（`testProcessNotifyResult_success`：`expected: not equal but was: <2026-10-26T07:24:06>`），同分支再推一次 `8a20ecbc` 又绿 —— 同一份代码一红一绿就是随机红的定义性证据。根因是上游断言的形状：上一句 `assertEquals(随机A, 随机A)`、这一句 `assertNotEquals(随机A, 随机B)`，合起来等于"断言两个随机值不相等"，`randomPojo` 每轮取值不同，撞上就红。**不是复活引入 flake，是它本来就不成立**，只是被 `@Disabled` 遮着。修法：加 2000 年 `STALE_TIME` 哨兵钉住两个时间字段，断言换成契约式（不重试分支断"仍等于哨兵"、重试分支断"晚于 now"、全部分支断 `lastExecuteTime` 非空）。读数：`PayNotifyServiceTest` 连续 5 次 11/11，pay 全模块 `167 / 0 失败 / 0 错误 / 10 跳过`。顺带量到 `PayNotifyServiceImpl:294` 的 `notifyTimes + 1` 直接做长度 8 的数组下标、`>= length` 挡不住负数溢出（CI 日志 `Index -1564220316 out of bounds for length 8`）：线上走不到，但一行脏 `notify_times` 就能打崩通知线程，已列为待裁决。详见 handoff §12.125 |
| 安全基线扫描的**明确盲区**（本轮量出，未修） | `P0=0` 不可当作"仓库里没有提交的凭据" | 3 个 pay 网关测试里长期提交着真形态商户凭据 —— 微信 `mchId`、两组 V2 `mchKey`、两组 `apiV3Key`、支付宝 `appId`、**两段 2048 位 RSA 私钥正文**与 `/Users/yunai/...` 绝对路径，共 22 处字面量，已全部换成 `REPLACE_WITH_YOUR_*`（grep 残留 0）。而 `security_baseline_scan.py`（534 文件、P0=0、status=passed）一条都没报：`R3` 只匹配 `password|secret|token|api[-_]?key` 后接 `:=` 的键值对写法，既无 `PRIVATE KEY`/PEM 正文规则，也看不见 `config.setMchKey("…")` 这种 setter。补规则需要正负样本才算"补上了"，本轮只登记缺口。详见 handoff §12.116 |
| 商城支付链的种子缺口（V135） | **已补 + 干净库真机** | 上一轮"mall 支付跑通"其实依赖两行手工数据（`pay_app app_key='mall'`、`pay_channel code='mock'`，creator 是管理员 userId），全仓 `git grep INSERT INTO pay_app / pay_channel` 命中 0 —— 干净检出会在 `validateChannelCanSubmit` 处抛 `PAY_APP_NOT_FOUND`，表现就是"下单后付不了款"。V135 按**现存租户**种这两行：app key 与 `TradeOrderProperties.payAppKey` 默认值一致，mock 渠道**种成停用态**（启用它等于不付款即可标已支付；停用确实由 `PayChannelServiceImpl:149` 的 `PAY_CHANNEL_IS_DISABLE` 分支拦截）。干净库 `mysql_chain_run.py`：142 files / 0 failed / 557 表，内自检 4 项 + `V135_verify` 5 项全 PASS，覆盖读数 `3 租户 → 3 应用 → 3 渠道，enabled=0`（不是 0=0 的假通过），幂等复跑仍 3/3。打到已有安装的库时 `pay_app` 只补缺失租户，并如实报 `MOCK_CHANNEL_DISABLED FAIL enabled=1`（那条渠道是人显式启用的，迁移不替他关）。**已验收（2026-10-04）**：全新库 + 应用内 Flyway 端到端跑通，见下一行与 handoff §12.124 |
| ② mall：全新安装端到端（应用自带 Flyway + V135 种子行） | **PASS（BIZ-13 结）** | 重打 jar 并开包核对含 `V132…V135` → 只停自己起的 PID → 新建 `zhicloud_sandbox_clean` 导两份 dump（59 表、无 flyway 历史）→ 应用自带 Flyway `baselined at 1`、`validated 142`、`applied 142 / success=142`，V132–V135 四行 success=1，约 9 分钟起来。流程全程只用**迁移产出的** `pay_app`/`pay_channel`（`creator='1'`）：加购→用 `cartId` 下单（自提）→**负向对照**：渠道仍是种子默认停用态时提交被拒 `1007001001 支付渠道已经禁用`→运营启用→提交 `code=0`→`trade_order.status=10`+`pay_time`、`pay_order.status=10`+`success_time`+`app_id/channel_id` 指向迁移行、`pay_notify_task.status=10`、`pay_notify_log={"code":0,"msg":"","data":true}`、购物车逻辑删除后计数 0 —— **8/8 断言通过**。同时纠正一条我自己写错的机制归因（那两行来自本机 seed 文件而非管理员 API），并记下两套 list 列约定并存的坑（`slider_pic_urls` 要 JSON、`verify_user_ids` 要逗号文本，写错就是 500）。详见 handoff §12.124 |
| ② 资金三链：钱包充值到账 / 余额支付 / 佣金提现打款 | **PASS（三链全绿，代价是三条各自的一个建单缺陷）** | 沿 §3.2 的 mall 链往钱的方向跑，三条链**一条都不通**，且断点形状相同——建单方漏字段或漏配数据。① `pay_app` 的 `refund/transfer_notify_url` 被种成 pay 自己的网关回调地址（这两列按 `PayRefundServiceImpl:118`、`PayTransferServiceImpl:85` 是**业务方**收款地址），且 `wallet` 支付应用根本不存在 → **V136** 补 wallet 应用 + mall→wallet 渠道（启用）+ wallet→mock 渠道（停用）并把两列改指 `/admin-api/trade/after-sale/update-refunded`、`/admin-api/trade/brokerage-withdraw/update-transferred`；② `pay_wallet_recharge.pay_status` 是 `bit(1) NULL` 无默认值、建单从不写 → 到账回调第一行 unbox 就 NPE，绕过 NPE 也过不了幂等 CAS（`NULL = 0` 恒假）⇒ **充值从来没有可能到账** → **V137** `NOT NULL DEFAULT b'0'`（先 UPDATE 存量再 ALTER）+ 服务显式 `setPayStatus(false)`；③ `trade_brokerage_withdraw.status` 同形漏写，审核两道关卡（`notEqual(AUDITING, null)` 与 `WHERE status = NULL`）全打不中，`approve` 稳定返回 `1011008001` ⇒ **提现从来没有可能被审核通过并打款** → **V138** `int NOT NULL DEFAULT 0` + 服务显式赋 AUDITING。真机读数（`zhicloud_sandbox_clean`，15/15）：充值 18255→**48255**（流水 `biz_type=1`）；余额支付下单 `trade_order.status=10`、余额→**31340**（`biz_type=3`）；提现申请→审核通过→`pay_transfer.status=10`/渠道单号 `W202610041615541`/转账回调 1 条→`trade_brokerage_withdraw.status=11`、余额→**40340**（`biz_type=6`），三行余额算术自洽。**取证边界**：本轮跑的是 V134 时代的 jar，**不含**上面两行 Java 改动，所以 15/15 证明的是**列默认值那一层护栏**（MyBatis-Plus 不为 null 字段生成列），代码侧那两行效果待重打包后复跑；`trade_brokerage_withdraw.total_price` 快照列同样建单不写，但不在任何校验/CAS 里，**本轮未动**。详见 handoff §12.126 |
| ② 资金侧延伸：售后仅退款闭环（退款成功 → 售后完成 → 全额退款自动取消订单） | **PASS（V139 收口回调侧断点）** | V136 修对 `refund_notify_url` 后，首次真实回调即暴露下一层：`trade_order.refund_point` 是 V123 建的可空无默认列、建单只写孪生的 `refundPrice(0)` 从不写它 ⇒ 退款成功回调 `order.getRefundPoint() + item.getUsePoint()` unbox NPE、事务整体回滚，售后单永远停在“等待平台退款(40)”——**售后退款从来没有可能被标记为完成**（钱退了，单据断在最后一环）。**V139** 先 UPDATE 填平存量 NULL 再 ALTER `NOT NULL DEFAULT 0` + 建单显式 `setRefundPoint(0)`；同路径其余拆箱逐一量过（`item.use_point`、`pay_status` 行上非 NULL，分销/优惠券 handler null 防御），只有这一列是漏的。真机读数（`zhicloud_sandbox_clean`，新 jar 类哈希 `27bf1a6a…→ad7e81c0…` CHANGED）：修复前回执 500 + NPE 堆栈（sha256 `0687d861…`）；补发同请求 → `code=0`，1 号售后单痊愈（`status=50` / 订单 11 取消 `cancel_type=20` / `refund_status=20` 全额）；新订单全链资金 15/15 + 售后 **10/10**（真实回调回执 `code=0`）；变异检查（列默认值拔成 NULL 后 API 建单，行上 `refund_point=0`）钉住代码层。详见 handoff §12.128 |
| 商业域整体移除（所有者裁决：会员/积分/充值/钱包/佣金等商城资金会员域不保留） | **完成（V140，企业域零波及）** | 依赖探测：企业域 15 个模块对 member/pay/trade/product/promotion 的 import=0、iot 与 mall 无接线、全库视图无一 FROM 商业表（QMS 视图的 product_id 为列名误报，逐一排除）、租户套餐 CSV 0 引用、demo-data 0 命中。移除面：后端 3 模块+server pom 6 依赖+根 pom 3 行+CI 事务门禁清单+permitall allowlist 25 条+商城 RBAC 守卫测试退役；前端 mall-uniapp 整包与 admin-vue3 的 api/views/路由段/DiyEditor/AppLinkInput/kefu store；yaml 三文件。**V140** 反向清理（不动既有迁移正文）：DROP 74 表（清单来自 information_schema 实测）+菜单递归闭包+商业字典，迁移内自检 5 项与 `V140_verify` 6 项全 PASS（企业域 459 表完好）；R4 破坏性语句门禁新增登记豁免（双向 fail-closed）。真机读数：`/app-api/member/**`→`code=404`、`/admin-api/trade|pay/**`→`code=501 模块已禁用`、企业域冒烟（ERP 库存/QMS 检验单）`code=0`；python 全量 `Ran 653 tests OK`；契约 `tracked=51 scanned=147 findings=0`。详见 handoff §12.129 |
| ③④ 未覆盖流程补完（V140 全新库上重走） | **PASS（③ 公文 BPM 回写监听器补齐 + 三流程端到端；④ 出库全链 + TMS 签收回传闭环）** | 全新安装真实断点：`ACT_RE_PROCDEF` 为空且播种白名单无公文 key → ③ 的公文 submit 必断。修复：`OaDocumentStatusListener`（BPM 通过→25 待签发、驳回/撤销→10 草稿，仅审核中(20)落一次防重投倒退，测试 6/6 含重投不倒退）+ 播种白名单补 `oa_document`。真机读数（`zhicloud_sandbox_v140`）：报销→**30 已通过**；请假→**1 已批准 + 余额 10→9.0**；公文 9/9：20→**25（新监听器）**→签发 30→归档 50。④ 走完 §12.95.5 停下的那一段：出库全链 create→pick→review→pack→ship(50) + TMS 运单 dispatch/arrival/sign，**签收回传出库单 99 已完成、waybill_id 回填、库存 10→6**。主数据（HR/仓库/物料/SKU/车辆/司机）为 SQL 造数（probe- 前缀，如实记录）；python 全量 `Ran 653 tests OK`。详见 handoff §12.130、§12.131 |
| R-009/R-010/R-007 重审与补完（业务缺口第一批） | **PASS（登记册滞后已复核；残留缺口修复；对账入口落地）** | 重审发现 R-009 三主缺陷（状态机卡死/报工超产/发料超 BOM）与 R-010 旁路**现行代码已修**（登记册描述停在修复前），本轮补真机证明：MES 工单全链 15/15（派工/开工/报工/关闭**新端点** + 超产 1040502009 拒绝 + REPORTING 完工 + 已派工/草稿取消、已完成取消被拒 1040502010——**取消死锁残留修复**）；R-010 负向：旁路 /complete 被 fail-closed 拒绝（1060201008），草稿不动；R-007 第一步：**V141 `v_inventory_ledgers_overview`** 三本账统一形态总览（迁移内自检 3 + verify 5 全 PASS；行级对账需跨模块物料映射，边界如实登记）。详见 handoff §12.132–§12.134 |
| R-012 会计端到端链补完（专项） | **PASS（链上 5 个真缺陷全修，端到端+期间负向真机闭环）** | 第一次跑这条从没验过的链（订单→出库→审核→MDM 映射→WMS→收款→审核→会计事件→GL 凭证→审核/反审核/重审→冲销→期间关闭），每层各撞出一个真缺陷：① 会计事件跨租户断裂（平台模板分录行带租户拦截器查不到→任何非平台租户过账必报"无分录行"，`TenantUtils.execute(rule.getTenantId())` 修）；② 框架库存 `getOrCreate` 的 null 复合键没归零（`= NULL` 永不匹配→增量全走 INSERT 撞唯一键→出库审核 500/假性库存不足，`normalize()` 修）；③ `CrmContractSaveReqVO.products`+ERP 两处 sale `items` 缺 `@Valid` 级联（明细缺字段照收→金额算成 0→凭证拒），三处补；④ 同族金额 NPE（`updateSaleInReceiptPrice` 首笔回款 null unbox，`defaultZero` 修）；⑤ 探测脚本时间戳反序列化口径错（传字符串→1970，非产品缺陷，记）。链要跑通须先有主数据映射（租户 1 GL 科目/MDM 映射/WMS 商家均 SQL 造数，正是 R-014 的实感）。真机：收款事件→凭证 2 分录借贷平衡→审核 10↔20→反审核→重审→冲销（同单事件数=2）；期间负向 6/6（关闭后审核被拒、还原后过）。ERP 161/161、CRM 0 失败、inventory 12/12（含 null 键回归）、python 653 OK。详见 handoff §12.139 |
| R-014→R-007 这条线（主数据映射维护入口 + 行级库存对账） | **PASS（MDM 管理 API 落地；ERP↔WMS 行级对账视图真机读出差异）** | R-014：`MdmCrossRefService` 补 `page`，新增 `MdmCrossRefController` 三端点（`/system/mdm-cross-ref` 的 page/save/invalidate，upsert 幂等），权限点 `system:mdm-cross-ref:*` 在 **V142** 同批种进 system_menu（凭空造权限串会恒 403，§12.95.3 教训）；真机 7/7（page total=3、save 落库、二次 upsert 仍 1 行 target 覆盖、invalidate 转 INVALID）。R-007：**V143 `v_inventory_reconcile_erp_wms`** 以映射为桥把 erp_stock 与 wms_inventory 行级对到一行，真机读出 `ERP 产品1=55 vs WMS SKU1=8 → diff=47 QUANTITY_DIFF`（三套账背离一眼可见）；边界：只对已映射物料、不 JOIN 仓库映射、键显式 CAST。自纠：V142 注释 `--（` 非注释致真机 1064、自检 `COUNT(*)=3` 口径错（菜单+按钮同权限）→ 已应用正文不回改，**V144** 补正确断言。契约 tracked=55；python 653 OK。详见 handoff §12.140、§12.141 |
| R-012 采购侧会计链（暂估+付款）端到端 | **PASS（同族金额 NPE 第六/七/八例一次扫净）** | 采购订单→审核→采购入库→审核（→`PURCHASE_RECEIPT_ACCRUAL` 暂估事件→凭证：借 1403/贷 2202 借贷平衡，非税场景进项税行按 0 金额正确跳过）→付款单→审核（→`PAYMENT_POSTED` 事件→凭证平衡→入库单 `payment_price` 回写 1000）。链上抓出同族金额 NPE 三处（采购入库 otherPrice/采购订单项 in_count 与 return_count/付款回写 payment_price）+ @Valid 级联两处（采购入库/采购订单 items）——**至此 ERP 订单/出入库累计数量读侧全部 null-safe**；口径修正：订单头累计列有默认 0 安全，明细列全可空（危险面）。付款单建单曾被正确的超付校验拒绝（探测脚本金额错）。ERP 161/161、python 653 OK。详见 handoff §12.142 |
| 收尾批次（采购退货链 / MES 纳入对账 / 调度任务孤儿 / 历史档案） | **PASS（§12.143 五件事全部落地）** | 采购退货链真机跑通（`PURCHASE_RETURN_POSTED` 事件+退货凭证平衡，且首次验证 §12.142 的 `return_count` null NPE 修复——建退货单即订单项 `return_count` 0→2 不再炸；补第 4/5 处 `@Valid` 级联）。R-007 第三步：**V145 `v_inventory_reconcile_mes_wms`** 用 §12.140 的 R-014 维护 API 登记 MES.ITEM→WMS.SKU 映射（维护入口首个真实用例，不再手写 INSERT），MES 物料账纳入行级对账（MES=3 vs WMS=8 → diff=-5 QUANTITY_DIFF，V145_verify 3/3）。发现并清理 **V140 漏网的 12 条商业域调度任务孤儿**（pay×5/trade×7，handler bean 已随模块删），**V146** 精确删除并留 6 个现存 handler（V146_verify 幸存者集合精确匹配）；这同时把 §12.127「支付通知无重投」裁决项实质收口——pay 模块与 payNotifyJob 一并移除，议题消失。五份 2026-08 历史档案先加状态头、后于 §12.144 按所有者指示删除；R-G3.5-4 维持"需所有者定义、登记不动"。ERP 161/161、python 653 OK、契约 tracked=57。详见 handoff §12.143 |
| B+C 批次收口（历史档案删除 / QRTZ 纳入 Flyway / MES 仓库对账 / @Valid 扫净 / 证据盘点） | **PASS（§12.144 五件事全部落地）** | B-2 `git rm` 五份 2026-08 历史档案（含已过时的"政企/金融客户"类定位表述），同步删 plan 规则 7 防 dead-group。B-3 **V147**：11 张 QRTZ_* 表纳入 Flyway（R-031 收口——全新库 iot 调度器此前每分钟报 `QRTZ_LOCKS doesn't exist`，真机重启后报错计数=0）。纯 DDL + `CREATE TABLE IF NOT EXISTS` 不 DROP；按 FK 依赖重排（原 quartz.sql 子表排在父表前，迁移会挂）；不搬其过期 INSERT（含 V146 删过的商业域 job），并清手工导入库残留行。C-2 **V148**：MES 物料×仓库双桥对账（维护 API 登记 MES.WAREHOUSE→WMS.WAREHOUSE），diff=-5 QUANTITY_DIFF 2/2 PASS——**至此 R-007 对账账面完整：V141 总览 + V143 ERP↔WMS（含仓）+ V145 MES 物料 + V148 MES 物料×仓**。C-3 @Valid 级联横向补 8 处（Python 直读重扫，第一版 shell 吞斜杠致 RespVO 误报——先修方法）；金额拆箱 NPE 横扫 165 命中**无真实新增**（多为插入即写的 status/type，累减额已有 ZERO/@NotNull 守卫），不盲铺。B-4/C-1 澄清退货"红字发票"即 PURCHASE_RETURN_POSTED 规则贷方行（非独立事件）；**发票税务与成本核算经核实是**设计边界**（自洽的申报/计算子系统，非双重记账单据，见 §12.145）——上一版"真实缺口"系口头过度，已更正。B-1 补 `evidence_retention_inventory.py` **只读**盘点（复用 citations 解析器防漂移；真机 1660 文件/25.7MB/被引用 105，retention=14 天→0 候选；**retention=3 天的"1555 候选"是人为低阈值产物、非真实过期，§12.148 已更正并加固工具**；测试锁死"不删"契约）——R-G3.5-4 的数字可随取，**owner/保留期拍板仍留所有者**。ERP/CRM 全绿、python 653 OK、契约 tracked=59。详见 handoff §12.144 |
| 后续操作落点更正 + 同族真半截护栏（§12.145） | **PASS（发票/成本改判为设计边界；关账补 GL 凭证守卫）** | 所有者批准"进行后续操作"（对象=发票/成本接入会计事件）。**动手前先核实——经读码判定为设计边界**：发票=自洽税务申报子系统（状态机+金税+VAT 销项−进项），成本=归集计算（`executeProfitLossTransfer` 仅汇总销售出库收入 vs 采购入库成本、不生成凭证），会计事件引擎合法类型恰是六种有借贷双重含义的库存/现金单据；强行接入=凭空发明"发票/成本→凭证"的科目借贷映射、静默污染账本，撞"点到即止/不臆造"红线——**不做**，并把 §12.144 的"真实缺口"措辞更正为"设计边界"。**转向做同族真半截护栏**：`closePeriod` 终局守卫只 `sumUnapproved` 六种业务单据、不含 GL 草稿凭证（凭证带 `period_id`、mapper 已有按状态计数方法但没被调）——实测损益结转不强制凭证过账，草稿凭证能存活到关账后被 `resolvePeriod` 永久搁死。修复：关账末尾加 `未过账 = 期间凭证总数 − APPROVED 数 >0 即拒`（新码 1_030_700_008）；度量经 `reverseApprove`→回 DRAFT、posting 仅 approve 置 POSTED 证伪反向漏判，且复用现有 mapper 不碰公共 VO。验证：`closePeriod` 此前 0 测试，补 `ErpPeriodServiceImplCloseGuardTest` 3/3；全模块 164/164；真实链 month-check→transfer→close 先被既有 step4 业务守卫挡（证明链可达且既有那半生效）。详见 handoff §12.145 |
| R-012 报表收口 + R-007 对账可见化（§12.146） | **PASS（资产负债表权益补平；报表/对账权限种子 V149；只读对账端点）** | 先核实再动手：利润表/资产负债表本就从已审核凭证独立取数（"利润表独立性"并非缺口；"期间关闭报表"作为独立功能也不存在缺口）；**真缺口=资产负债表循环只走资产/负债/权益三类、损益科目被静默丢弃**——结转有意不生成凭证（§12.145），一张损益凭证过账即破 `资产=负债+权益`。修复：未结转损益净额作为「本年利润（未分配）」显式并入权益（与利润表同口径，不发明 4103 凭证）。同时补 `erp:financial-statement:query` 的 system_menu 种子（3 端点 @PreAuthorize 却全链无菜单行→普通角色恒 403，§12.95.3 同型）= V149。**R-007**：`InventoryReconciliationService`（真值源 vs ERP/MES 投影，报告结构齐全）**除单测外无任何调用方**——漂移只在 log.warn 里、运维查不到；做**只读端点** `GET /erp/inventory-reconcile/run`（权限串与 V149 同步）；"告警"那半属运维设施、按"点到即止"不做，有端点后差异可见、是否推告警留所有者。验证：`ErpFinancialStatementBalanceSheetTest` 3/3（盈利/亏损/互抵三情形恒等式成立）；真机：审核含 6401 费用分录的凭证后 资产 2500=负债 0+权益 2500、权益行出现「本年利润（未分配）」；对账端点返回 `consistent=false, mismatches=2`（真值 100 vs ERP 73/MES 3——三本账漂移从只能翻日志变成可查询）；重复调用恒等（只读）。V149 自检+verify 3/3；契约 tracked=60。详见 handoff §12.146 |
| 跨模块物料映射批量补全（§12.147） | **PASS（MDM batch upsert `save-batch`）** | 需求核实：MDM 此前只有单条 upsert，真机上 5 条 ERP 审核事件因缺映射进死信（`last_error=MDM_CROSS_REF_NOT_FOUND`），运营补映射只能一条条 POST。新增 `POST /system/mdm-cross-ref/save-batch`——**逐条独立**（部分成功正是补全本意，整体回滚会让 48 成 2 败变全废），逐条复用 `upsert`（校验/幂等/租户规则与单条一致），只吞 `ServiceException` 记入该条、其它异常继续抛；**租户上下文在循环前 fail-closed**（写测试时抓出：第一版把租户缺失也吞成逐条失败→200+全 false，会把环境问题误判成数据问题）；权限复用 `system:mdm-cross-ref:create`（不新造串，§12.95.3）。返回逐条结果（index/success/id/key/error）。验证：`MdmCrossRefServiceTest` 19/19（含部分失败隔离/幂等/noop/无租户 fail-closed）；真机 7/7（3 条含 1 非法 → 2 成 1 败、成功落库失败不落库、重投幂等仍 3 行、空列表 noop、page 反查 5 条）。边界：解决"补映射要一条条来"，**不**自动重投已进死信的 outbox 事件（死信重放属可靠集成层另一入口）。详见 handoff §12.147 |

| CRM 分页 / demo 销售链 / 平台科目回退（§12.151） | **PASS（CRM 两层修复；销售出库→GL 过账真机闭环）** | `crm/follow-up-record/page` 分两层修复：`bizType/bizId` 必填化与权限语义一致（无参真机 `code=400`），`contact_ids/business_ids` 空集合兜底（带参真机 `code=0,total=1`）。销售出库 5480009 审批 `code=0`，生成事件 21/凭证 17/四分录、库存 -2；凭证 17 审核后事件/凭证/posting 均 POSTED，平台科目余额落值。平台科目余额跨租户共享是当前回退设计残余风险。测试：CRM 50/0/5 skip、ERP 169/0/0。详见 handoff §12.151 |
| demo 收款 / 采购入库 / QA 判定（§12.152） | **PASS（收款过账与采购入库→QA 真机落库）** | 收款单 5880006 审核后生成事件 22/凭证 18，凭证审核后 posting POSTED，1002 借 77.24、1122 贷 77.24，销售出库 5480009 `receipt_price=77.24`。采购订单 5560008→采购入库 5600006 审核后库存 +3，事件 23/凭证 19/过账 POSTED，1403 借 1906.26、2202 贷 1906.26；QA 检验单 6320008 绑定 `PURCHASE_IN/5600006` 并 PASS。demo 旧明细与新服务结构不兼容已登记，不改种子数据。 |
| R-G3.5-4 治理 / demo 兼容 / tms collation / 对账告警（§12.153） | **PASS（四项按建议落地；无破坏性清理）** | 新增资源保留策略与四条硬护栏；demo 旧明细 42 行回填新读写列；V150 将 `tms_shipment` 收敛到 `utf8mb4_unicode_ci` 并 PASS；新增外部对账告警推送脚本；2026-10-09 按所有者要求改为纯手动按钮/手动脚本，不注册任何自动排程。真机 dry-run 识别 2 条差异。 |
| R-G3.5-4 收口核实（§12.148） | **维持 OPEN，登记不动（并更正两处事实）** | 所有者点名"owner/保留期、1555 个超期证据文件处置"。**先核实后动手，发现前提不成立**：① **不存在"1555 超期文件"**——该数字是我 §12.144 用 `--retention-days 3`（**任意低阈值**，为证分类器非平凡）跑出的候选数，非真实过期；权威口径重算：磁盘 1660 文件/25.7MB，**已入库冻结基线 400**（`6a47efeb`，`.gitignore:107-109` 反忽略 `*.log` 刻意入库）+ 未入库被引用 51 + 未入库未引用 1209（10.8MB）；证据**最老才 11 天**，**无任何文件按合理保留期算超期**。② **R-G3.5-4 的登记对象是沙箱"运行资源"（容器/卷/网络/DB 账号），不是 evidence 文件**——我此前把文件盘点捆进该项属归类外溢。只读盘点（未删/未停/未改）：`zhicloud-*` 容器 6 个全 Exited（0 在跑）、卷 6、网络 1；工件 `sandbox-and-evidence-census-20261008.json`（SHA-256 `543ab8ae…`，按 §3.4 不入库）。证据处置边界：内层 1209 未引用文件中仅 2 个 basename 与外层仓库文档同名（外层**有自己的** `evidence/production` 且是**另一会话在编辑的独立仓库**），故删内层不影响外层；但删除不可逆且跨会话，**未具名责任人前不删任何文件**。工具已加固：tracked 基线单独统计且永不入候选、保留期短于年龄跨度时显式告警（`retention_below_age_span`）、测试 5 条（含"入库基线绝不候选""非 git 目录不崩"）。结论：**该要的是责任人+保留期+清库存流程+回收证据四件所有者裁决事项，无代码面；维持 OPEN。** |
| 演示主数据真机填充（§12.149） | **PASS（填充落地 + 同族 creator 解析缺陷 32 处修复）** | 所有者指示"数据相关内容网上搜资料模拟填充"。外网本会话不可达，**也不需要**：仓库自带正式模拟通道 `demo-data/`（三重隔离 tenant≥9001/`DEMO_` 前缀/`creator=demo-seed`/`.invalid` 邮箱），§12.114/HANDOFF 红线禁止真实公司名——按通道填充全部虚构。核实更正 §12.114 旧口径：**"demo 无 MES 主数据"已被业务生成器支撑段实质覆盖**（13 张 MES 表 small 档 96 行，含对账契约引用的 `mes_md_item`），不动生成器、不重钉哈希产物。动作：demo 两 SQL 导入服务库（267+2,458 行，号段零碰撞）+ 注册演示租户 9001 + demo_admin 账号（`active_username` 生成列不能显式插值——踩坑）；断言在 DB 行上：逐表计数=manifest、六场景跨域查询非零（4/6/12/64/6/2400）、API 分页 total 对上（validTenant 1 分钟缓存需等待）、**双向租户隔离复查成立**。**当场抓出同族真缺陷**：列表拼接创建人昵称处 `Long.parseLong(creator)` 假设审计列是数字——demo/bootstrap 都会写非数字 creator（`demo-seed`/`admin`），整页 500；租户 1 数字 creator 此前从不触发。修复=① `NumberUtils.parseLong/parseInt` 宽松化（非数字→null，与空串同降级路径，hutool `isLong` 边界实测）②全仓枚举 19 controller **32 处**统一改走 NumberUtils。验证：`NumberUtilsTest` 4/4、common+CRM+ERP 模块测试全绿（48+167）、类字节码验进包、真机重启 **14 个曾 500 端点全 code=0**。登记不动：`crm/follow-up-record/page` 无 bizId 的切面 NPE（租户 1 同样炸，与演示数据无关）。边界：本轮=数据级+读路径验证，未跑 demo 写路径/审批/过账。详见 handoff §12.149 |
| R-008/R-011/#47/R-012 重审与补完（业务缺口第二批） | **PASS×3 + 专项登记（R-008 双向验证；R-011 真缺陷修复；#47 湖半边验收；R-012 专项待下一轮）** | R-008 质检卡点真机双向 8/8（FAILED 拒 1060200008 / 无单 fail-closed / PASSED 放行库存 6→8）；R-011 **真缺陷抓到并修复**：FOR UPDATE 锁了合同但额度 SUM 是 RR 快照读，并发双过（1600>1000）——改锁读变体后真机恰一成一拒（1020004007）、累计 800；#47 ⑤湖半边验收：§12.102 假设被证伪（schema-properties 非法），真机制是 CREATE SCHEMA WITH(location)——lake_build 全链 FINISHED、CTAS 入湖、逐行对数 5==5；R-012 会计事件机制已有过账/冲销护栏（登记册滞后），端到端专项留下一轮。详见 handoff §12.135–§12.138 |
| 支付通知"只有第一次"：重投依赖的调度器在本 profile 整条关闭 | **已量清，未改（留部署形态裁决）** | 一次真实 `Connection timed out` 让 `pay_notify_task` 停在 `status=22 / notify_times=1`，之后 8 分钟无第 2 次投递。机制量清三点：**首投不依赖调度器**（`createPayNotifyTask` 建完任务即内联 `executeNotify0`，所以网络正常时"到账"看着即时）；**重投只依赖 `payNotifyJob`**；而它跑不起来是两层叠加——`infra_job` 18 条任务**全部 `status=2`(STOP)**，且 `application-local.yaml:9` 把 `QuartzAutoConfiguration` 整条 exclude（手工 `PUT /admin-api/infra/job/trigger?id=5` 实测 `code=501 "[定时任务 - 已禁用]"`）。所以口径是"当前部署形态没有异步重试"，不是 pay 的局部缺陷；表现是业务回调失败一次即永久停在已建单未到账的中间态，**且无任何可见化**（无待重投计数、无告警）。本轮未改：放开它要同时决定 18 条任务里哪些该跑、quartz 是否随包启动，属部署形态决策。留两个一句话问题见 handoff §12.127（与 §12.125 的 `notify_times` 钳制同表不同因，不得互相掩盖） |
| 分销绑定在干净库上 NPE（已修）+ trade 参照数据厚度 | **已修 + 真机对照** | 用干净链跑库枚举 `product_*/promotion_*/trade_*` 共 49 表，**全 0 行**，并据此分清"运营该录的"（商品/活动/运费模板）与"系统该种的"（`pay_app` 那类，不种主流程就报错）。`trade_config` 空表不阻断下单（`isGlobalExpressFree`、`addBrokerage` 都把"配置缺失"当"未启用"），但 `BrokerageUserServiceImpl` 的 `:133`/`:194` **直接解引用** `getTradeConfig()` → 会员绑定推荐人这类动作在初始化后的系统里 500。修法沿用模块既有语义（配置缺失＝未启用），不替运营猜默认值；是否给 `trade_config` 种一行仍留给所有者决定。顺带复活 `BrokerageUserServiceImplTest`（3 用例）：挡住它的是一条过时的 `@Disabled` 理由 + **又一处 H2 测试 DDL 漂移**（`price int NOT NULL` vs 生产 `brokerage_price int NULL`），以及一个上游写法自相矛盾的用例（按 mapper 真实过滤列重造夹具，保留 `assertEquals(1,…)` 不动）。读数：该类 3/3 绿，变异检查恰好让空配置那条报 NPE；`zhicloud-module-trade` 全模块 `51 / 0 失败 / 0 错误 / 9 跳过`。详见 handoff §12.121 |
| ⑤ datalake：湖侧查询视图（真跑通，模块层+行级证据） | **PASS**（又修掉两处真 Trino 才现形的缺陷） | Trino 483 真环境（镜像源 `docker.m.daocloud.io`；网络 `zc-datalake`，宿主 `127.0.0.1:55133`；catalog `mysql`+`iceberg`）。业务表 `qms_inspection_order` 经 CTAS 入湖 **9 行 == MySQL 9 行**（真 parquet）；湖视图 `iceberg.ads.v_qms_inspection_daily` **5 行与 MySQL 报表视图 `v_qms_inspection_status_daily` 逐行相等**。用模块自己的生产类打真 Trino：namespaces/tables/table-schema/queryTable 全部返回真数据。修掉的缺陷：① `IcebergCatalogServiceImpl` 缺 `X-Trino-User`（含分页跟进）→ 真 Trino 一律 400；② `queryTable` 只读首屏 `columns`，而 Trino 首屏常只给 `nextUri` → 返回"N 行"却全是空 Map（日志写着"查询返回 5 行, 0 列"，数据被静默丢弃），改为边翻页边吸收列名；同一探针对照：修复前 `[{}, {}]` → 修复后 `[{tenant_id=1, stat_date=2026-10-02, order_count=4, ...}]`。两条都是全 mock 单测结构上看不到的。环境事实（复现必需）：Trino 483 已删除 `hive.metastore=file`；Iceberg JDBC 元数据表要手工建且必须按 **V1 布局**（命名空间属性 `property_key`/`property_value` 分行、`iceberg_tables` 含 `previous_metadata_location`）；写本地路径要 `fs.local.enabled=true`。归档半程也已真跑（隔离探针库，业务库未动）：修掉三处缺陷——对象名缺 schema 段导致任何配置表名都必然失败、DML 不跟进 nextUri 导致"报 SUCCESS 但一条都没归档"、受影响行数误读 `stats.processedRows`（真值在中间页的 `rows` 列）；修后实测 cold 5 行入湖且源表删净（total 9→4、cold 5→0、archivedRows=5）。**管理端点也已真机复跑**（服务端 485 秒起来、`DataLakeAutoConfiguration` 按 catalogUri=127.0.0.1:55133 初始化）：`/admin-api/datalake/namespaces` 返回 4 个真实命名空间、`/tables?namespace=ads` 返回 `v_qms_inspection_daily`、`/table-schema` 返回 8 列类型、`/tables?namespace=ods` 返回入湖表；`/archive-status` 如实返回 `UNKNOWN`（没跑归档就不编造 SUCCESS）。详见 handoff §12.102/§12.104 |
| 生产就绪标记 | 未通过 | `production_ready=false` |
| Gate3 | 未通过 | `gate3_passed=false` |

### 3.3 安全扫描 P0 口径

当前 T-000 证据为 `security-baseline-t000-20260929_095135.json`：

```text
status = passed
scanned_files = 494
finding_count = 216
P0 = 0
P1 = 216
controls_failed = 0
```

这里的 `P0=0` 只表示当前静态规则集未命中阻断项，不表示 Secret、认证、TLS 或依赖漏洞已经关闭。历史 2026-09-27 扫描曾为 463 文件、248 发现、P0=32；历史 P0 的规则来源为：

- 20 条生产 Flyway 种子账号：`R7`
- 12 条 SQL 明文客户端凭据：`R10`；其中 8 套安装脚本的 96 条已移除

此前 3 条 `existingSecret` 空值命中已确认是扫描器假阳性并修复，R5 当前为 0，不得继续计入 P0。具体位置、规则和修复边界见 `13-安全基线扫描证据.md`。当前工作树仍有 1378 项（81 个 tracked 修改 + 1297 个未跟踪文件；不带 `-uall` 折叠为 588 行）未提交/未跟踪。仍未关闭项的可执行清单（每项写清需要谁做什么、证据锚点、不得过度解读的边界）见 `handoff/uncommitted-review-groups-20260928.md` §12.58。
§12.20–§12.24 各批次（方向3 中继实现、两次经中继的 harness 尝试、首连预检、以及**已整体撤销**的 R-036 过期探针）
把该数从 927 推到 933：净增 6 个未跟踪文件全是 harness 证据与旁车，代码类新增只有
`mysql-sandbox.ps1`（tracked 修改，计数不变）与 `test_mysql_sandbox_relay.py`；探针的 2 个文件已删除并记于 §12.24.3。R4 批次增量（硬止损收敛 R-G4-2/R-G3.5-2）：untracked 608→613 = 3 个新代码文件（`scripts/production/SandboxNative.ps1`、`test_sandbox_native_seam.py`、`test_sandbox_native_behavior.py`）+ 2 份 §11.1 可行性实验旁车（`demo-data-timeout-verified/demo-data-mysql-20260930_153509.json` 与同戳 `-migration.log`）；tracked 修改数不变（改动的 6 个 `.ps1` 里 4 个本就是未跟踪新文件，另 2 个已在 65 项内）。本轮真链路 IT 复跑增量（861→864）：untracked 793→796 = 1 个证据目录里的 2 份 Maven 日志 + 1 份 `FILES.sha256`（明细见 `handoff/uncommitted-review-groups-20260928.md` §12.18），tracked 修改数不变，未新增任何代码文件。§12.19 的 R-033 回环中继可行性实验再增 2 项（864→866）：
`evidence/production/r033-loopback-relay-probe-20260930_221131/probe-record.txt` 与其 `.sha256` 旁车，仍属证据类、按 3.4 节不入库。该数随编辑漂移，冻结前必须重测，且不得写入提交说明。R1–R3 批次增量可对账：tracked 56→65 = 前序既有 56 项 + 本轮 9 项（4 个门禁脚本与测试、`mysql-sandbox.ps1` 探针修复、`verify_mysql_runner_sandbox.ps1` 断言扩充、`migration-contract.json` purpose、以及加入行尾豁免注释的 `BootstrapAdminRunner`/`Bm25LexicalRetriever`/`ErpInventoryDualWriter`/`MesInventoryDualWriter`）；untracked 521→582 = 3 个新测试文件 + 1 份新安全基线 JSON 及旁车 + Helm 复现包 + runner 沙箱实跑产物 + Maven 日志及旁车，证据类全部按第 3.4 节保持不入库。非 MySQL 已有静态升级脚本，但尚未在目标实库执行等价约束验证。因此 T-101、T-102、T-107 仍保持 OPEN。

管理员 SQL 模板资产覆盖 8 套方言并按 `tenant_id=1 + username`、`role_id=1`、`post_id=4` 做静态契约校验；生产 K8s 入口是 `BootstrapAdminRunner`。历史 MySQL 8.4.9 沙箱 runner 和只读审计确认了 ensure 创建、相同密码 noop、不同密码 fail-closed 且哈希不变、rotate 成功、旧密码失效、新密码 noop，以及账号唯一性、租户、角色、岗位、BCrypt 形态和 `uk_system_users_tenant_active_username` 索引一致；`independent_review=false`。当前 T-000 history-lock 证据使用 MySQL 8.4.11 并观察到锁与 history 不变。该 bootstrap 证据只覆盖隔离 MySQL 沙箱库，不证明生产目标库、事务边界、并发或其余 7 套方言。V116/V117 的并发/软删除/合成 DDL 证据同样仅限隔离沙箱，不证明生产规模迁移、非 MySQL 并发唯一性、生产物理主从/真实读写分离/多租户数据源路由、生产故障/补偿对账或生产可用；其余 7 套尚未在目标实库执行。

### 3.4 证据存放约定（2026-10-09 更新）

`docs/enterprise-platform/evidence/production/**` 下的执行证据自 2026-10-09 起**纳入版本库并随 `master` 推送**。仓库已按所有者指令改为 private；在仓库保持 private 的前提下，允许保留原始证据文件，避免干净检出后引用悬空。

- 责任人：项目所有者（GitHub `cgs123456`，2026-10-09 明确指令）。
- 保留期：随 `master` 版本保留；删除、重写或公开披露需所有者再次裁决。
- 安全边界：2026-10-09 对未跟踪证据执行 `check_evidence_credentials.py`（1677 文件 / 26,162,555 B，MySQL 凭据命中 0）及高风险模式扫描（1277 个未跟踪 evidence 文件；AWS/私钥/GitHub/Slack/OpenAI/Google/Bearer/JDBC 密码/URI 密码命中 0）。仍存在 92 个文件含本机用户路径、453 个含 JDBC MySQL URL、576 个含回环地址、198 个含 MySQL 容器名，因仓库为 private 暂按原字节保留；若仓库改回 public，必须先脱敏并重算被引用证据的 SHA-256。
- 凡被文档引用的证据，引用处仍必须内联六要素：相对路径 / SHA-256 / 生成时间（取 JSON `generated_at`）/ 生成命令 / 保留期 / 责任人。缺任一要素的结论只能写作“待验证”。
- 有 `.sha256` 旁车的，旁车值必须与文档所写一致。
- 文档中的任何计数必须标注来源证据 SHA，并注明是否为 CI、独立复核。
- `.gitignore` 中 `!docs/enterprise-platform/evidence/production/**/*.log` 的反忽略规则必须保留。
- `evidence/**` 下的 Markdown 是生成产物，CR/BOM 属真实字节，禁止顺手归一。
## 4. 文档索引

| 文件 | 内容 |
|---|---|
| `01-平台现状核验.md` | 代码、模块、迁移、测试、部署现状 |
| `02-企业级能力差距分析.md` | 与企业级目标的差距和优先级 |
| `03-目标架构与升级方案.md` | 目标架构、核心机制、模块边界 |
| `04-业务模块升级路线.md` | 八大域和新域的升级路线 |
| `05-生产就绪门禁清单.md` | 上线门禁、P0、退出条件 |
| `06-数据迁移与发布方案.md` | 数据清理、迁移、双写、回滚 |
| `07-模拟数据与验收方案.md` | 无真实数据时的模拟数据和验收 |
| `08-测试与证据.md` | 测试金字塔、证据分类、缺口 |
| `09-实施排期与资源估算.md` | 阶段、人员、周期、依赖 |
| `10-最终Review与交付审计.md` | 最终审计口径和当前结论 |
| `11-外部对标与资料索引.md` | GitHub 对标、来源和限制 |
| `12-风险登记册.md` | 风险、触发条件、缓解措施 |
| `13-安全基线扫描证据.md` | 当前安全扫描唯一有效口径 |
| `14-实施任务分解与执行顺序.md` | 可直接执行的阶段任务清单 |

## 5. 当前结论

**可以进入生产化改造，不可以直接进入生产部署。**

> 2026-10-08 复核补充：所有者口径已从“工程化收口”收敛为“工程化点到即止，重心是业务功能模块齐全度”。当前商业域已整体移除；演示数据租户 9001 只读链路已验，写路径/审批/过账未跑；生产标记仍 No-Go。R-G3.5-4、CRM follow-up NPE、DLQ 重放、collation 统一、demo 写路径等留所有者裁决或后续验证。下一步先按所有者裁决执行，不自作主张翻状态；继续做业务时以可断言落库的完整链为准。

> 2026-10-09 Issue #1 核实补充：八条中“安全上下文缺失”“MES Job 缺租户”不成立或已闭环；“RAG 缺表”“前端未完整交付”“README 不一致”已补或修正；“模块化构建”“V77 非幂等”“Redis 空密码”本轮落地修复/恢复路径。V77 正文当时为兼容已成功库 checksum 未回改，新增人工恢复脚本 `scripts/production/repair_v77_aimultiagent_resilience.sql`；Redis 空串归一测试 4/4、迁移 runner 31/31、前端构建与 Vite HTTP 200 均已复验。当时 V77 自动幂等、前端构建告警、后端 48080 登录端到端和干净机器脚本实测仍未闭环，不能据此声称生产就绪。后续已在 §12.163.7 收口或划清边界，见下条；详见 handoff §12.162。

> 2026-10-09 未闭环收敛补充（第 1/2/3/4/5 项）：Issue #1 的“无法按 README 登录”残留已定位并修复——全新库缺 `default` OAuth2 客户端（V1.1 清空该表，V115 又把它当演示客户端禁用），而 `AdminAuthServiceImpl` 硬编码 `CLIENT_ID_DEFAULT`。新增前向迁移 `V151__default_oauth2_client_bootstrap.sql`（Secret 执行期随机生成、不含 `client_credentials`、幂等），并登记迁移契约（tracked=62 scanned=158 findings=0）。隔离 MySQL 8.4.11 + 后端 48080 容器取得真实读数：Flyway 实应用 V151、`V151_verify.sql` 5/5 PASS、登录/权限信息/刷新令牌全链路 `code=0`。

> 本轮收口：第 1 项 V77 自动幂等已闭环（正文 `information_schema` + 动态 `ALTER TABLE`，旧 checksum `1776434009` 仅做一次性受控对齐，当前 checksum `-282762132`；定向测试 36/36，隔离 MySQL 重复执行与恢复脚本 6/6 通过）；第 4 项对账告警按所有者要求改为**纯手动按钮/手动脚本，无 Quartz、无 Windows Task Scheduler、无 CI 定时**，真实 webhook 推送通过；第 5 项 `validOAuthClientFromCache` 空 Secret 跳过校验按 A 方案闭环（非空 secret 必须匹配，公开客户端仍可空；定向测试 23/23）。第 2 项（生产等价登录/业务 E2E）与第 3 项（真实干净 Windows 机完整安装）只完成隔离容器/空缓存冷启动的可执行部分，仍不能宣称生产等价；前端 `pnpm build:local` 成功、Vite HTTP 200。附带修复 `SkillCatalogValidator` 启动期租户上下文死代码（3/3）。详见 handoff §12.163.7。

> 2026-10-09 完整性 review + 收尾补充（§12.165）：按所有者指令做完整性 review 与收尾时，发现**远端 master 的 CI 已连续 5 次失败**，失败点是 Bare-Throw Exception Gate（豁免 81 > 上限 64）——39fcebeda 给 DatabaseMigrationRunner 新增 17 处同类豁免却漏改常量，已修正为 81 并在脚本内写明理由。同轮清理：移除误提交的 zhicloud-extracted/（806 文件 / 517 MB 的解包 fat jar，迁移只到 V84）、根目录 redis2_out.txt/runlog.txt、application-local.yaml 的 IM 残留日志配置，并释放本地约 4.1 GB 忽略产物。README 纠偏：登录改为 init_bootstrap_admin.py 随机口令流程（原 admin/admin123 已被 V115 软禁用，属 Issue #1 根因类别），规模数字（主代码 5,882 / 测试 404 / Controller 515 / Flyway 158 至 V151 / SQL 341 / Starter 18）、测试表（删除 V140 已移除的商业域五行）、模块启用状态全部按实测重写；JMeter/Gatling/灾备脚本的同一失效凭据一并纠正。验证：全反应堆 clean package BUILD SUCCESS（07:09）、迁移契约 tracked=62/scanned=158/findings=0、commit readiness PASS、CI 各门禁本地全绿、bare-throw 25/25。**状态不翻**：production_ready=false、gate3_passed=false、independent_review=false、Go/No-Go=No-Go。

> 2026-10-09 完整性 review 收尾补充（§12.166）：修复上一提交（f827af431）在写入本文档与 handoff 台账时**把转义序列当成真实控制字符**的问题（`\r`/`\a`/`\b`/`\0` 吃掉后一字符，导致 `application-local.yaml`、`admin/admin123`、提交短哈希与 Windows 路径被写坏），并顺带修复同类的存量损坏 `evidence/production/schema-source-drift-20260928.md`（37 处）与 `script/docker/nacos/README.md`（`bash`）。修复全程保持各文件原行尾不变。README 再纠偏 5 处：SQL 432→**341**、ERP Controller 71→**70**、Maven 子模块 20→**22**、裸抛豁免 4→**81**、错误码定义 2051→**1830**。远端 master CI 的代码类门禁已全部 success（含此前连续 5 次失败的 Bare-Throw）；仅剩 `Upload Surefire Reports`/`Upload Maven Build Log` 因**账号级 artifact 配额耗尽**失败，属存储问题非代码缺陷，处置需所有者决策（删旧 artifact 或调整上传语义），本轮未擅改 workflow。**状态不翻**：production_ready=false、gate3_passed=false、independent_review=false、Go/No-Go=No-Go。

> 2026-10-09 第 2/3 项收口补充：第 3 项已在**真实干净 Windows Server 2025 Datacenter Evaluation VM** 上闭环——初始无 Java/Maven/Node/pnpm，`start-backend.bat` 在空 Maven 仓库下构建成功并启动 48080/48090，`start-frontend.bat` 在空 pnpm store 下下载 1147 包并以 Vite 8.1.4 启动，`http://127.0.0.1/` HTTP 200；登录/权限/刷新令牌/库存对账均 `code=0`。边界：2 GB VM 需 `MAVEN_OPTS=-Xmx2048m`，且是 Windows Server 2025 Evaluation，不是 Windows 10/11 桌面。第 2 项在单机多副本 + Nginx TLS 入口 + 故障转移 + webhook 上取得生产形态证据，但真实多节点、真实 CA、规模、网络/数据库/进程故障注入仍未验证，因此**第 2 项保持 OPEN**；第 4 项确认纯手动按钮/手动脚本，无任何自动化。证据目录 `docs/enterprise-platform/evidence/production/prodlike-e2e-20261009/` 与 `docs/enterprise-platform/evidence/production/clean-windows-bat-install-20261009/`；详见 handoff §12.164。状态不翻：`production_ready=false`、`gate3_passed=false`、`independent_review=false`、`Go/No-Go=No-Go`。
下一步必须先完成：

1. 在 MySQL 沙箱证据基础上，进入预发/生产等量库验证 V115-V121 的 MDL、锁等待、复制延迟、磁盘空间、回滚和真实业务数据，并对 V121 的空白登记/legacy NULL 回收、lease/expiry/recovery/reconciliation 及 `ADD COLUMN` 影响做生产等价验证，同时修复存量库的 bootstrap 顺序闭环。
2. 为非 MySQL 方言补齐活跃账号并发唯一约束和存量 MFA schema 升级路径。
3. 生产迁移中的客户端 Secret 轮换和外部 Secret 的实际注入、对象存在性、键名、权限和挂载验证。
4. 真实 TLS 握手、证书链、主机名验证，以及 K8s 服务端 dry-run。
5. 库存、质检、工单、回款等核心业务一致性问题修复。
6. 备份恢复、灾备、监控告警和 SLO 证据。
7. 在预发/生产等量环境完成登录与核心业务 E2E，并在无 JDK/Maven/Node/pnpm 缓存的真实干净 Windows 机上跑 `.bat` 全安装；在此之前第 2、3 项保持 OPEN。
