# 智云·企业数字化平台 — 业务模块优化方案

> 范围：纯业务逻辑与业务架构优化（erp / mes / wms / qms / crm / pay）。
> 不涉及运维、测试、CI、部署、监控。
> 依据：① 业务域代码体检（file:line 证据）；② 2026 年互联网/制造企业 Java 后端招聘要求；③ Odoo / ERPNext / RuoYi-MES 等同类开源项目对标。
> 日期：2026-08-17

---

## 0. 执行摘要

本轮对六大核心业务域做体检，并结合外部标杆，得出一个核心结论：

**项目当前的根本矛盾不是"功能缺不缺"，而是"业务一致性没有单一事实来源、模块边界被 import 打破、关键聚合缺少并发保护"。**
具体表现为三套互不同步的库存台账、质检结论无人消费、MES 工单状态机断裂且可无限超产、WMS 可跳过拣货复核直接扣库存。这些问题在单体模块化架构下被掩盖，但在"招聘要求所期待的 DDD / 分布式事务 / 事件驱动 / 幂等"能力线上是硬伤。

本方案分三档推进：
- **P0（业务正确性，1–2 周）**：修复 5 个高严重度风险，低风险高回报。
- **P1（架构一致性，1–2 月）**：确立单一库存真值源 + Outbox/CDC 同步 + Saga 事件化跨模块流程 + 模块 Api 边界。
- **P2（工程纪律，贯穿）**：@Version 乐观锁、DB 层 CAS、幂等键、BigDecimal 全程化、状态机驱动消费。

---

## 一、业务域体检结论（证据级）

体检覆盖 erp(77) / mes(156) / wms(40) / qms(28) / crm(32) / pay(12) 个 ServiceImpl，三个全局事实：

| 事实 | 证据 |
|---|---|
| **三套互不同步的库存台账** | `erp_stock` / `wms_inventory` / `mes_wm_material_stock` |
| ERP ↔ WMS **零调用关系** | 全量 grep：双方 ServiceImpl 无任何跨模块引用 |
| ERP/MES/WMS/QMS **零 `@Version`、零分布式锁** | 全量 grep `@Version` 与 `RLock/Redisson/@Lock4j` 均为空 |

并发控制仅靠零散手段：WMS 的 `SELECT … FOR UPDATE`、单据状态 CAS `updateByIdAndStatus`、库存表 `WHERE count >= x`。**聚合级业务规则（工单累计产出、累计发料、合同累计回款）无任何并发保护。**

### 各模块 Top 问题
- **ERP（高）** `BigDecimal.equals()` 比较数量（`ErpPurchaseOrderServiceImpl.java:237/:259`，`1.0`≠`1.00`）；入库数量无正数校验（`ErpStockInSaveReqVO.java:56-57`、`ErpStockInServiceImpl.java:139-151`）；负库存开关硬编码 TODO（`ErpStockServiceImpl.java:36-38`）。
- **MES（高）** 工单状态机断裂：`finishWorkOrder` 仅接受 `CONFIRMED`，`DISPATCHED`/`REPORTING` 工单永久卡死（`MesProWorkOrderStatusEnum.java:13-18` vs `MesProWorkOrderServiceImpl.java:182`）；报工无超产上限（`MesProWorkOrderMapper.java:38-42`）；发料无 BOM 累计上限（`MesWmProductIssueServiceImpl.java:153-176`）；SQL 字符串拼接 BigDecimal 破坏精度（`MesWmMaterialStockMapper.java:81`）。
- **WMS（高）** 状态机旁路：`completeShipmentOrder` 从 `PREPARE` 直跳 `FINISHED` 并扣库存，绕过拣货→复核→打包→发运（`WmsShipmentOrderServiceImpl.java:112-124`）；盘点只写 `quantity` 不同步 `availableQuantity`，可用量永久失真（`WmsInventoryServiceImpl.java:100`）；出库无预占。
- **QMS（高）** `failRate > 0.5` 才 FAIL，即半数不良仍判合格且用 `double` 判定（`InspectionOrderServiceImpl.java:49/:154-157`）；质检结论无人消费、无 `api` 包，不合格品照样入库（`InspectionOrderServiceImpl.java:148/:156`）；检验单无幂等键/无 `@Transactional`。
- **CRM（中）** 回款超合同额校验为读后写 TOCTOU，并发可超收（`CrmReceivableServiceImpl.java:118-130`）。
- **PAY（低）** 金额为 `Integer` 分，`getRefundPrice()` 为 null 拆箱 NPE（`PayRefundServiceImpl.java:166`）；退款幂等依赖唯一索引兜底、无显式 `DuplicateKeyException` 捕获。

### 最优先修的 5 个业务风险
1. **库存三套账零同步**（账实必然背离，无对账入口）。
2. **质检不闭环 + 判定错误**（不合格品可流向客户，唯一"质量事故+合规"双风险）。
3. **MES 工单双缺陷**（状态机断裂阻塞生产 + 无限超产放大成本错账）。
4. **WMS 流程旁路 + 盘点撕裂**（绕过复核扣库存、可用量校验失效）。
5. **ERP 精度与校验**（各改 1–2 行，但造成采购入库数长期错账 + 可反向扣库存路径）。

