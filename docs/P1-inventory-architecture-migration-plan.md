# P1 库存架构一致性迁移方案（P1-1 / P1-2 / P1-4）

> 文档类型：架构迁移方案（仅规划，不含代码实现）
> 适用范围：yudao 多模块库存一致性治理
> 基线日期：2026-08-19
> 配套状态：P2 工程纪律已收口（@Version 五模块、DB CAS、DuplicateKeyException、@Transactional、H2 schema、CI 基线）；P1-3 模块边界治理已完成（`erp.api.ErpMrpExecutorGateway` 反向 SPI 收口）

---

## 1. 背景与目标

当前库存数据在三个业务域各自独立持有，存在**三份真值源**与**双写漂移**风险：

| 真值源 | 模块 | 表 | 并发保护现状 |
|--------|------|----|--------------|
| ERP 库存 | `yudao-module-erp` | `erp_stock` | `updateCountIncrement` / `updateLockedCountIncrement` DB CAS + `ErpStockDO.@Version`（V80，防御性兜底） |
| WMS 库存 | `yudao-module-wms` | `wms_inventory` | `WmsInventoryDO.@Version`（乐观锁）+ 写路径 DA CAS |
| MES 物料库存 | `yudao-module-mes` | `mes_wm_material_stock` | `(item,warehouse,location,area,batch)` 唯一索引 + `getOrCreateMaterialStock` 的 `DuplicateKeyException` 兜底（V79）+ `MesProWorkOrderDO.@Version` |

**核心矛盾**：三套库存各自维护库存数量、锁定数量、CAS 逻辑与并发保护，缺乏统一抽象。跨模块调用目前为**同步调用**（WMS `InventoryApi` 只读、MES→ERP `erp.api` SPI、MES→QMS / WMS→QMS `api`），无事件驱动的最终一致机制。

**目标（P1 三件套）**：
- **P1-4 共享库存 Starter**：抽离统一的库存模型、读写原语与并发保护，消除三处重复实现。
- **P1-1 单一库存真值源**：以 WMS 库存为唯一真值，ERP/MES 库存退化为只读投影或接入共享服务，消灭双写。
- **P1-2 Outbox/Saga 事件化**：以事务发件箱（零新基础设施）打通跨模块库存变更传播，关键流程改为 Saga + 补偿。

---

## 2. 现状拓扑与依赖边界（已核实）

```
                 ┌──────────────────────── 跨模块契约（api 包）────────────────────────┐
 yudao-module-erp │  erp.api.ErpMrpExecutorGateway  (SPI, ERP 定义/MES 实现)             │
 yudao-module-wms │  wms.api.InventoryApi            (只读: getAvailableQuantity/isSufficient) │
 yudao-module-qms │  qms.api.InspectionOrderApi      (MES/WMS 消费)                       │
                 └──────────────────────────────────────────────────────────────────────┘
   库存三真值源:  erp_stock  ──┐
                 wms_inventory ─┼─ 各自独立写、各自 CAS、各自 @Version
                 mes_wm_material_stock ─┘
   同步调用链:  MPS下发 ──erp.api──▶ MES.MrpPlan (SPI 反向)
               入库 ──wms/qms.api──▶ QMS 质检卡点
```

**关键代码锚点（迁移时直接复用/改造）**：
- `yudao-module-wms/api/InventoryApi.java` —— 已有只读契约，升级为共享 Starter 的对外 API 雏形。
- `yudao-module-wms/dal/dataobject/inventory/WmsInventoryDO.java` —— `WmsInventoryDO.@Version`（乐观锁范式）。
- `yudao-module-erp/service/stock/ErpStockServiceImpl.java` —— `updateCountIncrement` / `updateLockedCountIncrement` 的 DB CAS 范式。
- `yudao-module-mes/service/wm/materialstock/MesWmMaterialStockServiceImpl.java` —— `getOrCreateMaterialStock` + `(item,warehouse,location,area,batch)` 复合唯一键（P1-4 复合键的直接来源）。
- `yudao-module-erp/api/ErpMrpExecutorGateway.java` —— 已完成边界收口的 SPI 范例。

---

## 3. P1-4 共享库存 Starter（基础，先行）

**新增模块**：`yudao-spring-boot-starter-biz-inventory`（对齐 `yudao-spring-boot-starter-mybatis` 命名约定）。

