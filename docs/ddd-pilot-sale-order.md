# P2-1 DDD 深化试点：ERP 销售订单聚合根 + 领域事件 + CQRS 读模型

## 1. 试点说明

本试点在 ERP 销售订单模块中引入 DDD（领域驱动设计）实践，采用**最小侵入、并存过渡**的策略：
不删除现有 DO/Mapper/Service/Controller，新增 DDD 风格的聚合根、领域事件、CQRS 读模型，
与现有三层架构并存，新功能优先使用 DDD 风格。

### 1.1 试点范围

- **模块**：`yudao-module-erp` 销售订单（`erp_sale_order` + `erp_sale_order_items`）
- **不扩散**：采购、库存、财务等模块不在本次试点范围

### 1.2 包结构

```
com.zhicloud.yudao.module.erp
├── domain.saleorder                        # 新增：DDD 领域层（销售订单聚合）
│   ├── ErpSaleOrderAggregate.java          # 聚合根
│   ├── SaleOrderItem.java                  # 明细值对象
│   ├── SaleOrderStatus.java                # 状态枚举（值对象）
│   ├── vo/                                 # 命令对象
│   │   ├── CreateSaleOrderCommand.java
│   │   ├── UpdateSaleOrderOutCountCommand.java
│   │   └── UpdateSaleOrderReturnCountCommand.java
│   ├── event/                              # 领域事件
│   │   ├── SaleOrderCreatedEvent.java
│   │   ├── SaleOrderAuditedEvent.java
│   │   ├── SaleOrderOutCountUpdatedEvent.java
│   │   └── SaleOrderReturnCountUpdatedEvent.java
│   ├── repository/                         # 仓储（Port + Adapter）
│   │   ├── ErpSaleOrderRepository.java     # 接口（Port）
│   │   └── ErpSaleOrderRepositoryImpl.java # 实现（Adapter）
│   ├── listener/                           # 领域事件监听器
│   │   └── SaleOrderDomainEventListener.java
│   └── service/                            # 领域服务
│       └── SaleOrderDomainService.java
├── query.saleorder                         # 新增：CQRS 读模型
│   ├── SaleOrderView.java                  # 读模型视图
│   ├── SaleOrderPageQuery.java             # 分页查询参数
│   ├── SaleOrderQueryService.java          # 查询接口
│   └── SaleOrderQueryServiceImpl.java      # 查询实现
└── domain.sale                             # 已有：上一轮 DDD 试点（保留）
    ├── aggregate/
    └── event/
```

## 2. 聚合根设计

### 2.1 聚合边界

- **聚合根**：`ErpSaleOrderAggregate`
- **内部实体**：`SaleOrderItem`（值对象，不可变）
- **边界规则**：外部只能通过聚合根方法操作订单，明细不暴露独立修改入口

### 2.2 不变式

| 编号 | 不变式 | 校验位置 |
|------|--------|----------|
| 1 | 总价 = 明细小计之和 + 税额 - 优惠金额 | `recalculateAmount()` |
| 2 | 出库数量 ≤ 订单总数量 | `updateOutCount()` |
| 3 | 退货数量 ≤ 出库数量 | `updateReturnCount()` |
| 4 | 审批后不可修改明细 | 状态校验 |

### 2.3 状态机

```
DRAFT(10, 草稿) ──audit()──→ AUDITED(20, 已审批) ──cancel()──→ CANCELED(99, 已取消)
     │                                                        │
     └──────────────cancel()──────────────────────────────────┘
```

- `DRAFT`：可审批、可取消
- `AUDITED`：可出库、可退货、可取消（需无出库）
- `CANCELED`：终态

### 2.4 核心方法

| 方法 | 说明 | 触发事件 |
|------|------|----------|
| `create(cmd)` | 工厂方法，创建草稿订单 | `SaleOrderCreatedEvent` |
| `audit()` | 审批通过 | `SaleOrderAuditedEvent` |
| `updateOutCount(delta)` | 累加出库数量 | `SaleOrderOutCountUpdatedEvent` |
| `updateReturnCount(delta)` | 累加退货数量 | `SaleOrderReturnCountUpdatedEvent` |
| `cancel()` | 取消订单 | - |
| `reconstitute(...)` | 从持久化数据重建聚合根 | - |
| `pullDomainEvents()` | 拉取并清空事件队列 | - |

## 3. 领域事件清单

| 事件 | 触发时机 | 潜在订阅方 |
|------|----------|------------|
| `SaleOrderCreatedEvent` | 订单创建 | 财务（初始化应收）、消息（推送通知） |
| `SaleOrderAuditedEvent` | 订单审批通过 | 库存（预留库存）、财务（生成应收）、BPM（后续流程） |
| `SaleOrderOutCountUpdatedEvent` | 出库数量更新 | 库存（扣减实际库存） |
| `SaleOrderReturnCountUpdatedEvent` | 退货数量更新 | 库存（恢复库存） |

### 3.1 事件发布机制

- 聚合根在状态变更时将事件加入内部队列（`domainEvents`）
- 仓储 `save()` 持久化后调用 `pullDomainEvents()` 拉取事件
- 通过 Spring `ApplicationEventPublisher` 发布
- 监听器使用 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 异步处理

### 3.2 事件监听器

`SaleOrderDomainEventListener` 监听所有 4 种事件，当前为日志占位实现，
注释标注了未来可对接的系统（BPM / 库存 / 应收）。

## 4. CQRS 读模型

### 4.1 读写分离