---

## 二、外部对标

### 2.1 招聘要求（2026，社招/大厂：长城汽车、东方电气、软通动力等）
共性要求（3–5 年+ 后端）：
- **DDD 领域驱动设计**实战（分层架构、聚合、领域事件）；
- **分布式事务**（Seata/TCC/Saga）、**高并发**、**幂等性**、**熔断限流**；
- **事件驱动 / 消息队列**（Kafka/RocketMQ）做削峰与解耦；
- 事务隔离级别、锁机制、分库分表。

> 对标含义：当前项目虽是单体模块化（非真微服务），但模块间用"直接 import 对方 dal/service"而非 Api/事件，等价于**伪微服务**——既无事务一致性，也无上下文边界。招聘期待的能力线即本方案 P1 的改造方向。

### 2.2 同类开源项目
| 维度 | Odoo | ERPNext | RuoYi-MES（本项目的上游） |
|---|---|---|---|
| 库存 | **单一 WMS 为唯一真值**，多仓，强对账 | 单一库存账 + 内置对账 | 30+ 实体 WMS |
| 质检 | **QC 与入库强绑定**（不通过不能收货） | 质检模块 | 四道关卡 + 模板化 |
| 制造 | MRP + 路由 + 工作中心 + 实时报工 | BOM+工单+作业卡 | **状态机驱动工单** + 统一待检队列（PendingInspect `UNION ALL`） |
| 短板 | 企业版收费 | 长尾需自建 | 无 PLC/APS/OEE（本 fork 进一步退化） |

> 关键启发：① 成熟 ERP **只有一套库存账**，采购/销售/制造都围绕它；② **质检是入库的硬卡点**；③ 上游 RuoYi-MES 的「工单状态机 + 统一待检队列」是可借鉴的设计范式，本 fork 部分能力已退化，应回归。

### 2.3 一致性架构模式（检索结论）
跨服务/跨模块一致性工业方案：**Transactional Outbox**（业务写与事件同事务落库）+ **Saga**（正向步骤绑定补偿，幂等）+ **CDC（Debezium）** 近实时转发；无 CDC 基础设施时可用本地轮询 Outbox（秒级延迟）起步。核心原则：**主库为事实来源，事件先更新主库再广播**；消费侧**状态机驱动 + 幂等键**。

---

## 三、差距分析（项目 vs 标杆）

| # | 差距 | 标杆做法 | 项目现状 |
|---|---|---|---|
| G1 | 库存单一真值源 | Odoo/ERPNext 单账 + 对账 | 三套账零同步 |
| G2 | 质检闭环 | Odoo QC 入库卡点 | QMS 结论无人消费 |
| G3 | 工单状态机完整 | RuoYi 上游状态机驱动 | 状态机断裂 + 无限超产 |
| G4 | 模块边界 | DDD 防腐层 / Api | import 对方 dal/service |
| G5 | 聚合并发保护 | 乐观锁/分布式锁 | 全库 @Version 命中 0 |
| G6 | 事件驱动一致性 | Saga/Outbox/CDC | 同步 import 调用 |

---

## 四、优化方案（按优先级）

### P0 — 业务正确性（立即修，低风险高回报）

**P0-1 ERP 精度与校验**
- 现状：`BigDecimal.equals()` 比较数量（`ErpPurchaseOrderServiceImpl.java:237/:259`）；入库数量仅 `@NotNull`（`ErpStockInSaveReqVO.java:56-57`）。
- 建议：数量/金额比较统一 `compareTo(x)==0`；VO 加 `@DecimalMin(value="0", inclusive=false)`；`validateStockInItems` 补正数校验。
- 依据：金额/数量精度是 ERP 正确性底线；招聘强调"锁机制与事务"。

**P0-2 MES 工单状态机 + 上限**
- 现状：`finishWorkOrder` 拒绝 `DISPATCHED`/`REPORTING`（`MesProWorkOrderServiceImpl.java:182`）；报工/发料无上限 CAS。
- 建议：补全 `dispatch/start/close` 方法并放行 `REPORTING`；mapper 报工 CAS `quantity_produced + incr <= quantity` 抛超产异常；发料引入工单 BOM 已发/应发累计校验；`setSql` 改为 `@Update` + `#{}` 参数化（`MesWmMaterialStockMapper.java:81`）。
- 依据：RuoYi 上游状态机驱动；Odoo 工作中心/路由。

**P0-3 QMS 判定 + 入库卡点**
- 现状：`failRate>0.5` 判合格且用 `double`（`InspectionOrderServiceImpl.java:49/:154-157`）；结论无外部消费。
- 建议：改 AQL Ac/Re 判定 + CRITICAL 缺陷一票否决 + BigDecimal；新增 `module/qms/api` 暴露 `isQualified(bizType,bizId)`，WMS/MES 收货前前置校验质检状态（不合格拒绝入库）。
- 依据：Odoo QC 与入库强绑定；质量合规红线。

