-- ============================================================
-- V103: WMS 入库/出库单类型列扩容
--
-- 背景：
--   WmsReceiptOrderTypeEnum 使用 100~103（生产/采购/退货/归还入库），
--   WmsShipmentOrderTypeEnum 使用 200~202（退货/销售/生产出库），
--   system_dict_data 也登记为这些值。
--
--   但 V11 建表时两列都定义为有符号 TINYINT（上限 127），
--   在 MySQL 严格模式下，WMS 服务创建 100/200 段单据会直接报
--   “Out of range value for column 'type'”。这不是数据问题，而是
--   枚举、字典和 DDL 的契约不一致。
--
-- 处理：
--   1. 将两列扩为 SMALLINT（有符号上限 32767），保留原 NOT NULL 约束。
--   2. 不改写历史数据：原先合法的 1~4 仍可保留，新的 100/200 段可写入。
--   3. 使用 MODIFY COLUMN，重复执行是幂等的；本迁移不删除、不清理任何数据。
--
-- 风险与边界：
--   SMALLINT 足够覆盖当前两个枚举段，但后续若新增超过 32767 的业务类型，
--   必须再次迁移；不能继续靠隐式截断。
-- ============================================================

ALTER TABLE `wms_receipt_order`
    MODIFY COLUMN `type` SMALLINT NOT NULL
    COMMENT '入库类型（100 生产入库 101 采购入库 102 退货入库 103 归还入库；兼容历史 1~4）';

ALTER TABLE `wms_shipment_order`
    MODIFY COLUMN `type` SMALLINT NOT NULL
    COMMENT '出库类型（200 退货出库 201 销售出库 202 生产出库；兼容历史 1~4）';
