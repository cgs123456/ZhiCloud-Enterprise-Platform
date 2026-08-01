# yudao-module-qms

> QMS 质量管理系统：覆盖 IQC（来料检验）/ IPQC（过程检验）/ OQC（出货检验）/ CAPA（纠正预防措施）等企业级质量管控能力。

## 1. 模块简介

`yudao-module-qms` 是 yudao 项目的业务模块（Spring Modulith 声明 A3），实现制造企业质量管理的核心流程：

- **IQC（Incoming Quality Control，来料检验）**：采购入库前对供应商来料进行抽样检验
- **IPQC（In-Process Quality Control，过程检验）**：生产过程中对工单/工序进行检验
- **OQC（Outgoing Quality Control，出货检验）**：成品出库前进行最终检验
- **CAPA（Corrective and Preventive Action，纠正预防措施）**：对质量问题进行根因分析、纠正、预防、关闭

模块依赖 `yudao-module-system` / `yudao-module-infra`，并通过 `yudao-module-mes` / `yudao-module-erp` 接收外部触发（详见 §3 业务边界）。

## 2. 核心能力

| 能力 | Service | 说明 |
|---|---|---|
| 检验单管理 | `InspectionOrderService` | 创建、提交检验结果（自动计算 PASS/FAIL）、查询、关闭 |
| 检验项目 | `InspectionItemService` | 检验项配置、抽样方案（上下限 / 目标值 / 单位） |
| 检验记录 | `InspectionRecordService` | 实际检验结果记录（实测值 + 检验结果） |
| CAPA 管理 | `CAPADocumentService` | 纠正预防措施单：开启、调查、关闭、验证 |

**核心流程**：

```
[来料/工单/出货触发]
        ↓
   创建检验单（InspectionOrder，type=IQC/IPQC/OQC，status=PENDING）
        ↓
   配置/复用检验项目（InspectionItem，含上下限）
        ↓
   录入检验记录（InspectionRecord，measuredValue + result）
        ↓
   提交检验（submitInspection，自动按上下限计算 PASS/FAIL）
        ↓
   检验不通过？──是──→ 创建 CAPA 文档（CAPADocument，source=内部/外部/客户投诉/审核）
        ↓                                    ↓
   检验通过 → 入库/出货              根因分析 → 纠正措施 → 预防措施 → 关闭
```

## 3. 业务边界

### 3.1 与 MES 模块的关系

- **接收 MES 工单/物料触发检验**：MES 工单下达或工序流转时，调用 `InspectionOrderService.createInspectionOrder`（`type=IPQC`，`workOrderId=<MES 工单 ID>`）创建过程检验单
- **检验结果回写 MES**：检验完成后，MES 可通过 `InspectionOrderService.getInspectionOrder` 查询结果，决定工单是否继续流转

### 3.2 与 ERP 模块的关系

- **采购入库触发 IQC**：ERP 采购单入库前调用 `InspectionOrderService.createInspectionOrder`（`type=IQC`，`supplierId=<ERP 供应商 ID>`，`batchNo=<ERP 批次号>`）创建来料检验单
- **检验通过后入库**：ERP 通过 `submitInspection` 接口或查询 `InspectionOrderDO.status=PASSED` 后再执行入库

### 3.3 与 yudao-module-ai 的关系

- 当前 QMS 模块本身不依赖 `yudao-module-ai`
- 规划中将通过 Spring AI `@Tool` 注解暴露 MCP 工具给 yudao-module-ai 的 ReAct Agent / 多 Agent 编排调用（详见 §7 MCP 工具）

## 4. 模块结构