**P0-4 WMS 流程完整 + 库存准确**
- 现状：`completeShipmentOrder` 跳过中间状态直接扣库存（`WmsShipmentOrderServiceImpl.java:112-124`）；盘点只写 `quantity`（`WmsInventoryServiceImpl.java:100`）。
- 建议：下线旁路接口（或加开关 + 审计 + 仅测试环境）；盘点按差额同步 `availableQuantity`；出库在创建/拣货阶段用 `LOCK` changeType 预占；新建库存行补 `available/locked/frozen` 零值初始化。
- 依据：库存准确 = 营运资金最大杠杆（标杆共识）；Odoo 收货流程完整性。

### P1 — 架构一致性（中期，高杠杆）

**P1-1 单一库存真值源**
- 建议：确立 `wms_inventory` 为权威库存源；ERP/MES 库存改为经 `wms/api` 获取的**只读投影**，或经 Outbox 表 + 本地轮询/CDC 事件同步；清除三套账。保留双写过渡期 + 日终对账任务兜底。
- 依据：Outbox/Saga/CDC（检索结论）；Odoo/ERPNext 单账（G1）。

**P1-2 跨模块流程 Saga / 事件化**
- 建议：采购入库、生产完工入库、质检门禁改为领域事件（同事务写 Outbox 表 → 轮询/CDC 转发 → 幂等消费），每个正向步骤绑定补偿（如释放预占）。先做"采购→库存""质检→收货"两个最小闭环验证。
- 依据：Saga/Outbox（检索结论）；招聘要求事件驱动（G6）。

**P1-3 模块边界治理**
- 建议：新增 `module/*/api` 包暴露领域能力（优先 QMS Api、WMS 库存 Api）；MES/ERP 经 api 调用而非 import dal；引入防腐层（ACL）。
- 依据：DDD 上下文映射（G4）；招聘要求 DDD。

**P1-4 共享库存能力 starter**
- 建议：把"库存 get-or-create + 负库存校验"抽为 `zhicloud-spring-boot-starter-biz-inventory`，消除三处重复且语义不一致的实现（`ErpStockServiceImpl.java:79-93`、`WmsInventoryServiceImpl.java:299-322`、`MesWmMaterialStockServiceImpl.java:149-170`）。
- 依据：DRY + 标杆一致性。

### P2 — 工程纪律（贯穿业务改造）

- **@Version 乐观锁**：覆盖所有可变业务聚合根（工单/订单/库存/合同/检验单）。
- **关键累计量下沉 DB 层 CAS**：已有 WMS 行锁范式（`WmsInventoryServiceImpl.java:148` `selectListByIdsForUpdate`）推广到 MES/ERP 累计量。
- **幂等键/唯一索引**：检验单、回款、入库单按 `(bizType,bizId)` 建唯一索引；显式捕获 `DuplicateKeyException`。
- **BigDecimal 全程化**：金额/数量/比率（含 CRM 统计 `Double` 累加改 BigDecimal）。
- **状态机驱动消费**：参考 RuoYi `PendingInspect` 统一待检队列（`UNION ALL`）模式，收敛多来源待办入口。

---

## 五、落地路径与里程碑

| 里程碑 | 周期 | 交付物 |
|---|---|---|
| **M1 业务正确性** | 1–2 周 | P0-1~P0-4 全修复；状态机单测；DAO 参数化；VO 校验 |
| **M2 边界与卡点** | 2–4 周 | QMS Api + WMS/MES 收货质检卡点；单一库存真值源 + 日终对账任务；`api` 包起步 |
| **M3 事件化与纪律** | 1–2 月 | Outbox + Saga 核心链路（采购→库存、质检→收货）；共享库存 starter；@Version/幂等全覆盖 |

---

## 六、风险与权衡

1. **三套账合并是高影响重构**：必须双写过渡 + 对账兜底，禁止一次性切换，避免账实错乱。
2. **Saga 引入复杂度**：先验证两个最小闭环，再推广；单体架构下 Outbox 用本地轮询即可（无需强制 Debezium），降低基础设施门槛。
3. **APS/OEE/PLC 非本次阻断项**：属工业级增强 roadmap，不在 P0/P1 范围，避免范围蔓延。
4. **回归面**：状态机与并发改造影响主链路，建议配契约测试（VO/状态枚举不变式）守护，避免"修复即破坏"。

---

## 七、与招聘能力线的对齐小结

完成 P0–P2 后，项目在业务维度可对齐招聘要求的：DDD 上下文边界（Api/ACL）、分布式一致性（Saga/Outbox）、高并发正确性（@Version/CAS/幂等）、事件驱动（领域事件）。这既是工程质量提升，也使项目在简历/面试中具备"解决过跨模块库存一致性、质检闭环、状态机驱动"等可量化叙事。