### 3.1 契约与模型
- `InventoryItemDO`：以 MES 的 `(item_id, warehouse_id, location_id, area_id, batch_id)` 复合唯一键为**规范键**（WMS 用 `(sku_id, warehouse_id)`，ERP 用 `(product_id, warehouse_id)`；需做键映射适配层）。
- `InventoryApi`（提升自 WMS）：`getAvailableQuantity` / `isSufficient` / `reserve` / `release` / ` deduct` / `add`。
- `InventoryService`：统一实现 `add / deduct / reserve / release`，内部采用**已验证的并发三件套**：
  1. 复合唯一键 `getOrCreate`（复用 MES 的 `DuplicateKeyException` 兜底）；
  2. DB 层 CAS `UPDATE ... SET count = count + #{delta} WHERE id=#{id} AND (count + #{delta}) >= 0`（复用 ERP 范式）；
  3. `@Version` 乐观锁（复用 WMS 范式，守护全量 `updateById`）。
- `lockedCount` 锁定数量原语统一纳入 Starter（替代 WMS/ERP 各自 ad-hoc 的 `lockedCount`）。

### 3.2 落地步骤
1. 建 `yudao-spring-boot-starter-biz-inventory` 模块，仅依赖 `yudao-spring-boot-starter-mybatis` + framework。
2. 迁入 `InventoryApi` + `InventoryItemDO` + `InventoryService`（含三件套并发保护）+ `InventoryMapper`（复合唯一索引 DDL 由 Starter 的 `db/migration` 提供）。
3. 提供自动配置 `InventoryAutoConfiguration`（按 `InventoryProperties` 开关，默认启用）。
4. **验收**：Starter 单测覆盖 add/deduct/reserve/release 在并发下的正确性（复用现有 WMS/ERP/MES 测试范式）。

### 3.3 风险
- 复合键口径不一致（sku vs product vs item）：需在 Starter 内做**统一物品标识映射**，避免键冲突。建议引入 `ItemIdentity` 解析器，由各模块注册自己的 `itemId↔skuId/productId` 映射。

---

## 4. P1-1 单一库存真值源（依赖 P1-4）

**真值归属**：选 **WMS 库存**为唯一真值（已具备 `InventoryApi` 只读契约 + `@Version` + 最完整的库存语义）。

### 4.1 双写过渡（关键，必须带开关）
- **阶段 A（双写 + 投影）**：所有库存写操作（入/出/锁/预留）经 P1-4 Starter 落到 WMS 真值；ERP `erp_stock`、MES `mes_wm_material_stock` 改为**只读投影**，由 Starter 在写后通过 P1-2 事件同步更新（或临时同事务双写 + 日终对账）。
- **阶段 B（去双写）**：ERP/MES 业务读自己的投影；写一律走 Starter。引入**日终对账 Job** 对比 `wms_inventory` 与各投影，差异告警（过渡期安全网）。
- **阶段 C（下线）**：投影收敛为视图或按需查询 Starter；移除 `erp_stock` / `mes_wm_material_stock` 的写路径与表（DDL 迁移标记废弃）。

### 4.2 迁移约束
- 每个阶段用 Feature Flag（`InventoryProperties.enableSingleSource`）隔离，可一键回退到三真值源。
- 不做跨阶段大爆炸重构；逐模块灰度（先 MES 物料库存，再 ERP 库存）。

### 4.3 风险登记
| 风险 | 影响 | 缓解 |
|------|------|------|
| 三套库存历史数据口径不一致 | 合并后数量错乱 | 阶段 A 日终对账 + 数据修正脚本前置 |
| 大事务跨模块写 | 性能/锁竞争 | 写收敛到 Starter 单点；热点用 CAS 而非 SELECT FOR UPDATE |
| 回退困难 | 生产事故 | Feature Flag + 投影保留至阶段 C 之后一个发布周期 |

---

## 5. P1-2 Outbox/Saga 事件化（增量，可与 P1-1 并行）

**原则：零新基础设施优先**。当前无 MQ，采用**事务发件箱（Transactional Outbox）**模式，后续可平滑替换为 Kafka/RabbitMQ。

### 5.1 发件箱（Outbox）
- 新增 `biz_event_outbox` 表：`id, aggregate_type, aggregate_id, event_type, payload_json, status, created_at`。
- 注解 `@OutboxEvent` + `OutboxInterceptor`：在业务事务内同库写入事件行（与库存变更同 TX，保证原子性）。
- **中继（Relay）**：轻量定时轮询（或 Spring `TransactionSynchronization` 后投递到进程内 `ApplicationEvent`）将 `status=PENDING` 行发布；后续替换为 MQ Producer。
- 幂等消费：复用既有 `DuplicateKeyException` + 唯一索引模式（如 CRM `uk_contract_no`、MES 复合唯一键）做消费去重。