```
yudao-module-qms/
├── pom.xml                                       # 依赖：tenant/security/web/mybatis/excel
└── src/main/java/cn/zhicloud/yudao/module/qms/
    ├── package-info.java                          # Spring Modulith 声明（A3，依赖 system/infra/mes/wms/erp）
    ├── enums/
    │   ├── DictTypeConstants.java                 # 字典类型常量（qms_inspection_type 等）
    │   ├── ErrorCodeConstants.java                # 错误码（1-040-100 ~ 1-040-103 段）
    │   └── qms/
    │       ├── CAPAStatusEnum.java                # CAPA 状态枚举
    │       ├── CAPASourceEnum.java                # CAPA 来源枚举
    │       ├── InspectionOrderStatusEnum.java     # 检验单状态枚举
    │       ├── InspectionTypeEnum.java            # 检验类型枚举（IQC/IPQC/OQC）
    │       ├── InspectionMethodEnum.java          # 检验方法枚举（外观/尺寸/功能/理化）
    │       └── InspectionResultEnum.java          # 检验结果枚举（PASS/FAIL/NA）
    ├── controller/admin/                          # 自动继承 /admin-api 前缀
    │   ├── inspectionorder/
    │   │   ├── InspectionOrderController.java
    │   │   └── vo/（SaveReqVO、RespVO、PageReqVO）
    │   ├── inspectionitem/
    │   │   ├── InspectionItemController.java
    │   │   └── vo/（SaveReqVO、RespVO、PageReqVO）
    │   ├── inspectionrecord/
    │   │   ├── InspectionRecordController.java
    │   │   └── vo/（SaveReqVO、RespVO、PageReqVO）
    │   └── capa/
    │       ├── CAPADocumentController.java
    │       └── vo/（SaveReqVO、RespVO、PageReqVO）
    ├── service/
    │   ├── inspectionorder/
    │   │   ├── InspectionOrderService.java        # 含 submitInspection 自动计算 PASS/FAIL
    │   │   └── InspectionOrderServiceImpl.java
    │   ├── inspectionitem/
    │   │   ├── InspectionItemService.java
    │   │   └── InspectionItemServiceImpl.java
    │   ├── inspectionrecord/
    │   │   ├── InspectionRecordService.java
    │   │   └── InspectionRecordServiceImpl.java
    │   └── capa/
    │       ├── CAPADocumentService.java           # 含 closeCAPADocument 关闭流程
    │       └── CAPADocumentServiceImpl.java
    └── dal/
        ├── mysql/
        │   ├── inspectionorder/InspectionOrderMapper.java
        │   ├── inspectionitem/InspectionItemMapper.java
        │   ├── inspectionrecord/InspectionRecordMapper.java
        │   └── capa/CAPADocumentMapper.java
        └── dataobject/
            ├── inspectionorder/InspectionOrderDO.java    # qms_inspection_order
            ├── inspectionitem/InspectionItemDO.java       # qms_inspection_item
            ├── inspectionrecord/InspectionRecordDO.java   # qms_inspection_record
            └── capa/CAPADocumentDO.java                  # qms_capa_document
```

## 5. 枚举说明

> 所有枚举值与 `sql/mysql/qms.sql` 中字段的 TINYINT 数值保持一致，并在 `DictTypeConstants` 中定义对应字典类型。

### 5.1 检验类型 `InspectionTypeEnum`（字典：`qms_inspection_type`）

| 值 | 名称 | 说明 |
|---|---|---|
| 10 | IQC | 来料检验 |
| 20 | IPQC | 过程检验 |
| 30 | OQC | 出货检验 |

### 5.2 检验单状态 `InspectionOrderStatusEnum`（字典：`qms_inspection_order_status`）

| 值 | 名称 | 说明 |
|---|---|---|
| 10 | PENDING | 待检验 |
| 20 | INSPECTING | 检验中 |
| 30 | PASSED | 检验通过 |
| 40 | FAILED | 检验不通过 |

### 5.3 检验结果 `InspectionResultEnum`（字典：`qms_inspection_result`）

| 值 | 名称 | 说明 |
|---|---|---|
| 10 | PASS | 合格 |
| 20 | FAIL | 不合格 |
| 30 | NA | 不适用 |

### 5.4 检验方法 `InspectionMethodEnum`（字典：`qms_inspection_method`）

| 值 | 名称 |
|---|---|
| 10 | APPEARANCE（外观） |
| 20 | DIMENSION（尺寸） |
| 30 | FUNCTION（功能） |
| 40 | CHEMICAL（理化） |

### 5.5 CAPA 状态 `CAPAStatusEnum`（字典：`qms_capa_status`）

| 值 | 名称 | 说明 |
|---|---|---|
| 10 | OPEN | 待处理 |
| 20 | IN_PROGRESS | 处理中（含根因分析、纠正/预防措施） |
| 30 | CLOSED | 已关闭 |

