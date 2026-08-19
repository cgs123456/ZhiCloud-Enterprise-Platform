package cn.iocoder.yudao.module.wms.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * WMS 错误码枚举类
 * <p>
 * wms 系统，使用 1-060-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== WMS 基础数据-仓库 1-060-100-000 ==========
    ErrorCode WAREHOUSE_NOT_EXISTS = new ErrorCode(1_060_100_000, "仓库不存在");
    ErrorCode WAREHOUSE_NAME_DUPLICATE = new ErrorCode(1_060_100_001, "仓库名称重复");
    ErrorCode WAREHOUSE_CODE_DUPLICATE = new ErrorCode(1_060_100_002, "仓库编号重复");
    ErrorCode WAREHOUSE_HAS_ORDER = new ErrorCode(1_060_100_004, "删除失败！仓库已被{}使用！");
    ErrorCode WAREHOUSE_HAS_INVENTORY = new ErrorCode(1_060_100_005, "删除失败！仓库已存在库存余额！");

    // ========== WMS 基础数据-商品分类 1-060-102-000 ==========
    ErrorCode ITEM_CATEGORY_NOT_EXISTS = new ErrorCode(1_060_102_000, "商品分类不存在");
    ErrorCode ITEM_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_060_102_001, "商品分类名称重复");
    ErrorCode ITEM_CATEGORY_PARENT_NOT_EXISTS = new ErrorCode(1_060_102_002, "父商品分类不存在");
    ErrorCode ITEM_CATEGORY_PARENT_ERROR = new ErrorCode(1_060_102_003, "不能设置自己为父商品分类");
    ErrorCode ITEM_CATEGORY_PARENT_IS_CHILD = new ErrorCode(1_060_102_004, "不能设置自己的子商品分类为父商品分类");
    ErrorCode ITEM_CATEGORY_HAS_CHILDREN = new ErrorCode(1_060_102_005, "删除失败！请先删除该分类下的子分类！");
    ErrorCode ITEM_CATEGORY_HAS_ITEM = new ErrorCode(1_060_102_006, "删除失败！分类已被商品使用！");
    ErrorCode ITEM_CATEGORY_CODE_DUPLICATE = new ErrorCode(1_060_102_007, "商品分类编号重复");

    // ========== WMS 基础数据-商品品牌 1-060-103-000 ==========
    ErrorCode ITEM_BRAND_NOT_EXISTS = new ErrorCode(1_060_103_000, "商品品牌不存在");
    ErrorCode ITEM_BRAND_HAS_ITEM = new ErrorCode(1_060_103_001, "删除失败！品牌已被商品使用！");
    ErrorCode ITEM_BRAND_CODE_DUPLICATE = new ErrorCode(1_060_103_002, "商品品牌编号重复");
    ErrorCode ITEM_BRAND_NAME_DUPLICATE = new ErrorCode(1_060_103_003, "商品品牌名称重复");

    // ========== WMS 基础数据-商品 1-060-104-000 ==========
    ErrorCode ITEM_NOT_EXISTS = new ErrorCode(1_060_104_000, "商品不存在");
    ErrorCode ITEM_NAME_DUPLICATE = new ErrorCode(1_060_104_001, "商品名称重复");
    ErrorCode ITEM_CODE_DUPLICATE = new ErrorCode(1_060_104_007, "商品编号重复");
    ErrorCode ITEM_SKU_REQUIRED = new ErrorCode(1_060_104_002, "至少包含一个商品规格");
    ErrorCode ITEM_SKU_NAME_DUPLICATE = new ErrorCode(1_060_104_003, "商品规格名称【{}】重复");
    ErrorCode ITEM_SKU_NOT_EXISTS = new ErrorCode(1_060_104_004, "商品规格不存在");
    ErrorCode ITEM_SKU_HAS_INVENTORY = new ErrorCode(1_060_104_005, "删除失败！商品规格【{}】已被库存业务使用！");
    ErrorCode ITEM_SKU_HAS_ORDER = new ErrorCode(1_060_104_006, "删除失败！商品规格【{}】已被{}使用！");

    // ========== WMS 基础数据-往来企业 1-060-105-000 ==========
    ErrorCode MERCHANT_NOT_EXISTS = new ErrorCode(1_060_105_000, "往来企业不存在");
    ErrorCode MERCHANT_NOT_SUPPLIER = new ErrorCode(1_060_105_001, "往来企业必须是供应商或客户/供应商类型");
    ErrorCode MERCHANT_NOT_CUSTOMER = new ErrorCode(1_060_105_002, "往来企业必须是客户或客户/供应商类型");
    ErrorCode MERCHANT_HAS_ORDER = new ErrorCode(1_060_105_003, "删除失败！往来企业已被{}使用！");
    ErrorCode MERCHANT_CODE_DUPLICATE = new ErrorCode(1_060_105_004, "往来企业编号重复");
    ErrorCode MERCHANT_NAME_DUPLICATE = new ErrorCode(1_060_105_005, "往来企业名称重复");

    // ========== WMS 基础数据-库区 1-060-106-000 ==========
    ErrorCode ZONE_NOT_EXISTS = new ErrorCode(1_060_106_000, "库区不存在");
    ErrorCode ZONE_CODE_DUPLICATE = new ErrorCode(1_060_106_001, "库区编号重复");
    ErrorCode ZONE_NAME_DUPLICATE = new ErrorCode(1_060_106_002, "库区名称重复");
    ErrorCode ZONE_HAS_LOCATIONS = new ErrorCode(1_060_106_003, "删除失败！库区下存在库位，请先删除库位！");

    // ========== WMS 基础数据-库位 1-060-107-000 ==========
    ErrorCode LOCATION_NOT_EXISTS = new ErrorCode(1_060_107_000, "库位不存在");
    ErrorCode LOCATION_CODE_DUPLICATE = new ErrorCode(1_060_107_001, "库位编号重复");
    ErrorCode LOCATION_BARCODE_DUPLICATE = new ErrorCode(1_060_107_002, "库位条码重复");
    ErrorCode LOCATION_HAS_INVENTORY = new ErrorCode(1_060_107_003, "删除失败！库位已存在库存余额！");
    ErrorCode LOCATION_STATUS_INVALID = new ErrorCode(1_060_107_004, "库位状态不合法");

    // ========== WMS 入库单 1-060-200-000 ==========
    ErrorCode RECEIPT_ORDER_NOT_EXISTS = new ErrorCode(1_060_200_000, "入库单不存在");
    ErrorCode RECEIPT_ORDER_NO_DUPLICATE = new ErrorCode(1_060_200_001, "入库单号重复");
    ErrorCode RECEIPT_ORDER_STATUS_NOT_PREPARE = new ErrorCode(1_060_200_002, "入库单状态不是草稿，不能操作");
    ErrorCode RECEIPT_ORDER_DETAIL_REQUIRED = new ErrorCode(1_060_200_003, "入库单至少包含一条明细");
    ErrorCode RECEIPT_ORDER_STATUS_NOT_DELETABLE = new ErrorCode(1_060_200_005, "入库单状态不是草稿或已作废，不能删除");
    ErrorCode RECEIPT_ORDER_DETAIL_NOT_EXISTS = new ErrorCode(1_060_200_007, "入库单明细不存在");
    ErrorCode RECEIPT_ORDER_QC_NOT_QUALIFIED = new ErrorCode(1_060_200_008,
            "入库单已关联质检业务（qcBizId={}），但对应 QMS 检验单未通过，禁止入库");

    // ========== WMS 出库单 1-060-201-000 ==========
    ErrorCode SHIPMENT_ORDER_NOT_EXISTS = new ErrorCode(1_060_201_000, "出库单不存在");
    ErrorCode SHIPMENT_ORDER_NO_DUPLICATE = new ErrorCode(1_060_201_001, "出库单号重复");
    ErrorCode SHIPMENT_ORDER_STATUS_NOT_PREPARE = new ErrorCode(1_060_201_002, "出库单状态不是草稿，不能操作");
    ErrorCode SHIPMENT_ORDER_DETAIL_REQUIRED = new ErrorCode(1_060_201_003, "出库单至少包含一条明细");
    ErrorCode SHIPMENT_ORDER_STATUS_NOT_DELETABLE = new ErrorCode(1_060_201_005, "出库单状态不是草稿或已作废，不能删除");
    ErrorCode SHIPMENT_ORDER_DETAIL_NOT_EXISTS = new ErrorCode(1_060_201_007, "出库单明细不存在");
    ErrorCode SHIPMENT_ORDER_FAST_COMPLETE_DISABLED = new ErrorCode(1_060_201_008,
            "出库单快捷完成（跳过拣货/复核/打包/发运）已禁用，请走完整状态机流程：开始拣货→完成拣货→复核→打包→发运→完成");

    // ========== WMS 移库单 1-060-202-000 ==========
    ErrorCode MOVEMENT_ORDER_NOT_EXISTS = new ErrorCode(1_060_202_000, "移库单不存在");
    ErrorCode MOVEMENT_ORDER_NO_DUPLICATE = new ErrorCode(1_060_202_001, "移库单号重复");
    ErrorCode MOVEMENT_ORDER_STATUS_NOT_PREPARE = new ErrorCode(1_060_202_002, "移库单状态不是草稿，不能操作");
    ErrorCode MOVEMENT_ORDER_DETAIL_REQUIRED = new ErrorCode(1_060_202_003, "移库单至少包含一条明细");
    ErrorCode MOVEMENT_ORDER_STATUS_NOT_DELETABLE = new ErrorCode(1_060_202_005, "移库单状态不是草稿或已作废，不能删除");
    ErrorCode MOVEMENT_ORDER_DETAIL_NOT_EXISTS = new ErrorCode(1_060_202_006, "移库单明细不存在");
    ErrorCode MOVEMENT_ORDER_WAREHOUSE_SAME = new ErrorCode(1_060_202_007, "来源仓库和目标仓库不能相同");

    // ========== WMS 盘库单 1-060-203-000 ==========
    ErrorCode CHECK_ORDER_NOT_EXISTS = new ErrorCode(1_060_203_000, "盘库单不存在");
    ErrorCode CHECK_ORDER_NO_DUPLICATE = new ErrorCode(1_060_203_001, "盘库单号重复");
    ErrorCode CHECK_ORDER_STATUS_NOT_PREPARE = new ErrorCode(1_060_203_002, "盘库单状态不是草稿，不能操作");
    ErrorCode CHECK_ORDER_DETAIL_REQUIRED = new ErrorCode(1_060_203_003, "盘库单至少包含一条明细");
    ErrorCode CHECK_ORDER_STATUS_NOT_DELETABLE = new ErrorCode(1_060_203_005, "盘库单状态不是草稿或已作废，不能删除");
    ErrorCode CHECK_ORDER_DETAIL_NOT_EXISTS = new ErrorCode(1_060_203_006, "盘库单明细不存在");
    ErrorCode CHECK_ORDER_INVENTORY_CHANGED = new ErrorCode(1_060_203_007, "盘库单库存已变化，请重新加载库存后再完成");

    // ========== WMS 库存 1-060-300-000 ==========
    ErrorCode INVENTORY_QUANTITY_NOT_ENOUGH = new ErrorCode(1_060_300_000,
            "库存不足，商品：{}，商品规格：{}，仓库：{}，当前库存：{}，变更数量：{}");
    ErrorCode INVENTORY_NOT_EXISTS = new ErrorCode(1_060_300_001, "库存不存在");

    // ========== WMS 库存批次 1-060-301-000 ==========
    ErrorCode INVENTORY_BATCH_NOT_EXISTS = new ErrorCode(1_060_301_000, "库存批次不存在");
    ErrorCode INVENTORY_BATCH_QUANTITY_NOT_ENOUGH = new ErrorCode(1_060_301_001,
            "批次可用数量不足，批次号：{}，当前可用：{}，需要数量：{}");
    ErrorCode INVENTORY_BATCH_STATUS_NOT_AVAILABLE = new ErrorCode(1_060_301_002,
            "批次状态不可用，批次号：{}，当前状态：{}");
    ErrorCode INVENTORY_BATCH_EXPIRED = new ErrorCode(1_060_301_003, "批次已过期，批次号：{}");
    ErrorCode INVENTORY_BATCH_PUTAWAY_EXPIRED = new ErrorCode(1_060_301_004, "上架批次已过期，批次号：{}，过期日期：{}");
    ErrorCode INVENTORY_BATCH_FEFO_INSUFFICIENT = new ErrorCode(1_060_301_005,
            "FEFO 批次分配不足，SKU：{}，仓库：{}，需求：{}，可用：{}");
    ErrorCode INVENTORY_BATCH_LOCK_FAILED = new ErrorCode(1_060_301_006, "批次锁定失败，批次号：{}，原因：{}");

    // ========== WMS 上架 Slotting 1-060-302-000 ==========
    ErrorCode SLOTTING_NO_SUITABLE_WAREHOUSE = new ErrorCode(1_060_302_000, "无合适上架仓库，SKU：{}");
    ErrorCode SLOTTING_BATCH_MISMATCH = new ErrorCode(1_060_302_001, "批次信息不匹配，目标库存批次号：{}，当前批次号：{}");

    // ========== WMS 波次单 1-060-204-000 ==========
    ErrorCode WAVE_ORDER_NOT_EXISTS = new ErrorCode(1_060_204_000, "波次单不存在");
    ErrorCode WAVE_ORDER_NO_DUPLICATE = new ErrorCode(1_060_204_001, "波次单号重复");
    ErrorCode WAVE_ORDER_STATUS_NOT_PREPARE = new ErrorCode(1_060_204_002, "波次单状态不是草稿，不能操作");
    ErrorCode WAVE_ORDER_DETAIL_REQUIRED = new ErrorCode(1_060_204_003, "波次单至少包含一条明细");
    ErrorCode WAVE_ORDER_STATUS_NOT_DELETABLE = new ErrorCode(1_060_204_005, "波次单状态不是草稿或已作废，不能删除");
    ErrorCode WAVE_ORDER_DETAIL_NOT_EXISTS = new ErrorCode(1_060_204_006, "波次单明细不存在");
    ErrorCode WAVE_ORDER_NO_SHIPMENT_SELECTED = new ErrorCode(1_060_204_007, "未选择任何出库单，无法生成波次");

    // ========== WMS 基础数据-序列号 SN（1-060-108-000） ==========
    ErrorCode SN_NOT_EXISTS = new ErrorCode(1_060_108_000, "序列号不存在");
    ErrorCode SN_DUPLICATE = new ErrorCode(1_060_108_001, "序列号已存在：{}");
    ErrorCode SN_STATUS_INVALID = new ErrorCode(1_060_108_002, "序列号状态不允许此操作，当前状态：{}");
    ErrorCode SN_NOT_BOUND = new ErrorCode(1_060_108_003, "序列号未绑定库存，无法出库");
    ErrorCode SN_ALREADY_BOUND = new ErrorCode(1_060_108_004, "序列号已绑定库存，无法重复绑定");
    ErrorCode SN_GENERATE_QUANTITY_INVALID = new ErrorCode(1_060_108_005, "生成数量必须大于 0");

    // ========== WMS 安全库存配置 1-060-109-000 ==========
    ErrorCode SAFETY_STOCK_CONFIG_NOT_EXISTS = new ErrorCode(1_060_109_000, "安全库存配置不存在");
    ErrorCode SAFETY_STOCK_CONFIG_DUPLICATE = new ErrorCode(1_060_109_001, "安全库存配置已存在（仓库 + SKU 唯一）");

    // ========== WMS 库存预警 1-060-110-000 ==========
    ErrorCode INVENTORY_ALERT_NOT_EXISTS = new ErrorCode(1_060_110_000, "库存预警不存在");
    ErrorCode INVENTORY_ALERT_STATUS_INVALID = new ErrorCode(1_060_110_001, "库存预警状态不合法，无法操作");

    // ========== WMS 盘点类型/循环盘点 1-060-111-000 ==========
    ErrorCode CHECK_ORDER_TYPE_INVALID = new ErrorCode(1_060_111_000, "盘点类型不合法");
    ErrorCode CHECK_CYCLE_PLAN_NOT_EXISTS = new ErrorCode(1_060_111_001, "循环盘点计划不存在");
    ErrorCode CHECK_CYCLE_PLAN_DUPLICATE = new ErrorCode(1_060_111_002, "循环盘点计划已存在（仓库 + SKU 唯一）");
    ErrorCode CHECK_CYCLE_PLAN_ABC_INVALID = new ErrorCode(1_060_111_003, "ABC 分类不合法，仅支持 A/B/C");

    // ========== WMS 出库单状态机扩展 1-060-112-000 ==========
    ErrorCode SHIPMENT_ORDER_STATUS_INVALID = new ErrorCode(1_060_112_000, "出库单状态流转不合法，当前状态：{}，目标状态：{}");
    ErrorCode SHIPMENT_ORDER_NOT_PICKABLE = new ErrorCode(1_060_112_001, "出库单不在可拣货状态");
    ErrorCode SHIPMENT_ORDER_NOT_REVIEWABLE = new ErrorCode(1_060_112_002, "出库单不在可复核状态");
    ErrorCode SHIPMENT_ORDER_NOT_PACKABLE = new ErrorCode(1_060_112_003, "出库单不在可打包状态");
    ErrorCode SHIPMENT_ORDER_NOT_SHIPPABLE = new ErrorCode(1_060_112_004, "出库单不在可发货状态");
    ErrorCode SHIPMENT_ORDER_NOT_FINISHABLE = new ErrorCode(1_060_112_005, "出库单不在可完成状态");

    // ========== WMS 越库单 1-060-205-000 ==========
    ErrorCode CROSS_DOCK_ORDER_NOT_EXISTS = new ErrorCode(1_060_205_000, "越库单不存在");
    ErrorCode CROSS_DOCK_ORDER_NO_DUPLICATE = new ErrorCode(1_060_205_001, "越库单号重复");
    ErrorCode CROSS_DOCK_ORDER_STATUS_NOT_PREPARE = new ErrorCode(1_060_205_002, "越库单状态不是待收货，不能操作");
    ErrorCode CROSS_DOCK_ORDER_DETAIL_REQUIRED = new ErrorCode(1_060_205_003, "越库单至少包含一条明细");
    ErrorCode CROSS_DOCK_ORDER_STATUS_NOT_DELETABLE = new ErrorCode(1_060_205_005, "越库单状态不是待收货或已取消，不能删除");
    ErrorCode CROSS_DOCK_ORDER_DETAIL_NOT_EXISTS = new ErrorCode(1_060_205_007, "越库单明细不存在");
    ErrorCode CROSS_DOCK_ORDER_STATUS_NOT_RECEIVABLE = new ErrorCode(1_060_205_008, "越库单不在待收货状态，不能确认收货");
    ErrorCode CROSS_DOCK_ORDER_STATUS_NOT_COMPLETABLE = new ErrorCode(1_060_205_009, "越库单不在已分配状态，不能完成");

    // ========== WMS 3PL 计费-合同 1-060-400-000 ==========
    ErrorCode BILLING_CONTRACT_NOT_EXISTS = new ErrorCode(1_060_400_000, "计费合同不存在");
    ErrorCode BILLING_CONTRACT_NO_DUPLICATE = new ErrorCode(1_060_400_001, "计费合同号重复");
    ErrorCode BILLING_CONTRACT_ITEM_NOT_EXISTS = new ErrorCode(1_060_400_002, "计费合同条款不存在");
    ErrorCode BILLING_CONTRACT_ITEM_REQUIRED = new ErrorCode(1_060_400_003, "计费合同至少包含一条条款");
    ErrorCode BILLING_CONTRACT_NOT_DELETABLE = new ErrorCode(1_060_400_004, "计费合同状态不是失效或已终止，不能删除");

    // ========== WMS 3PL 计费-账单 1-060-401-000 ==========
    ErrorCode BILLING_BILL_NOT_EXISTS = new ErrorCode(1_060_401_000, "计费账单不存在");
    ErrorCode BILLING_BILL_NO_DUPLICATE = new ErrorCode(1_060_401_001, "计费账单号重复");
    ErrorCode BILLING_BILL_LINE_NOT_EXISTS = new ErrorCode(1_060_401_002, "计费账单明细不存在");
    ErrorCode BILLING_BILL_STATUS_NOT_DRAFT = new ErrorCode(1_060_401_003, "计费账单状态不是草稿，不能操作");
    ErrorCode BILLING_BILL_STATUS_NOT_DELETABLE = new ErrorCode(1_060_401_004, "计费账单状态不是草稿或已付款，不能删除");
    ErrorCode BILLING_CONTRACT_NOT_EFFECTIVE = new ErrorCode(1_060_401_005, "计费合同不在生效状态");

    // ========== WMS 月台管理 1-060-113-000 ==========
    ErrorCode DOCK_NOT_EXISTS = new ErrorCode(1_060_113_000, "月台不存在");
    ErrorCode DOCK_CODE_DUPLICATE = new ErrorCode(1_060_113_001, "月台编号重复");
    ErrorCode DOCK_HAS_ASN = new ErrorCode(1_060_113_002, "删除失败！月台已被 ASN 到货通知使用！");

    // ========== WMS ASN 到货通知 1-060-206-000 ==========
    ErrorCode ASN_ORDER_NOT_EXISTS = new ErrorCode(1_060_206_000, "ASN 到货通知不存在");
    ErrorCode ASN_ORDER_NO_DUPLICATE = new ErrorCode(1_060_206_001, "ASN 到货通知号重复");
    ErrorCode ASN_ORDER_STATUS_NOT_PENDING = new ErrorCode(1_060_206_002, "ASN 到货通知状态不是待到货，不能操作");
    ErrorCode ASN_ORDER_STATUS_NOT_ARRIVED = new ErrorCode(1_060_206_003, "ASN 到货通知状态不是已到货，不能操作");
    ErrorCode ASN_ORDER_DETAIL_REQUIRED = new ErrorCode(1_060_206_004, "ASN 到货通知至少包含一条明细");
    ErrorCode ASN_ORDER_DETAIL_NOT_EXISTS = new ErrorCode(1_060_206_005, "ASN 到货通知明细不存在");

    // ========== WMS 拣货任务 1-060-207-000 ==========
    ErrorCode PICK_TASK_NOT_EXISTS = new ErrorCode(1_060_207_000, "拣货任务不存在");
    ErrorCode PICK_TASK_STATUS_NOT_PICKABLE = new ErrorCode(1_060_207_001, "拣货任务状态不在可拣货状态");
    ErrorCode PICK_TASK_GENERATE_NO_DETAIL = new ErrorCode(1_060_207_002, "出库单无明细，无法生成拣货任务");
    ErrorCode PICK_TASK_SHIPMENT_NOT_PICKABLE = new ErrorCode(1_060_207_003, "出库单不在可拣货状态");
    ErrorCode PICK_TASK_NOT_YOURS = new ErrorCode(1_060_207_004, "拣货任务归属当前登录用户，无权操作");
    ErrorCode WMS_PICK_QUANTITY_NEGATIVE = new ErrorCode(1_060_207_005, "拣货数量不能为负数");
    ErrorCode WMS_PICK_QUANTITY_EXCEEDS = new ErrorCode(1_060_207_006, "拣货数量不能超过应拣数量");

    // ========== WMS 批次效期预警 1-060-303-000 ==========
    ErrorCode BATCH_EXPIRY_ALERT_NOT_EXISTS = new ErrorCode(1_060_303_000, "批次效期预警不存在");
    ErrorCode BATCH_EXPIRY_ALERT_SCAN_FAILED = new ErrorCode(1_060_303_001, "批次效期扫描失败：{}");

}
