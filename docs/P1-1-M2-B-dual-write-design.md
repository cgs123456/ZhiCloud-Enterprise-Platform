# P1-1 阶段 B：双写路径落地设计（M2 Stage B）

> 文档类型：实施计划（待评审通过后落地）
> 基线日期：2026-08-19
> 前置条件：M2 阶段 A 只读投影已交付（96bc2cb），`InventoryProjectionReader` SPI 已就绪

---

## 1. 目标

`enableSingleSource=true` 时，所有库存写操作**同时落** `inventory_item` 真值源与业务投影表（`mes_wm_material_stock` / `erp_stock`），由共享 Starter 承担并发保护与 CAS，保持业务表结构不变。Feature Flag 可一键回退（关闭后双写停止，读照常）。

---

## 2. 现状写路径锚点

| 模块 | 写入口 | 事务边界 | 关键方法 |
|------|--------|---------|---------|
| **MES** | `MesWmTransactionServiceImpl` | 全方法 `@Transactional` | `getOrCreateMaterialStock` → `updateMaterialStockQuantity` |
| **ERP** | `ErpStockServiceImpl` + `ErpStockRecordServiceImpl` | 各自 `@Transactional` | `updateStockCountIncrement` / `lockStock` / `unlockStock` |
| **Starter** | `InventoryService.add/deduct/reserve/release` | 各自 `@Transactional` | 复合键 `getOrCreate` + DB CAS + `@Version` |

---

## 3. 设计决策

### 3.1 事务模型：同 TX 双写（不引入异步）

- 使用 Spring `TransactionSynchronizationManager.registerSynchronization` 在**事务提交前**触发双写。
- 若 `beforeCommit` 失败 → 整笔事务回滚，无孤立数据风险。
- 若 `afterCommit` 失败 → 业务事务已提交，业务表与真值源不一致，依赖日终对账 Job（M2 阶段 A 已具备）修复。
- **优先 `beforeCommit`**：保证写幂等、事务完整。

### 3.2 键映射规则

| 维度 | MES | ERP |
|------|-----|-----|
| `itemId` | `MesWmMaterialStockDO.itemId` | `ErpStockDO.productId` |
| `warehouseId` | 相同 | 相同 |
| `locationId` | 相同 | **null**（ERP 无此维度，对账按 0L 处理） |
| `areaId` | 相同 | **null** |
| `batchId` | 相同 | **null** |
| `batchCode` | 保留冗余写入 `inventory_item.batch_code` | N/A |
| `quantity` delta | `newQty - oldQty`（正=入/负=出） | `newCount - oldCount` |
| `lockedCount` delta | MES 无锁定原语，**始终为 0** | 通过 `lockStock`/`unlockStock` 计算 delta |

### 3.3 模块间循环依赖规避

- ERP 已依赖 `zhicloud-spring-boot-starter-biz-inventory`（M2-A 已登记）。
- MES 已依赖同一 Starter（M2-A 已登记）。
- 两模块双写实现类（`MesInventoryDualWriter` / `ErpInventoryDualWriter`）注册为 `@Component`，**不反向 import 业务 DAL**，仅调 `InventoryService` API。

---

## 4. 改动范围

### 4.1 Starter 层（新增 SPI）

```
zhicloud-framework/zhicloud-spring-boot-starter-biz-inventory/src/main/java/
  inventory/config/
    InventoryProperties.java          # 新增字段：enableDualWrite = false
  inventory/service/
    InventoryDualWriter.java          # 新增：SPI 接口（readAll 已有；新增 afterWrite 钩子）
```

**`InventoryDualWriter` SPI：**

```java
public interface InventoryDualWriter {
    /**
     * 事务提交前执行，写入 inventory_item。
     * 若返回 false 则触发事务回滚（beforeCommit 语义）。
     */
    boolean beforeCommit(Long itemId, Long warehouseId, Long locationId,
                         Long areaId, Long batchId, String batchCode,
                         BigDecimal quantityDelta, BigDecimal lockedDelta);
}
```

**`InventoryAutoConfiguration` 更新：**
- `@Bean` 注册 `InventoryDualWriteTransactionSynchronizationRegistrar`，收集所有 `InventoryDualWriter` 实例并注册 `TransactionSynchronization`。

### 4.2 MES 模块（新增 Writer）

```
zhicloud-module-mes/src/main/java/cn/zhicloud/zhicloud/module/mes/service/wm/materialstock/
  MesInventoryDualWriter.java          # 新增
```