| 维度 | 写侧 | 读侧 |
|------|------|------|
| 入口 | `SaleOrderDomainService` | `SaleOrderQueryService` |
| 模型 | `ErpSaleOrderAggregate` | `SaleOrderView` |
| 持久化 | `ErpSaleOrderRepository` → Mapper | 直接读取 Mapper |
| 事务 | 读写事务 | 只读 |

### 4.2 读模型视图

`SaleOrderView` 为扁平化视图，包含：
- 订单头字段（no, status, orderTime, ...）
- 冗余字段（customerName, saleUserName, productName）
- 金额字段（totalPrice, discountPrice, baseCurrencyTotalPrice, ...）
- 出库/退货跟踪（outCount, returnCount）
- 明细列表（List<Item>，含 productName）

### 4.3 查询优化

- **分页查询**：批量加载客户名称和销售员名称，避免 N+1
- **详情查询**：批量加载产品名称
- **参数转换**：`SaleOrderPageQuery` → `ErpSaleOrderPageReqVO`，复用现有 Mapper 查询逻辑

## 5. 仓储设计

### 5.1 端口适配器模式

```
domain.saleorder.repository.ErpSaleOrderRepository  （Port 接口）
        ↑ 实现
domain.saleorder.repository.ErpSaleOrderRepositoryImpl  （Adapter）
        ↓ 使用
dal.mysql.sale.ErpSaleOrderMapper / ErpSaleOrderItemMapper  （现有 Mapper）
```

### 5.2 聚合根持久化

- `save()`：订单头 insert/update + 明细先删后插（保证聚合一致性）
- `findById()`：加载订单头 + 明细，通过 `reconstitute()` 重建聚合根
- `findByIds()`：批量加载，按 orderId 分组明细

## 6. 与现有 Service 的并存策略

### 6.1 并存原则

| 现有实现 | DDD 实现 | 关系 |
|----------|----------|------|
| `ErpSaleOrderService` | `SaleOrderDomainService` | 并存，新功能优先 DDD |
| `ErpSaleOrderServiceImpl` | `SaleOrderDomainService` | 互不影响 |
| `ErpSaleOrderController` | 无新 Controller | 路由不变 |
| `query.sale.SaleOrderQueryService` | `query.saleorder.SaleOrderQueryService` | 并存，不同包 |
| `domain.sale.aggregate.SaleOrderAggregate` | `domain.saleorder.ErpSaleOrderAggregate` | 并存，不同包 |

### 6.2 Bean 名称隔离

- 现有查询实现 Bean 名：`saleOrderQueryServiceImpl`
- 新增查询实现 Bean 名：`dddSaleOrderQueryServiceImpl`（显式指定，避免冲突）

### 6.3 迁移路径

1. **当前**：DDD 实现与现有实现并存，互不影响
2. **下一步**：出库 / 退货流程接入 `SaleOrderDomainService`（替代直接调用 `updateSaleOrderOutCount`）
3. **远期**：现有 Service 方法标记 `@Deprecated`，逐步迁移到 DDD 风格

## 7. 领域服务

`SaleOrderDomainService` 封装跨聚合的编排逻辑：
- 加载聚合根 → 调用聚合根方法 → 持久化（事件自动发布）

| 方法 | 说明 |
|------|------|
| `auditOrder(orderId)` | 审批订单 |
| `updateOutCount(cmd)` | 更新出库数量 |
| `updateReturnCount(cmd)` | 更新退货数量 |
| `cancelOrder(orderId)` | 取消订单 |

## 8. 未来推广方向

1. **扩展到出库 / 退货**：为 `ErpSaleOut`、`ErpSaleReturn` 建立独立聚合根
2. **事件驱动库存**：出库 / 退货事件由库存模块独立监听，实现销售 → 库存单向解耦
3. **读模型物化**：为高频查询场景建立物化视图表，定时同步
4. **领域服务下沉**：跨聚合逻辑（如订单 + 库存联动）统一走领域服务
5. **试点扩散**：验证成熟后推广到采购、库存等模块

## 9. 新增文件清单

### 9.1 DDD 领域层（`domain/saleorder/`）
- `ErpSaleOrderAggregate.java` - 聚合根
- `SaleOrderItem.java` - 明细值对象
- `SaleOrderStatus.java` - 状态枚举
- `vo/CreateSaleOrderCommand.java` - 创建命令
- `vo/UpdateSaleOrderOutCountCommand.java` - 出库命令
- `vo/UpdateSaleOrderReturnCountCommand.java` - 退货命令
- `event/SaleOrderCreatedEvent.java` - 创建事件
- `event/SaleOrderAuditedEvent.java` - 审批事件
- `event/SaleOrderOutCountUpdatedEvent.java` - 出库事件
- `event/SaleOrderReturnCountUpdatedEvent.java` - 退货事件
- `repository/ErpSaleOrderRepository.java` - 仓储接口
- `repository/ErpSaleOrderRepositoryImpl.java` - 仓储实现
- `listener/SaleOrderDomainEventListener.java` - 事件监听器
- `service/SaleOrderDomainService.java` - 领域服务

### 9.2 CQRS 读模型（`query/saleorder/`）
- `SaleOrderView.java` - 读模型视图
- `SaleOrderPageQuery.java` - 分页查询参数
- `SaleOrderQueryService.java` - 查询接口
- `SaleOrderQueryServiceImpl.java` - 查询实现

### 9.3 修复缺失文件（现有代码补全）
- `domain/sale/aggregate/OrderItem.java` - 明细值对象（record）
- `query/sale/SaleOrderSummaryVO.java` - 摘要 VO
- `query/sale/SaleOrderDetailVO.java` - 详情 VO