> CAPA 关闭前需完成 `cause` / `rootCauseAnalysis` / `correctiveAction` / `preventiveAction` 字段填写。

### 5.6 CAPA 来源 `CAPASourceEnum`（字典：`qms_capa_source`）

| 值 | 名称 |
|---|---|
| 10 | INTERNAL（内部） |
| 20 | EXTERNAL（外部） |
| 30 | CUSTOMER_COMPLAINT（客户投诉） |
| 40 | AUDIT（审核） |

## 6. 数据表

> 建表脚本：`sql/mysql/qms.sql`，所有表均含 `creator/create_time/updater/update_time/deleted/tenant_id` 字段（继承 yudao 框架 `TenantBaseDO`）。

### 6.1 `qms_inspection_item` 检验项目表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `code` | VARCHAR(64) | 检验项目编码（唯一） |
| `name` | VARCHAR(255) | 检验项目名称 |
| `type` | TINYINT | 检验类型（10/20/30） |
| `method` | TINYINT | 检验方法（10/20/30/40） |
| `standard` | VARCHAR(500) | 检验标准 |
| `target` | VARCHAR(100) | 目标值 |
| `upper_limit` | DECIMAL(20,4) | 上限 |
| `lower_limit` | DECIMAL(20,4) | 下限 |
| `unit` | VARCHAR(32) | 单位 |
| `status` | TINYINT | 0 启用 / 1 停用 |

### 6.2 `qms_inspection_order` 检验单表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `order_no` | VARCHAR(64) | 检验单号 |
| `type` | TINYINT | 检验类型（10/20/30） |
| `supplier_id` | BIGINT | 供应商 ID（IQC 关联 ERP） |
| `batch_no` | VARCHAR(64) | 批次号 |
| `work_order_id` | BIGINT | 工单 ID（IPQC 关联 MES） |
| `product_id` | BIGINT | 产品 ID |
| `inspector` | VARCHAR(64) | 检验员 |
| `inspect_time` | DATETIME | 检验时间 |
| `status` | TINYINT | 状态（10/20/30/40） |

### 6.3 `qms_inspection_record` 检验记录表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `order_id` | BIGINT | 检验单 ID（关联 `qms_inspection_order.id`） |
| `item_id` | BIGINT | 检验项目 ID（关联 `qms_inspection_item.id`） |
| `measured_value` | VARCHAR(100) | 实测值 |
| `result` | TINYINT | 检验结果（10/20/30） |
| `inspector` | VARCHAR(64) | 检验员 |
| `inspect_time` | DATETIME | 检验时间 |

### 6.4 `qms_capa_document` CAPA 文档表

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | BIGINT | 主键 |
| `capa_no` | VARCHAR(64) | CAPA 单号 |
| `source` | TINYINT | 来源（10/20/30/40） |
| `problem` | TEXT | 问题描述 |
| `cause` | TEXT | 原因 |
| `root_cause_analysis` | TEXT | 根本原因分析 |
| `corrective_action` | TEXT | 纠正措施 |
| `preventive_action` | TEXT | 预防措施 |
| `responsible_person` | VARCHAR(64) | 责任人 |
| `due_date` | DATETIME | 截止日期 |
| `close_date` | DATETIME | 关闭日期 |
| `status` | TINYINT | 状态（10/20/30） |

## 7. API 端点

> 所有控制器位于 `controller.admin` 包下，自动继承 `/admin-api` 前缀。所有写操作均需 `@PreAuthorize` 权限校验。

### 7.1 检验单 `/admin-api/qms/inspection-order/*`

| 方法 | 端点 | 权限 | 说明 |
|---|---|---|---|
| POST | `/create` | `qms:inspection-order:create` | 创建检验单 |
| PUT | `/update` | `qms:inspection-order:update` | 更新检验单 |
| DELETE | `/delete?id={id}` | `qms:inspection-order:delete` | 删除检验单 |
| GET | `/get?id={id}` | `qms:inspection-order:query` | 获取检验单 |
| GET | `/page` | `qms:inspection-order:query` | 分页查询 |
| POST | `/submit?orderId={id}` | `qms:inspection-order:submit` | 提交检验结果（自动计算 PASS/FAIL） |