**核心逻辑：**
1. 注入 `InventoryService`。
2. `beforeCommit` 内调用 `inventoryService.add(itemId, warehouseId, locationId, areaId, batchId, batchCode, quantityDelta)`；lockedDelta 恒为 0（MES 无锁定原语）。
3. 异常 → 记录 warn 日志 + 抛 `RuntimeException` 触发事务回滚。

**双写触发点：**
- 在 `MesWmMaterialStockServiceImpl` 中增加 `@Autowired(required=false) List<InventoryDualWriter> dualWriters`。
- 在 `updateMaterialStockQuantity` 末尾：收集本事务内的 qty delta 列表，通过 `TransactionSynchronizationManager.registerSynchronization` 统一在 `beforeCommit` 中触发双写。
- `getOrCreateMaterialStock` 本身不触发双写（仅创建空行，后续 quantity 更新时才触发）。

**事务安全注意：**
- `MesWmTransactionServiceImpl` 本身有 `@Transactional`，内部调用 `MesWmMaterialStockServiceImpl` 两个方法均处于同一 TX，通过 `TransactionSynchronizationManager` 保证仅提交一次。

### 4.3 ERP 模块（新增 Writer）

```
zhicloud-module-erp/src/main/java/cn/zhicloud/zhicloud/module/erp/service/stock/
  ErpInventoryDualWriter.java          # 新增
```

**核心逻辑：**
1. 注入 `InventoryService`。
2. `beforeCommit` 内：
   - qty delta：`inventoryService.add(productId, warehouseId, null, null, null, null, quantityDelta)`
   - locked delta：`inventoryService.reserve/release(productId, warehouseId, null, null, null, lockDelta)`
3. 异常 → warn + RuntimeException 触发回滚。

**双写触发点：**
- `ErpStockServiceImpl.updateStockCountIncrement` / `lockStock` / `unlockStock` 末尾通过 `TransactionSynchronizationManager` 收集 delta 并在 `beforeCommit` 统一触发。

### 4.4 Feature Flag

```yaml
# application-unit-test.yaml（测试）
zhicloud:
  inventory:
    enabled: true
    enableSingleSource: false       # 默认关闭，M2-B 灰度开启
    enableDualWrite: false          # 默认关闭，M2-B 灰度开启
```

- 两开关联动：`enableSingleSource=true` 且 `enableDualWrite=true` 时双写生效。
- 任一关闭 → 双写静默跳过，业务行为不变。

### 4.5 测试

| 测试 | 模块 | 内容 |
|------|------|------|
| `MesInventoryDualWriterTest` | MES | mock `InventoryService`，验证 delta 映射正确 |
| `ErpInventoryDualWriterTest` | ERP | mock `InventoryService`，验证 delta 映射正确 |
| `MesWmTransactionServiceImplTest`（新增） | MES | 端到端：双写开启后，插入业务表 + 同步写入 `inventory_item` |
| `ErpStockRecordServiceImplTest`（新增） | ERP | 同上 |
| 日终对账重跑 | MES + ERP | 验证双写一致（M2-A 集成测试已覆盖此基础） |

---

## 5. 风险登记

| 风险 | 影响 | 缓解 |
|------|------|------|
| 双写失败导致事务回滚 | 业务操作失败 | `beforeCommit` 抛异常 → 整笔回滚，不出现半写；日终对账 Job 兜底 |
| 事务嵌套 / 传播级联 | 双写执行次数异常 | 用 `TransactionSynchronizationManager.isSynchronizationActive()` 判活性，单 TX 仅注册一次 |
| ERP 无 location/area/batch 维度 | 投影与真值维度不对齐 | 对账 `compositeKey` 已处理 null→0L，对齐无误 |
| 回退困难 | 生产事故 | Feature Flag 可一键关闭双写；投影保留至 M4 |

---

## 6. 灰度顺序

1. **MES 灰度**（先）：物料库存粒度更细、业务量更大，优先验证双写稳定性。
2. **ERP 灰度**（后）：产品库存粒度粗，风险相对可控。
3. 每个模块灰度期间：日终对账 Job 持续运行，差异率 > 0% 则告警 + 自动回滚双写开关。

---

## 7. DoD

- `enableDualWrite=true` 时，MES/ERP 库存写操作原子落两表。
- 日终对账在双写开启后稳定 ≤ 0 差异（或人工确认可忽略阈值）。
- Feature Flag 可一键关闭双写，业务行为恢复原状。
- 四道 CI 门禁全绿（error_codes / bare_throws / electronic_signature / preauthorize）。
