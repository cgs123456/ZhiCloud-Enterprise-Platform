-- ============================================================
-- V102: 主数据跨域对照模拟数据（mdm_cross_ref 种子）
--
-- 背景：
--   用户没有真实数据，V101 只建了 mdm_cross_ref 结构。没有映射行时，
--   「ERP 销售出库审核通过 -> WMS 生成出库单」这条链路会在第一条事件上就因
--   「映射缺失」进入死信——这是设计意图（宁可显式失败），但会让 V101 无法演示。
--   本迁移灌入模拟映射行，让链路在演示环境可跑通。
--
-- 这是一份「演示用模拟数据」，不是生产数据：
--   1. 结构（V101）与数据（V102）刻意分开：结构进生产，数据不进生产。
--   2. 引用的 id 是假设：假设 ERP 产品 1~12 / 仓库 1、2 / 客户 1、2 与
--      WMS SKU 1~12 / 仓库 1、2 / 往来单位 1、2 已在各自系统中存在且顺序一致。
--      若实际 id 不同，请按实际 id 修改本文件后重灌（ON DUPLICATE KEY UPDATE 可安全重跑），
--      或走 MDM 维护界面录入。
--   3. 「同号映射」（ERP 产品 1 -> WMS SKU 1）只是演示便利，绝不表示两套主键存在
--      任何隐含等价关系；文件末尾另放了 2 条非同号映射用于证明映射是真实生效的。
--   4. id 从 8000000000000000001 起（与既有模拟数据约定的 id_base 一致），
--      显式插入会把表的 AUTO_INCREMENT 计数器推到该区间；禁止入生产。
--   5. tenant_id=0：ERP/WMS 主表当前没有 tenant_id 列，V101 链路统一按默认租户 0 运行，
--      故种子也落 0；这与「其他模拟数据用 tenant_id=1」的约定不同，原因是
--      运行时取不到租户列，落 1 会导致链路查不到映射。
--
-- 兼容性：
--   * INSERT ... ON DUPLICATE KEY UPDATE，可重复执行且不产生重复行。
--   * 只更新 target_id / status / remark，不动其他列。
-- ============================================================

-- ------------------------------------------------------------
-- 1. ERP 产品 -> WMS SKU（12 条同号 + 1 条非同号，共 13 条）
-- ------------------------------------------------------------
INSERT INTO `mdm_cross_ref`
    (`id`, `tenant_id`, `source_system`, `source_entity`, `source_id`, `target_system`, `target_entity`, `target_id`, `status`, `remark`)
VALUES
    (8000000000000000001, 0, 'ERP', 'PRODUCT', '1',  'WMS', 'SKU', '1',  'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000002, 0, 'ERP', 'PRODUCT', '2',  'WMS', 'SKU', '2',  'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000003, 0, 'ERP', 'PRODUCT', '3',  'WMS', 'SKU', '3',  'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000004, 0, 'ERP', 'PRODUCT', '4',  'WMS', 'SKU', '4',  'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000005, 0, 'ERP', 'PRODUCT', '5',  'WMS', 'SKU', '5',  'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000006, 0, 'ERP', 'PRODUCT', '6',  'WMS', 'SKU', '6',  'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000007, 0, 'ERP', 'PRODUCT', '7',  'WMS', 'SKU', '7',  'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000008, 0, 'ERP', 'PRODUCT', '8',  'WMS', 'SKU', '8',  'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000009, 0, 'ERP', 'PRODUCT', '9',  'WMS', 'SKU', '9',  'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000010, 0, 'ERP', 'PRODUCT', '10', 'WMS', 'SKU', '10', 'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000011, 0, 'ERP', 'PRODUCT', '11', 'WMS', 'SKU', '11', 'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000012, 0, 'ERP', 'PRODUCT', '12', 'WMS', 'SKU', '12', 'VALID', 'FAKE-模拟数据：产品映射（演示同号）'),
    (8000000000000000013, 0, 'ERP', 'PRODUCT', '101', 'WMS', 'SKU', '201', 'VALID', 'FAKE-模拟数据：产品映射（非同号，证明映射真实生效）')
ON DUPLICATE KEY UPDATE
    `target_id` = VALUES(`target_id`),
    `status`    = VALUES(`status`),
    `remark`    = VALUES(`remark`);

-- ------------------------------------------------------------
-- 2. ERP 仓库 -> WMS 仓库（2 条同号 + 1 条非同号，共 3 条）
-- ------------------------------------------------------------
INSERT INTO `mdm_cross_ref`
    (`id`, `tenant_id`, `source_system`, `source_entity`, `source_id`, `target_system`, `target_entity`, `target_id`, `status`, `remark`)
VALUES
    (8000000000000000014, 0, 'ERP', 'WAREHOUSE', '1', 'WMS', 'WAREHOUSE', '1',  'VALID', 'FAKE-模拟数据：仓库映射（演示同号）'),
    (8000000000000000015, 0, 'ERP', 'WAREHOUSE', '2', 'WMS', 'WAREHOUSE', '2',  'VALID', 'FAKE-模拟数据：仓库映射（演示同号）'),
    (8000000000000000016, 0, 'ERP', 'WAREHOUSE', '9', 'WMS', 'WAREHOUSE', '12', 'VALID', 'FAKE-模拟数据：仓库映射（非同号，证明映射真实生效）')
ON DUPLICATE KEY UPDATE
    `target_id` = VALUES(`target_id`),
    `status`    = VALUES(`status`),
    `remark`    = VALUES(`remark`);

-- ------------------------------------------------------------
-- 3. ERP 客户 -> WMS 往来单位（2 条，可选映射：缺失时出库单不带客户，不阻断链路）
-- ------------------------------------------------------------
INSERT INTO `mdm_cross_ref`
    (`id`, `tenant_id`, `source_system`, `source_entity`, `source_id`, `target_system`, `target_entity`, `target_id`, `status`, `remark`)
VALUES
    (8000000000000000017, 0, 'ERP', 'CUSTOMER', '1', 'WMS', 'MERCHANT', '1', 'VALID', 'FAKE-模拟数据：客户映射（可选）'),
    (8000000000000000018, 0, 'ERP', 'CUSTOMER', '2', 'WMS', 'MERCHANT', '2', 'VALID', 'FAKE-模拟数据：客户映射（可选）')
ON DUPLICATE KEY UPDATE
    `target_id` = VALUES(`target_id`),
    `status`    = VALUES(`status`),
    `remark`    = VALUES(`remark`);

-- 回滚：模拟数据可直接按 id 区间删除；
-- DELETE FROM mdm_cross_ref WHERE id BETWEEN 8000000000000000001 AND 8000000000000000018;
-- 注意：删除后链路会因「映射缺失」进入死信，属预期行为，请先停写或先补真实映射。