### 7.2 检验项目 `/admin-api/qms/inspection-item/*`

| 方法 | 端点 | 权限 | 说明 |
|---|---|---|---|
| POST | `/create` | `qms:inspection-item:create` | 创建检验项目 |
| PUT | `/update` | `qms:inspection-item:update` | 更新检验项目 |
| DELETE | `/delete?id={id}` | `qms:inspection-item:delete` | 删除检验项目 |
| GET | `/get?id={id}` | `qms:inspection-item:query` | 获取检验项目 |
| GET | `/page` | `qms:inspection-item:query` | 分页查询 |

### 7.3 检验记录 `/admin-api/qms/inspection-record/*`

| 方法 | 端点 | 权限 | 说明 |
|---|---|---|---|
| POST | `/create` | `qms:inspection-record:create` | 创建检验记录 |
| PUT | `/update` | `qms:inspection-record:update` | 更新检验记录 |
| DELETE | `/delete?id={id}` | `qms:inspection-record:delete` | 删除检验记录 |
| GET | `/get?id={id}` | `qms:inspection-record:query` | 获取检验记录 |
| GET | `/page` | `qms:inspection-record:query` | 分页查询 |

### 7.4 CAPA 文档 `/admin-api/qms/capa/*`

| 方法 | 端点 | 权限 | 说明 |
|---|---|---|---|
| POST | `/create` | `qms:capa:create` | 创建 CAPA 文档 |
| PUT | `/update` | `qms:capa:update` | 更新 CAPA 文档 |
| DELETE | `/delete?id={id}` | `qms:capa:delete` | 删除 CAPA 文档 |
| GET | `/get?id={id}` | `qms:capa:query` | 获取 CAPA 文档 |
| GET | `/page` | `qms:capa:query` | 分页查询 |
| PUT | `/close?id={id}` | `qms:capa:close` | 关闭 CAPA 文档 |

## 8. MCP 工具（规划中）

> 以下 4 个 MCP 工具为本模块与 `yudao-module-ai` ReAct Agent / 多 Agent 编排对接的**规划能力**。当前 QMS 模块未依赖 `yudao-module-ai`，待后续通过 Spring AI `@Tool` 注解暴露后，可被 `yudao-module-ai-multiagent` 的 `ReActAgent` 自动发现并调用。

| 工具名 | 入参 | 出参 | 用途 |
|---|---|---|---|
| `qms_get_inspection_order_by_id` | `id: Long` | `InspectionOrderDO` | 按编号查询检验单详情 |
| `qms_get_capa_page` | `CAPADocumentPageReqVO` | `PageResult<CAPADocumentDO>` | CAPA 文档分页查询 |
| `qms_list_unclosed_capa` | 无 | `List<CAPADocumentDO>` | 查询所有未关闭的 CAPA（status != CLOSED） |
| `qms_get_inspection_record_page` | `InspectionRecordPageReqVO` | `PageResult<InspectionRecordDO>` | 检验记录分页查询 |

启用方式（待实现）：在对应 ServiceImpl 方法上添加 `@Tool(description = "...")` 注解，并在 yudao-server 中通过 `MethodToolCallbackProvider` 注册为 Spring AI ToolCallback。

## 9. 错误码段

使用 `1-040-xxx-xxx` 段（与 yudao-module-ai 共享 1-040 前缀，但通过子段区分）：

| 段 | 说明 |
|---|---|
| `1_040_100_000` ~ `1_040_100_001` | 检验项目（不存在 / 编码重复） |
| `1_040_101_000` ~ `1_040_101_002` | 检验单（不存在 / 不可提交 / 单号生成失败） |
| `1_040_102_000` | 检验记录（不存在） |
| `1_040_103_000` ~ `1_040_103_001` | CAPA 文档（不存在 / 不可关闭） |

## 10. 参考链接

- ISO 9001 质量管理体系：https://www.iso.org/iso-9001-quality-management.html
- 8D 报告法（CAPA 根因分析）：https://en.wikipedia.org/wiki/Eight_Disciplines_Model
- yudao-module-mes（制造执行系统）：见项目根目录
- yudao-module-erp（企业资源计划）：见项目根目录
