-- ============================================================
-- V101: 主数据跨域对照（mdm_cross_ref）
--
-- 背景：
--   ERP 的 productId / warehouseId 与 WMS 的 skuId / warehouseId 是两套独立自增主键。
--   V100 落地的可靠集成数据面解决了「事件可靠投递」，但没有解决「投递的内容里
--   的 ID 属于哪套主键空间」：ERP 销售出库审核通过 -> WMS 生成出库单 这条链路，
--   若直接把 ERP 的 productId 当 WMS 的 skuId 写入，只有在两套数据恰好同号时才
--   「看起来正常」，一旦任一侧重新导入或主键空间变化，就会把出库单建到错误的
--   SKU / 仓库上，且事后难以追溯。02-企业级能力现状核验.md 把「跨域主数据未统一」
--   列为 P0 第 1 条。
--
-- 目标：
--   1. 建立显式的跨域主数据对照表，把「编码相同」的巧合替换为可维护的契约。
--   2. 让 ERP -> WMS 首条链路在映射缺失时显式失败（进入死信并告警），而不是静默写错。
--
-- 语义：
--   * 唯一键 uk_cross_ref(tenant_id, source_system, source_entity, source_id,
--     target_system, target_entity)：同一源实体在同一目标系统内只允许一个当前映射，
--     重复登记是 upsert（更新 target_id），不是新增行——对照表表达「当前事实」。
--   * status：VALID 生效 / INVALID 已失效。失效映射等同缺失，解析时必须失败，
--     避免用一条已废弃的映射继续写下游单据。用 VARCHAR 而非 ENUM，避免改列。
--   * source_id / target_id 用 VARCHAR(64) 而不是 BIGINT：当前 ERP/WMS 都是数值主键，
--     但外部系统（电商、客户编码）的标识可能是字符串，数值列会堵死这条路。
--   * 没有外键：映射指向的主数据可能被归档/清理，外键会让「清理主数据」被映射表阻断，
--     也会让「映射先于主数据建立」的初始化顺序无法实现。一致性由应用层查询校验。
--   * idx_target 用于反向追溯（给定一个 WMS SKU，反查它对应哪个 ERP 产品），
--     对账与问题定位会用到，建表时一起建，避免后续追索引。
--
-- 兼容性：
--   * CREATE TABLE IF NOT EXISTS，可重复执行；不删除、不改名、不删列。
--   * 新增表，不存在历史数据回填问题。
--
-- 限界：
--   * 本迁移只建结构，不灌数据；模拟映射行由 V102 单独灌入，便于区分「结构」与「演示数据」。
--   * 不建主数据治理流程（申请/审批/版本/失效原因），只给运行时解析所需的最小结构。
--   * 未做多租户强绑定：ERP/WMS 主表当前没有 tenant_id 列，链路统一按默认租户 0 运行。
-- ============================================================

CREATE TABLE IF NOT EXISTS `mdm_cross_ref`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT,
    `tenant_id`     BIGINT      NOT NULL DEFAULT 0                       COMMENT '租户编号',
    `source_system` VARCHAR(32) NOT NULL                                 COMMENT '源系统，如 ERP',
    `source_entity` VARCHAR(32) NOT NULL                                 COMMENT '源实体类型，如 PRODUCT/WAREHOUSE/CUSTOMER',
    `source_id`     VARCHAR(64) NOT NULL                                 COMMENT '源编号（字符串，兼容非数值标识）',
    `target_system` VARCHAR(32) NOT NULL                                 COMMENT '目标系统，如 WMS',
    `target_entity` VARCHAR(32) NOT NULL                                 COMMENT '目标实体类型，如 SKU/WAREHOUSE/MERCHANT',
    `target_id`     VARCHAR(64) NOT NULL                                 COMMENT '目标编号',
    `status`        VARCHAR(16) NOT NULL DEFAULT 'VALID'                 COMMENT '状态：VALID 生效 / INVALID 已失效',
    `remark`        VARCHAR(500) DEFAULT NULL                            COMMENT '备注（映射来源、维护人、失效原因）',
    `creator`       VARCHAR(64)  DEFAULT ''                              COMMENT '创建者',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `updater`       VARCHAR(64)  DEFAULT ''                              COMMENT '更新者',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       BIT          NOT NULL DEFAULT 0                      COMMENT '是否删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_cross_ref` (`tenant_id`, `source_system`, `source_entity`, `source_id`, `target_system`, `target_entity`),
    KEY `idx_tenant_source` (`tenant_id`, `source_system`, `source_entity`, `source_id`),
    KEY `idx_target` (`target_system`, `target_entity`, `target_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = '主数据跨域对照（ERP/WMS/MES/QMS/TMS 主数据映射，V101）';

-- 回滚：新增表，未被业务写入前可直接 DROP；一旦已有映射并被集成链路依赖，
-- 删除会让正在运行的链路全部因「映射缺失」进入死信，必须先停写再执行。