### 5.2 Saga 编排（关键流程）
- **MPS→MRP**：`ErpMpsPlanServiceImpl` 下发时写 Outbox 事件，MES 消费后生成 MRP（替代当前同步 `erp.api` SPI 调用，SPI 转为事件订阅）。
- **入库→质检→上架**：WMS 入库完成发事件 → QMS 质检 → 质检通过发事件 → Starter 上架（当前 WMS→QMS 为同步 `api`，改为事件 + 补偿）。
- **销售订单→库存预留**：订单确认发事件 → Starter `reserve`；取消发补偿事件 → `release`。
- **补偿（Compensating）**：每步定义逆向操作（如 MRP 生成失败 → 标记 MPS 下发异常，人工/重试）。

### 5.3 落地步骤
1. 建 `biz_event_outbox` 表 + Outbox 注解/拦截器（先在库存域试点）。
2. Relay 进程内投递（Spring Events），验证端到端。
3. 选 1~2 条核心链路改 Saga（MPS→MRP、入库→质检）。
4. （可选）接入 MQ，Relay 改为 MQ Producer，消费者幂等消费。

### 5.4 风险
| 风险 | 影响 | 缓解 |
|------|------|------|
| 同 TX 写事件行增大事务 | 延迟上升 | 事件行精简；热点链路评估 |
| Relay 投递重复/丢失 | 状态不一致 | 消费幂等（唯一键）+ 至少一次投递语义 |
| Saga 长事务 | 中间态可见 | 明确可见中间态 + 补偿可观测 |

---

## 6. 落地顺序与里程碑

```
M0 现状基线（已完成）         P2 工程纪律 + P1-3 边界治理
   │
M1 P1-4 共享库存 Starter      InventoryApi/DO/Service + 三件套并发 + 单测   ← 前置依赖
   │
M2 P1-1 阶段A 双写+投影       Feature Flag 开启，WMS 为真值，ERP/MES 投影
   │                          + 日终对账 Job（安全网）
M3 P1-2 Outbox 试点           biz_event_outbox + 注解 + 进程内 Relay + 单链路 Saga
   │
M4 P1-1 阶段B/C + P1-2 扩展   去双写、下线旧表；更多链路 Saga；可选 MQ
```

**依赖关系**：P1-4 ⊙ P1-1（P1-1 必须基于 P1-4 的共享抽象）；P1-2 可与 P1-1 并行，但 P1-1 阶段 A 的事件同步可借 P1-2 实现。

**建议节奏**：M1→M2 串行（强依赖）；M2 与 M3 可重叠；M4 在 M2/M3 稳定后启动。

---

## 7. 验收标准 / 完成定义（DoD）

- **P1-4 DoD**：单一 Starter 提供 add/deduct/reserve/release；并发三件套单测全绿；WMS/ERP/MES 三处重复 CAS 逻辑标记废弃。
- **P1-1 DoD**：库存写仅经 Starter 落到 WMS 真值；ERP/MES 投影与真值日终对账差异率为 0（或低于阈值并告警）；Feature Flag 可回退；旧表写路径移除。
- **P1-2 DoD**：核心跨模块库存链路经 Outbox 事件传播；消费者幂等（无重复副作用）；至少 1 条 Saga 链路具备补偿可观测。

---

## 8. 附录：与既有约定的对齐

- **并发三件套复用**：`DuplicateKeyException` 兜底（MES `getOrCreateMaterialStock`）、DB CAS（`ErpStockServiceImpl.updateCountIncrement`）、`@Version`（WMS/CRM/QMS/MES/ERP）已在 P2 收口，P1-4 直接复用，不重新发明。
- **CI 门禁延续**：新增 Starter / 迁移 DDL 须通过现有 `error_codes.py`、`missing_preauthorize.py`（`--max-gaps 89`）、`electronic_signature.py`、`bare_throws.py` 四道门禁；H2 `create_tables.sql` 须与 Flyway 迁移对齐（既有纪律）。
- **模块边界纪律**：所有跨模块调用继续走 `*.api` 包（P1-3 已确立），新增 Starter 对外契约亦置于 `api` 包。
- **Flyway 幂等**：新增 DDL 复用 `information_schema` 判存在模式（参考 V78/V79/V80）。

---

*本方案为规划文档，落地前需逐里程碑评审并确认 Feature Flag 与回退预案。代码实现不在本文档范围。*
