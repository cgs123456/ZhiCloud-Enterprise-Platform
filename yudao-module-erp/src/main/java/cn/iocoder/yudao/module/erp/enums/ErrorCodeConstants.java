package cn.iocoder.yudao.module.erp.enums;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

/**
 * ERP 错误码枚举类
 * <p>
 * erp 系统，使用 1-030-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== ERP 供应商（1-030-100-000） ==========
    ErrorCode SUPPLIER_NOT_EXISTS = new ErrorCode(1_030_100_000, "供应商不存在");
    ErrorCode SUPPLIER_NOT_ENABLE = new ErrorCode(1_030_000_000, "供应商({})未启用");

    // ========== ERP 采购订单（1-030-101-000） ==========
    ErrorCode PURCHASE_ORDER_NOT_EXISTS = new ErrorCode(1_030_101_000, "采购订单不存在");
    ErrorCode PURCHASE_ORDER_DELETE_FAIL_APPROVE = new ErrorCode(1_030_101_001, "采购订单({})已审核，无法删除");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL = new ErrorCode(1_030_101_002, "反审核失败，只有已审核的采购订单才能反审核");
    ErrorCode PURCHASE_ORDER_APPROVE_FAIL = new ErrorCode(1_030_101_003, "审核失败，只有未审核的采购订单才能审核");
    ErrorCode PURCHASE_ORDER_NO_EXISTS = new ErrorCode(1_030_101_004, "生成采购单号失败，请重新提交");
    ErrorCode PURCHASE_ORDER_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_101_005, "采购订单({})已审核，无法修改");
    ErrorCode PURCHASE_ORDER_NOT_APPROVE = new ErrorCode(1_030_101_006, "采购订单未审核，无法操作");
    ErrorCode PURCHASE_ORDER_ITEM_IN_FAIL_PRODUCT_EXCEED = new ErrorCode(1_030_101_007, "采购订单项({})超过最大允许入库数量({})");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL_EXISTS_IN = new ErrorCode(1_030_101_008, "反审核失败，已存在对应的采购入库单");
ErrorCode PURCHASE_ORDER_ITEM_RETURN_FAIL_IN_EXCEED = new ErrorCode(1_030_101_009, "采购订单项({})超过最大允许退货数量({})");
    ErrorCode PURCHASE_ORDER_PROCESS_FAIL_EXISTS_RETURN = new ErrorCode(1_030_101_010, "反审核失败，已存在对应的采购退货单");

    // ========== ERP 采购入库（1-030-102-000） ==========
    ErrorCode PURCHASE_IN_NOT_EXISTS = new ErrorCode(1_030_102_000, "采购入库单不存在");
    ErrorCode PURCHASE_IN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_102_001, "采购入库单({})已审核，无法删除");
    ErrorCode PURCHASE_IN_PROCESS_FAIL = new ErrorCode(1_030_102_002, "反审核失败，只有已审核的入库单才能反审核");
    ErrorCode PURCHASE_IN_APPROVE_FAIL = new ErrorCode(1_030_102_003, "审核失败，只有未审核的入库单才能审核");
    ErrorCode PURCHASE_IN_NO_EXISTS = new ErrorCode(1_030_102_004, "生成入库单失败，请重新提交");
    ErrorCode PURCHASE_IN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_102_005, "采购入库单({})已审核，无法修改");
    ErrorCode PURCHASE_IN_NOT_APPROVE = new ErrorCode(1_030_102_006, "采购入库单未审核，无法操作");
    ErrorCode PURCHASE_IN_FAIL_PAYMENT_PRICE_EXCEED = new ErrorCode(1_030_102_007, "付款金额({})超过采购入库单总金额({})");
    ErrorCode PURCHASE_IN_PROCESS_FAIL_EXISTS_PAYMENT = new ErrorCode(1_030_102_008, "反审核失败，已存在对应的付款单");

    // ========== ERP 采购退货（1-030-103-000） ==========
    ErrorCode PURCHASE_RETURN_NOT_EXISTS = new ErrorCode(1_030_103_000, "采购退货单不存在");
    ErrorCode PURCHASE_RETURN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_103_001, "采购退货单({})已审核，无法删除");
    ErrorCode PURCHASE_RETURN_PROCESS_FAIL = new ErrorCode(1_030_103_002, "反审核失败，只有已审核的退货单才能反审核");
    ErrorCode PURCHASE_RETURN_APPROVE_FAIL = new ErrorCode(1_030_103_003, "审核失败，只有未审核的退货单才能审核");
    ErrorCode PURCHASE_RETURN_NO_EXISTS = new ErrorCode(1_030_103_004, "生成退货单失败，请重新提交");
    ErrorCode PURCHASE_RETURN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_103_005, "采购退货单({})已审核，无法修改");
    ErrorCode PURCHASE_RETURN_NOT_APPROVE = new ErrorCode(1_030_103_006, "采购退货单未审核，无法操作");
    ErrorCode PURCHASE_RETURN_FAIL_REFUND_PRICE_EXCEED = new ErrorCode(1_030_103_007, "退款金额({})超过采购退货单总金额({})");
    ErrorCode PURCHASE_RETURN_PROCESS_FAIL_EXISTS_REFUND = new ErrorCode(1_030_103_008, "反审核失败，已存在对应的退款单");

    // ========== ERP 采购询价单（1-030-104-000） ==========
    ErrorCode PURCHASE_INQUIRY_NOT_EXISTS = new ErrorCode(1_030_104_000, "采购询价单不存在");
    ErrorCode PURCHASE_INQUIRY_NO_EXISTS = new ErrorCode(1_030_104_001, "生成询价单号失败，请重新提交");
    ErrorCode PURCHASE_INQUIRY_DELETE_FAIL = new ErrorCode(1_030_104_002, "采购询价单({})非草稿状态，无法删除");
    ErrorCode PURCHASE_INQUIRY_UPDATE_FAIL = new ErrorCode(1_030_104_003, "采购询价单({})非草稿状态，无法修改");
    ErrorCode PURCHASE_INQUIRY_SUBMIT_FAIL = new ErrorCode(1_030_104_004, "采购询价单({})非草稿状态，无法提交发布");
    ErrorCode PURCHASE_INQUIRY_CLOSE_FAIL = new ErrorCode(1_030_104_005, "采购询价单({})非已发布状态，无法关闭");
    ErrorCode PURCHASE_INQUIRY_CONVERT_FAIL_QUOTE = new ErrorCode(1_030_104_006, "采购询价单({})对应的供应商({})不存在已报价的报价单");
    ErrorCode PURCHASE_INQUIRY_CONVERT_FAIL_STATUS = new ErrorCode(1_030_104_007, "采购询价单({})非已比价状态，无法转采购订单");

    // ========== ERP 采购报价单（1-030-105-000） ==========
    ErrorCode PURCHASE_QUOTE_NOT_EXISTS = new ErrorCode(1_030_105_000, "采购报价单不存在");
    ErrorCode PURCHASE_QUOTE_NO_EXISTS = new ErrorCode(1_030_105_001, "生成报价单号失败，请重新提交");
    ErrorCode PURCHASE_QUOTE_DELETE_FAIL = new ErrorCode(1_030_105_002, "采购报价单({})已采纳或已拒绝，无法删除");
    ErrorCode PURCHASE_QUOTE_UPDATE_FAIL = new ErrorCode(1_030_105_003, "采购报价单({})已采纳或已拒绝，无法修改");
    ErrorCode PURCHASE_QUOTE_SUPPLIER_DUPLICATE = new ErrorCode(1_030_105_004, "询价单({})下供应商({})已存在报价单");
    ErrorCode PURCHASE_QUOTE_NOT_QUOTED = new ErrorCode(1_030_105_005, "采购报价单({})非已报价状态");

    // ========== ERP 采购比价单（1-030-106-000） ==========
    ErrorCode PURCHASE_COMPARE_NOT_EXISTS = new ErrorCode(1_030_106_000, "采购比价单不存在");
    ErrorCode PURCHASE_COMPARE_NO_EXISTS = new ErrorCode(1_030_106_001, "生成比价单号失败，请重新提交");
    ErrorCode PURCHASE_COMPARE_FAIL_NO_QUOTE = new ErrorCode(1_030_106_002, "询价单({})暂无已报价的报价单，无法生成比价单");
    ErrorCode PURCHASE_COMPARE_FAIL_EXISTS = new ErrorCode(1_030_106_003, "询价单({})已存在比价单，无法重复生成");

    // ========== ERP 客户（1-030-200-000）==========
    ErrorCode CUSTOMER_NOT_EXISTS = new ErrorCode(1_020_200_000, "客户不存在");
    ErrorCode CUSTOMER_NOT_ENABLE = new ErrorCode(1_020_200_001, "客户({})未启用");

    // ========== ERP 销售订单（1-030-201-000） ==========
    ErrorCode SALE_ORDER_NOT_EXISTS = new ErrorCode(1_020_201_000, "销售订单不存在");
    ErrorCode SALE_ORDER_DELETE_FAIL_APPROVE = new ErrorCode(1_020_201_001, "销售订单({})已审核，无法删除");
    ErrorCode SALE_ORDER_PROCESS_FAIL = new ErrorCode(1_020_201_002, "反审核失败，只有已审核的销售订单才能反审核");
    ErrorCode SALE_ORDER_APPROVE_FAIL = new ErrorCode(1_020_201_003, "审核失败，只有未审核的销售订单才能审核");
    ErrorCode SALE_ORDER_NO_EXISTS = new ErrorCode(1_020_201_004, "生成销售单号失败，请重新提交");
    ErrorCode SALE_ORDER_UPDATE_FAIL_APPROVE = new ErrorCode(1_020_201_005, "销售订单({})已审核，无法修改");
    ErrorCode SALE_ORDER_NOT_APPROVE = new ErrorCode(1_020_201_006, "销售订单未审核，无法操作");
    ErrorCode SALE_ORDER_ITEM_OUT_FAIL_PRODUCT_EXCEED = new ErrorCode(1_020_201_007, "销售订单项({})超过最大允许出库数量({})");
    ErrorCode SALE_ORDER_PROCESS_FAIL_EXISTS_OUT = new ErrorCode(1_020_201_008, "反审核失败，已存在对应的销售出库单");
    ErrorCode SALE_ORDER_ITEM_RETURN_FAIL_OUT_EXCEED = new ErrorCode(1_020_201_009, "销售订单项({})超过最大允许退货数量({})");
    ErrorCode SALE_ORDER_PROCESS_FAIL_EXISTS_RETURN = new ErrorCode(1_020_201_010, "反审核失败，已存在对应的销售退货单");
    ErrorCode SALE_ORDER_STOCK_LOCK_FAIL = new ErrorCode(1_020_201_011, "销售订单({})库存锁定失败");
    ErrorCode SALE_ORDER_STOCK_OUT_FAIL = new ErrorCode(1_020_201_012, "销售订单({})库存扣减失败");
    ErrorCode SALE_ORDER_STOCK_RETURN_FAIL = new ErrorCode(1_020_201_013, "销售订单({})库存恢复失败");

    // ========== ERP 销售出库（1-030-202-000） ==========
    ErrorCode SALE_OUT_NOT_EXISTS = new ErrorCode(1_020_202_000, "销售出库单不存在");
    ErrorCode SALE_OUT_DELETE_FAIL_APPROVE = new ErrorCode(1_020_202_001, "销售出库单({})已审核，无法删除");
    ErrorCode SALE_OUT_PROCESS_FAIL = new ErrorCode(1_020_202_002, "反审核失败，只有已审核的出库单才能反审核");
    ErrorCode SALE_OUT_APPROVE_FAIL = new ErrorCode(1_020_202_003, "审核失败，只有未审核的出库单才能审核");
    ErrorCode SALE_OUT_NO_EXISTS = new ErrorCode(1_020_202_004, "生成出库单失败，请重新提交");
    ErrorCode SALE_OUT_UPDATE_FAIL_APPROVE = new ErrorCode(1_020_202_005, "销售出库单({})已审核，无法修改");
    ErrorCode SALE_OUT_NOT_APPROVE = new ErrorCode(1_020_202_006, "销售出库单未审核，无法操作");
    ErrorCode SALE_OUT_FAIL_RECEIPT_PRICE_EXCEED = new ErrorCode(1_020_202_007, "收款金额({})超过销售出库单总金额({})");
    ErrorCode SALE_OUT_PROCESS_FAIL_EXISTS_RECEIPT = new ErrorCode(1_020_202_008, "反审核失败，已存在对应的收款单");

    // ========== ERP 销售退货（1-030-203-000） ==========
    ErrorCode SALE_RETURN_NOT_EXISTS = new ErrorCode(1_020_203_000, "销售退货单不存在");
    ErrorCode SALE_RETURN_DELETE_FAIL_APPROVE = new ErrorCode(1_020_203_001, "销售退货单({})已审核，无法删除");
    ErrorCode SALE_RETURN_PROCESS_FAIL = new ErrorCode(1_020_203_002, "反审核失败，只有已审核的退货单才能反审核");
    ErrorCode SALE_RETURN_APPROVE_FAIL = new ErrorCode(1_020_203_003, "审核失败，只有未审核的退货单才能审核");
    ErrorCode SALE_RETURN_NO_EXISTS = new ErrorCode(1_020_203_004, "生成退货单失败，请重新提交");
    ErrorCode SALE_RETURN_UPDATE_FAIL_APPROVE = new ErrorCode(1_020_203_005, "销售退货单({})已审核，无法修改");
    ErrorCode SALE_RETURN_NOT_APPROVE = new ErrorCode(1_020_203_006, "销售退货单未审核，无法操作");
    ErrorCode SALE_RETURN_FAIL_REFUND_PRICE_EXCEED = new ErrorCode(1_020_203_007, "退款金额({})超过销售退货单总金额({})");
    ErrorCode SALE_RETURN_PROCESS_FAIL_EXISTS_REFUND = new ErrorCode(1_020_203_008, "反审核失败，已存在对应的退款单");

    // ========== ERP 仓库 1-030-400-000 ==========
    ErrorCode WAREHOUSE_NOT_EXISTS = new ErrorCode(1_030_400_000, "仓库不存在");
    ErrorCode WAREHOUSE_NOT_ENABLE = new ErrorCode(1_030_400_001, "仓库({})未启用");

    // ========== ERP 其它入库单 1-030-401-000 ==========
    ErrorCode STOCK_IN_NOT_EXISTS = new ErrorCode(1_030_401_000, "其它入库单不存在");
    ErrorCode STOCK_IN_DELETE_FAIL_APPROVE = new ErrorCode(1_030_401_001, "其它入库单({})已审核，无法删除");
    ErrorCode STOCK_IN_PROCESS_FAIL = new ErrorCode(1_030_401_002, "反审核失败，只有已审核的入库单才能反审核");
    ErrorCode STOCK_IN_APPROVE_FAIL = new ErrorCode(1_030_401_003, "审核失败，只有未审核的入库单才能审核");
    ErrorCode STOCK_IN_NO_EXISTS = new ErrorCode(1_030_401_004, "生成入库单失败，请重新提交");
    ErrorCode STOCK_IN_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_401_005, "其它入库单({})已审核，无法修改");
    ErrorCode STOCK_IN_ITEM_COUNT_ERROR = new ErrorCode(1_030_401_006, "入库项数量必须大于 0");

    // ========== ERP 其它出库单 1-030-402-000 ==========
    ErrorCode STOCK_OUT_NOT_EXISTS = new ErrorCode(1_030_402_000, "其它出库单不存在");
    ErrorCode STOCK_OUT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_402_001, "其它出库单({})已审核，无法删除");
    ErrorCode STOCK_OUT_PROCESS_FAIL = new ErrorCode(1_030_402_002, "反审核失败，只有已审核的出库单才能反审核");
    ErrorCode STOCK_OUT_APPROVE_FAIL = new ErrorCode(1_030_402_003, "审核失败，只有未审核的出库单才能审核");
    ErrorCode STOCK_OUT_NO_EXISTS = new ErrorCode(1_030_402_004, "生成出库单失败，请重新提交");
    ErrorCode STOCK_OUT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_402_005, "其它出库单({})已审核，无法修改");

    // ========== ERP 库存调拨单 1-030-403-000 ==========
    ErrorCode STOCK_MOVE_NOT_EXISTS = new ErrorCode(1_030_000_001, "库存调拨单不存在");
    ErrorCode STOCK_MOVE_DELETE_FAIL_APPROVE = new ErrorCode(1_030_000_002, "库存调拨单({})已审核，无法删除");
    ErrorCode STOCK_MOVE_PROCESS_FAIL = new ErrorCode(1_030_000_003, "反审核失败，只有已审核的调拨单才能反审核");
    ErrorCode STOCK_MOVE_APPROVE_FAIL = new ErrorCode(1_030_000_004, "审核失败，只有未审核的调拨单才能审核");
    ErrorCode STOCK_MOVE_NO_EXISTS = new ErrorCode(1_030_000_005, "生成调拨号失败，请重新提交");
    ErrorCode STOCK_MOVE_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_000_006, "库存调拨单({})已审核，无法修改");

    // ========== ERP 库存盘点单 1-030-403-000 ==========
    ErrorCode STOCK_CHECK_NOT_EXISTS = new ErrorCode(1_030_403_000, "库存盘点单不存在");
    ErrorCode STOCK_CHECK_DELETE_FAIL_APPROVE = new ErrorCode(1_030_403_001, "库存盘点单({})已审核，无法删除");
    ErrorCode STOCK_CHECK_PROCESS_FAIL = new ErrorCode(1_030_403_002, "反审核失败，只有已审核的盘点单才能反审核");
    ErrorCode STOCK_CHECK_APPROVE_FAIL = new ErrorCode(1_030_403_003, "审核失败，只有未审核的盘点单才能审核");
    ErrorCode STOCK_CHECK_NO_EXISTS = new ErrorCode(1_030_403_004, "生成盘点号失败，请重新提交");
    ErrorCode STOCK_CHECK_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_403_005, "库存盘点单({})已审核，无法修改");

    // ========== ERP 产品库存 1-030-404-000 ==========
    ErrorCode STOCK_COUNT_NEGATIVE = new ErrorCode(1_030_404_000, "操作失败，产品({})所在仓库({})的库存：{}，小于变更数量：{}");
    ErrorCode STOCK_COUNT_NEGATIVE2 = new ErrorCode(1_030_404_001, "操作失败，产品({})所在仓库({})的库存不足");

    // ========== ERP 产品 1-030-500-000 ==========
    ErrorCode PRODUCT_NOT_EXISTS = new ErrorCode(1_030_500_000, "产品不存在");
    ErrorCode PRODUCT_NOT_ENABLE = new ErrorCode(1_030_500_001, "产品({})未启用");

    // ========== ERP 产品分类 1-030-501-000 ==========
    ErrorCode PRODUCT_CATEGORY_NOT_EXISTS = new ErrorCode(1_030_501_000, "产品分类不存在");
    ErrorCode PRODUCT_CATEGORY_EXITS_CHILDREN = new ErrorCode(1_030_501_001, "存在存在子产品分类，无法删除");
    ErrorCode PRODUCT_CATEGORY_PARENT_NOT_EXITS = new ErrorCode(1_030_501_002,"父级产品分类不存在");
    ErrorCode PRODUCT_CATEGORY_PARENT_ERROR = new ErrorCode(1_030_501_003, "不能设置自己为父产品分类");
    ErrorCode PRODUCT_CATEGORY_NAME_DUPLICATE = new ErrorCode(1_030_501_004, "已经存在该分类名称的产品分类");
    ErrorCode PRODUCT_CATEGORY_PARENT_IS_CHILD = new ErrorCode(1_030_501_005, "不能设置自己的子分类为父分类");
    ErrorCode PRODUCT_CATEGORY_EXITS_PRODUCT = new ErrorCode(1_030_502_002, "存在产品使用该分类，无法删除");

    // ========== ERP 产品单位 1-030-502-000 ==========
    ErrorCode PRODUCT_UNIT_NOT_EXISTS = new ErrorCode(1_030_502_000, "产品单位不存在");
    ErrorCode PRODUCT_UNIT_NAME_DUPLICATE = new ErrorCode(1_030_502_001, "已存在该名字的产品单位");
    ErrorCode PRODUCT_UNIT_EXITS_PRODUCT = new ErrorCode(1_030_000_007, "存在产品使用该单位，无法删除");

    // ========== ERP 结算账户 1-030-600-000 ==========
    ErrorCode ACCOUNT_NOT_EXISTS = new ErrorCode(1_030_600_000, "结算账户不存在");
    ErrorCode ACCOUNT_NOT_ENABLE = new ErrorCode(1_030_600_001, "结算账户({})未启用");

    // ========== ERP 付款单 1-030-601-000 ==========
    ErrorCode FINANCE_PAYMENT_NOT_EXISTS = new ErrorCode(1_030_601_000, "付款单不存在");
    ErrorCode FINANCE_PAYMENT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_601_001, "付款单({})已审核，无法删除");
    ErrorCode FINANCE_PAYMENT_PROCESS_FAIL = new ErrorCode(1_030_601_002, "反审核失败，只有已审核的付款单才能反审核");
    ErrorCode FINANCE_PAYMENT_APPROVE_FAIL = new ErrorCode(1_030_601_003, "审核失败，只有未审核的付款单才能审核");
    ErrorCode FINANCE_PAYMENT_NO_EXISTS = new ErrorCode(1_030_601_004, "生成付款单号失败，请重新提交");
    ErrorCode FINANCE_PAYMENT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_601_005, "付款单({})已审核，无法修改");

    // ========== ERP 收款单 1-030-602-000 ==========
    ErrorCode FINANCE_RECEIPT_NOT_EXISTS = new ErrorCode(1_030_602_000, "收款单不存在");
    ErrorCode FINANCE_RECEIPT_DELETE_FAIL_APPROVE = new ErrorCode(1_030_602_001, "收款单({})已审核，无法删除");
    ErrorCode FINANCE_RECEIPT_PROCESS_FAIL = new ErrorCode(1_030_602_002, "反审核失败，只有已审核的收款单才能反审核");
    ErrorCode FINANCE_RECEIPT_APPROVE_FAIL = new ErrorCode(1_030_602_003, "审核失败，只有未审核的收款单才能审核");
    ErrorCode FINANCE_RECEIPT_NO_EXISTS = new ErrorCode(1_030_602_004, "生成收款单号失败，请重新提交");
    ErrorCode FINANCE_RECEIPT_UPDATE_FAIL_APPROVE = new ErrorCode(1_030_602_005, "收款单({})已审核，无法修改");

    // ========== ERP 会计期间 1-030-700-000 (P0-6) ==========
    ErrorCode PERIOD_NOT_EXISTS = new ErrorCode(1_030_700_000, "会计期间不存在");
    ErrorCode PERIOD_CODE_DUPLICATE = new ErrorCode(1_030_700_001, "会计期间编码已存在：{}");
    ErrorCode PERIOD_NOT_OPEN = new ErrorCode(1_030_700_002, "会计期间({})不是开放状态，无法执行期末处理");
    ErrorCode PERIOD_ALREADY_CLOSED = new ErrorCode(1_030_700_003, "会计期间({})已关账，无法再次操作");
    ErrorCode PERIOD_CLOSE_FAIL_EXISTS_UNAPPROVED = new ErrorCode(1_030_700_004, "会计期间({})存在 {} 张未审核单据，无法关账");
    ErrorCode PERIOD_CLOSE_FAIL_NOT_DO_MONTH_CHECK = new ErrorCode(1_030_700_005, "会计期间({})未执行月末检查，无法执行调汇/损益结转");
    ErrorCode PERIOD_CLOSE_ALREADY_EXECUTED = new ErrorCode(1_030_700_006, "会计期间({})的{}已执行过，不可重复执行");
    ErrorCode PERIOD_CLOSE_FAIL_NOT_DO_TRANSFER = new ErrorCode(1_030_700_007, "会计期间({})未执行损益结转，无法关账");

    // ========== ERP 会计科目 1-030-701-000 (P0-7) ==========
    ErrorCode GL_ACCOUNT_NOT_EXISTS = new ErrorCode(1_030_701_000, "会计科目不存在");
    ErrorCode GL_ACCOUNT_CODE_DUPLICATE = new ErrorCode(1_030_701_001, "会计科目编码已存在：{}");
    ErrorCode GL_ACCOUNT_NOT_ENABLE = new ErrorCode(1_030_701_002, "会计科目({})未启用");
    ErrorCode GL_ACCOUNT_NOT_LEAF = new ErrorCode(1_030_701_003, "会计科目({})不是末级科目，无法录入凭证");
    ErrorCode GL_ACCOUNT_HAS_CHILDREN = new ErrorCode(1_030_701_004, "会计科目({})存在子科目，无法删除");
    ErrorCode GL_ACCOUNT_HAS_VOUCHER = new ErrorCode(1_030_701_005, "会计科目({})已被凭证使用，无法删除");
    ErrorCode GL_ACCOUNT_PARENT_NOT_EXITS = new ErrorCode(1_030_701_006, "父级会计科目不存在");
    ErrorCode GL_ACCOUNT_PARENT_ERROR = new ErrorCode(1_030_701_007, "不能设置自己为父级会计科目");
    ErrorCode GL_ACCOUNT_PARENT_IS_CHILD = new ErrorCode(1_030_701_008, "不能设置自己的子科目为父级科目");
    ErrorCode GL_ACCOUNT_TYPE_NOT_MATCH = new ErrorCode(1_030_701_009, "子科目类型({})与父科目类型({})不一致");

    // ========== ERP 会计凭证 1-030-702-000 (P0-7) ==========
    ErrorCode GL_VOUCHER_NOT_EXISTS = new ErrorCode(1_030_702_000, "会计凭证不存在");
    ErrorCode GL_VOUCHER_NO_EXISTS = new ErrorCode(1_030_702_001, "生成凭证字号失败，请重新提交");
    ErrorCode GL_VOUCHER_DELETE_FAIL_APPROVED = new ErrorCode(1_030_702_002, "会计凭证({})已审核，无法删除");
    ErrorCode GL_VOUCHER_UPDATE_FAIL_APPROVED = new ErrorCode(1_030_702_003, "会计凭证({})已审核，无法修改");
    ErrorCode GL_VOUCHER_APPROVE_FAIL = new ErrorCode(1_030_702_004, "审核失败，只有草稿状态的凭证才能审核");
    ErrorCode GL_VOUCHER_PROCESS_FAIL = new ErrorCode(1_030_702_005, "反审核失败，只有已审核的凭证才能反审核");
    ErrorCode GL_VOUCHER_ENTRY_EMPTY = new ErrorCode(1_030_702_006, "凭证分录不能为空");
    ErrorCode GL_VOUCHER_ENTRY_DEBIT_CREDIT_BOTH = new ErrorCode(1_030_702_007, "分录({})借方金额与贷方金额不能同时大于 0");
    ErrorCode GL_VOUCHER_ENTRY_DEBIT_CREDIT_ZERO = new ErrorCode(1_030_702_008, "分录({})借方金额与贷方金额不能同时为 0");
    ErrorCode GL_VOUCHER_NOT_BALANCE = new ErrorCode(1_030_702_009, "凭证借贷不平衡：借方合计 {}，贷方合计 {}");
    ErrorCode GL_VOUCHER_ACCOUNT_NOT_LEAF = new ErrorCode(1_030_702_010, "分录({})所选科目({})不是末级科目");
    ErrorCode GL_VOUCHER_PERIOD_CLOSED = new ErrorCode(1_030_702_011, "会计期间({})已关账，不允许录入凭证");

    // ========== ERP 库存批次 1-030-405-000 ==========
    ErrorCode STOCK_BATCH_NOT_EXISTS = new ErrorCode(1_030_405_000, "库存批次不存在");
    ErrorCode STOCK_BATCH_NO_DUPLICATE = new ErrorCode(1_030_405_001, "批次号已存在：{}");

    // ========== ERP 库存序列号 1-030-406-000 ==========
    ErrorCode STOCK_SERIAL_NOT_EXISTS = new ErrorCode(1_030_406_000, "库存序列号不存在");
    ErrorCode STOCK_SERIAL_NO_DUPLICATE = new ErrorCode(1_030_406_001, "序列号已存在：{}");

    // ========== ERP 币种 1-030-603-000 ==========
    ErrorCode CURRENCY_NOT_EXISTS = new ErrorCode(1_030_603_000, "币种不存在");
    ErrorCode CURRENCY_CODE_DUPLICATE = new ErrorCode(1_030_603_001, "币种编码已存在：{}");
    ErrorCode CURRENCY_NOT_ENABLE = new ErrorCode(1_030_603_002, "币种({})未启用");
    ErrorCode CURRENCY_BASE_EXISTS = new ErrorCode(1_030_603_003, "已存在本位币：{}");

    // ========== ERP 汇率 1-030-604-000 ==========
    ErrorCode EXCHANGE_RATE_NOT_EXISTS = new ErrorCode(1_030_604_000, "汇率记录不存在");
    ErrorCode EXCHANGE_RATE_NOT_FOUND = new ErrorCode(1_030_604_001, "未找到币种({}->{})在 {} 的有效汇率");

    // ========== ERP 成本中心 1-030-703-000 ==========
    ErrorCode COST_CENTER_NOT_EXISTS = new ErrorCode(1_030_703_000, "成本中心不存在");
    ErrorCode COST_CENTER_CODE_DUPLICATE = new ErrorCode(1_030_703_001, "成本中心编码已存在：{}");
    ErrorCode COST_CENTER_HAS_CHILDREN = new ErrorCode(1_030_703_002, "成本中心({})存在子成本中心，无法删除");
    ErrorCode COST_CENTER_PARENT_NOT_EXITS = new ErrorCode(1_030_703_003, "父级成本中心不存在");
    ErrorCode COST_CENTER_PARENT_ERROR = new ErrorCode(1_030_703_004, "不能设置自己为父级成本中心");

    // ========== ERP 利润中心 1-030-704-000 ==========
    ErrorCode PROFIT_CENTER_NOT_EXISTS = new ErrorCode(1_030_704_000, "利润中心不存在");
    ErrorCode PROFIT_CENTER_CODE_DUPLICATE = new ErrorCode(1_030_704_001, "利润中心编码已存在：{}");
    ErrorCode PROFIT_CENTER_HAS_CHILDREN = new ErrorCode(1_030_704_002, "利润中心({})存在子利润中心，无法删除");
    ErrorCode PROFIT_CENTER_PARENT_NOT_EXITS = new ErrorCode(1_030_704_003, "父级利润中心不存在");
    ErrorCode PROFIT_CENTER_PARENT_ERROR = new ErrorCode(1_030_704_004, "不能设置自己为父级利润中心");

    // ========== ERP 成本分摊 1-030-705-000 ==========
    ErrorCode COST_ALLOCATION_NOT_EXISTS = new ErrorCode(1_030_705_000, "成本分摊记录不存在");
    ErrorCode COST_ALLOCATION_TARGET_SAME = new ErrorCode(1_030_705_001, "源成本中心与目标成本中心不能相同");
    ErrorCode COST_ALLOCATION_AMOUNT_INVALID = new ErrorCode(1_030_705_002, "分摊金额必须大于 0");

    // ========== ERP 获利能力分析 1-030-706-000 ==========
    ErrorCode PROFITABILITY_ANALYSIS_NOT_EXISTS = new ErrorCode(1_030_706_000, "获利分析记录不存在");

    // ========== ERP 固定资产 1-030-707-000 ==========
    ErrorCode FIXED_ASSET_NOT_EXISTS = new ErrorCode(1_030_707_000, "固定资产不存在");
    ErrorCode FIXED_ASSET_CODE_DUPLICATE = new ErrorCode(1_030_707_001, "资产编码已存在：{}");
    ErrorCode FIXED_ASSET_DEPRECIATION_METHOD_NOT_SUPPORT = new ErrorCode(1_030_707_002, "不支持的折旧方法：{}");
    ErrorCode FIXED_ASSET_USEFUL_LIFE_INVALID = new ErrorCode(1_030_707_003, "使用年限必须大于 0");
    ErrorCode FIXED_ASSET_SALVAGE_INVALID = new ErrorCode(1_030_707_004, "预计残值不能大于等于资产原值");
    ErrorCode FIXED_ASSET_ALREADY_FULLY_DEPRECIATED = new ErrorCode(1_030_707_005, "资产({})已提足折旧，无需再次计提");
    ErrorCode FIXED_ASSET_NOT_IN_USE = new ErrorCode(1_030_707_006, "资产({})非在用状态，无法计提折旧");
    ErrorCode FIXED_ASSET_DEPRECIATION_NOT_EXISTS = new ErrorCode(1_030_707_007, "折旧记录不存在");
    ErrorCode FIXED_ASSET_DEPRECIATION_ALREADY_APPROVED = new ErrorCode(1_030_707_008, "折旧记录已审核，无法重复审核");
    ErrorCode FIXED_ASSET_DEPRECIATION_PERIOD_DUPLICATE = new ErrorCode(1_030_707_009, "资产({})在期间({})已存在折旧记录");
    ErrorCode FIXED_ASSET_DEPRECIATION_DATE_BEFORE_CAP = new ErrorCode(1_030_707_010, "折旧日期({})早于入账日期({})，无法计提折旧");
    ErrorCode FIXED_ASSET_HAS_DEPRECIATION_RECORDS = new ErrorCode(1_030_707_011, "资产({})存在折旧记录，无法删除");
    ErrorCode FIXED_ASSET_DEPRECIATION_GL_VOUCHER_FAIL = new ErrorCode(1_030_707_012, "折旧记录({})创建 GL 凭证失败，审核已回滚");

    // ========== ERP 预算管理 1-030-708-000 ==========
    ErrorCode BUDGET_NOT_EXISTS = new ErrorCode(1_030_708_000, "预算不存在");
    ErrorCode BUDGET_DETAIL_NOT_EXISTS = new ErrorCode(1_030_708_001, "预算明细不存在");
    ErrorCode BUDGET_STATUS_NOT_DRAFT = new ErrorCode(1_030_708_002, "预算({})非草稿状态，无法修改");
    ErrorCode BUDGET_ALREADY_APPROVED = new ErrorCode(1_030_708_003, "预算({})已审批，无法重复审批");
    ErrorCode BUDGET_PERIOD_DUPLICATE = new ErrorCode(1_030_708_004, "该期间({})下已存在同类型预算");
    ErrorCode BUDGET_DETAIL_AMOUNT_INVALID = new ErrorCode(1_030_708_005, "预算明细金额必须大于等于 0");
    ErrorCode BUDGET_DETAIL_TOTAL_MISMATCH = new ErrorCode(1_030_708_006, "预算明细金额合计({})与预算总额({})不一致");

    // ========== ERP 合并报表 1-030-709-000 ==========
    ErrorCode CONSOLIDATION_ENTRY_NOT_EXISTS = new ErrorCode(1_030_709_000, "合并报表抵消分录不存在");
    ErrorCode CONSOLIDATION_ENTRY_ALREADY_APPROVED = new ErrorCode(1_030_709_001, "抵消分录已审核，无法修改");
    ErrorCode CONSOLIDATION_DEBIT_CREDIT_NOT_BALANCE = new ErrorCode(1_030_709_002, "抵消分录借贷不平衡：借方合计 {}，贷方合计 {}");
    ErrorCode CONSOLIDATION_ACCOUNT_INVALID = new ErrorCode(1_030_709_003, "抵消分录借方或贷方科目不能同时为空");

    // ========== ERP 固定资产变动 1-030-710-000 ==========
    ErrorCode FA_CHANGE_NOT_EXISTS = new ErrorCode(1_030_710_000, "资产变动记录不存在");
    ErrorCode FA_CHANGE_STATUS_INVALID = new ErrorCode(1_030_710_001, "资产变动记录状态不支持当前操作");
    ErrorCode FA_CHANGE_ASSET_NOT_EXISTS = new ErrorCode(1_030_710_002, "资产变动关联的固定资产不存在");
    ErrorCode FA_CHANGE_ALREADY_APPROVED = new ErrorCode(1_030_710_003, "资产变动记录已审核，无法重复审核");

    // ========== ERP 成本项目 1-040-800-000 ==========
    ErrorCode COST_ITEM_NOT_EXISTS = new ErrorCode(1_040_800_000, "成本项目不存在");
    ErrorCode COST_ITEM_CODE_DUPLICATE = new ErrorCode(1_040_800_001, "成本项目编码已存在：{}");

    // ========== ERP 标准成本 1-040-801-000 ==========
    ErrorCode STANDARD_COST_NOT_EXISTS = new ErrorCode(1_040_801_000, "标准成本不存在");
    ErrorCode STANDARD_COST_DATE_INVALID = new ErrorCode(1_040_801_001, "生效日期不能晚于失效日期");
    ErrorCode STANDARD_COST_EXISTS = new ErrorCode(1_040_801_002, "同一产品+项目+生效日期已存在标准成本");

    // ========== ERP 实际成本 1-040-802-000 ==========
    ErrorCode ACTUAL_COST_NOT_EXISTS = new ErrorCode(1_040_802_000, "实际成本不存在");
    ErrorCode ACTUAL_COST_PERIOD_DUPLICATE = new ErrorCode(1_040_802_001, "产品({})在期间({})该项目已存在实际成本记录");

    // ========== ERP 成本差异 1-040-803-000 ==========
    ErrorCode COST_VARIANCE_NOT_EXISTS = new ErrorCode(1_040_803_000, "成本差异不存在");

    // ========== ERP 工单成本归集 1-040-804-000 ==========
    ErrorCode WORK_ORDER_COST_NOT_EXISTS = new ErrorCode(1_040_804_000, "工单成本归集不存在");

    // ========== ERP 成本核算 1-040-805-000 ==========
    ErrorCode COST_CALCULATION_NO_DATA = new ErrorCode(1_040_805_000, "成本核算无可用数据");
    ErrorCode COST_CALCULATION_PERIOD_CLOSED = new ErrorCode(1_040_805_001, "期间({})已关账，不能再计算当期成本");

    // ========== ERP 税率 1-040-900-000 ==========
    ErrorCode TAX_RATE_NOT_EXISTS = new ErrorCode(1_040_900_000, "税率不存在");
    ErrorCode TAX_RATE_CODE_DUPLICATE = new ErrorCode(1_040_900_001, "税率编码已存在：{}");
    ErrorCode TAX_RATE_RATE_INVALID = new ErrorCode(1_040_900_002, "税率必须在 0~1 之间");

    // ========== ERP 发票 1-040-901-000 ==========
    ErrorCode INVOICE_NOT_EXISTS = new ErrorCode(1_040_901_000, "发票不存在");
    ErrorCode INVOICE_NO_DUPLICATE = new ErrorCode(1_040_901_001, "发票号+发票代码已存在：{}-{}");
    ErrorCode INVOICE_STATUS_INVALID = new ErrorCode(1_040_901_002, "发票状态不支持当前操作");

    // ========== ERP 发票明细 1-040-902-000 ==========
    ErrorCode INVOICE_LINE_NOT_EXISTS = new ErrorCode(1_040_902_000, "发票明细不存在");

    // ========== ERP 发票金额校验 1-040-903-000 ==========
    ErrorCode INVOICE_AMOUNT_MISMATCH = new ErrorCode(1_040_903_000, "发票明细金额合计与主表金额不一致：不含税 {}，税额 {}，价税合计 {}");


    // ========== ERP 主生产计划 MPS 1-040-910-000 ==========
    ErrorCode MPS_PLAN_NOT_EXISTS = new ErrorCode(1_040_910_000, "主生产计划不存在");
    ErrorCode MPS_PLAN_NO_DUPLICATE = new ErrorCode(1_040_910_001, "主生产计划编号已存在：{}");
    ErrorCode MPS_PLAN_STATUS_INVALID = new ErrorCode(1_040_910_002, "主生产计划状态不正确，无法执行该操作");
    ErrorCode MPS_PLAN_PERIOD_DUPLICATE = new ErrorCode(1_040_910_003, "产品({})在周期({})已存在主生产计划");
    ErrorCode MPS_PLAN_NOT_CONFIRMABLE = new ErrorCode(1_040_910_004, "只有草稿状态的主生产计划才能确认");
    ErrorCode MPS_PLAN_NOT_RELEASABLE = new ErrorCode(1_040_910_005, "只有已确认状态的主生产计划才能下发 MRP");

    // ========== ERP 主生产计划明细 1-040-911-000 ==========
    ErrorCode MPS_PLAN_DETAIL_NOT_EXISTS = new ErrorCode(1_040_911_000, "主生产计划明细不存在");

    // ========== ERP 账簿（多账簿支持）1-040-920-000 ==========
    ErrorCode ACCOUNT_BOOK_NOT_EXISTS = new ErrorCode(1_040_920_000, "账簿不存在");
    ErrorCode ACCOUNT_BOOK_CODE_DUPLICATE = new ErrorCode(1_040_920_001, "账簿编码已存在：{}");
    ErrorCode ACCOUNT_BOOK_PRIMARY_EXISTS = new ErrorCode(1_040_920_002, "同一会计准则下已有主账簿：{}");
    ErrorCode ACCOUNT_BOOK_HAS_VOUCHERS = new ErrorCode(1_040_920_003, "账簿已被凭证引用，无法删除");
    ErrorCode ACCOUNT_BOOK_STATUS_INVALID = new ErrorCode(1_040_920_004, "账簿状态不支持当前操作");

    // ========== ERP 合并工作底稿 1-040-930-000 ==========
    ErrorCode CONSOLIDATION_WORKSHEET_NOT_EXISTS = new ErrorCode(1_040_930_000, "合并工作底稿不存在");

    // ========== ERP 合并范围 1-040-931-000 ==========
    ErrorCode CONSOLIDATION_SCOPE_NOT_EXISTS = new ErrorCode(1_040_931_000, "合并范围不存在");
    ErrorCode CONSOLIDATION_SCOPE_DUPLICATE = new ErrorCode(1_040_931_001, "母子公司合并范围已存在");

    // ========== ERP 合并抵消引擎 1-040-932-000 ==========
    ErrorCode CONSOLIDATION_NO_SCOPE = new ErrorCode(1_040_932_000, "合并范围为空，无法生成抵消分录");

    // ========== ERP 金税四期接口 1-040-940-000 (P0-1) ==========
    ErrorCode GOLDEN_TAX_NOT_ENABLED = new ErrorCode(1_040_940_000, "金税接口未启用，请在配置中开启（yudao.erp.golden-tax.enabled=true）");

    // ========== ERP 客户信用控制 1-040-941-000 (P0-2) ==========
    ErrorCode SALE_ORDER_CREDIT_EXCEED = new ErrorCode(1_040_941_000, "销售订单({})金额({})+客户({})已用额度({})超过信用额度({})");
    ErrorCode CUSTOMER_CREDIT_NOT_ENOUGH = new ErrorCode(1_040_941_001, "客户({})信用额度不足，可用额度：{}");

    // ========== ERP 银行账户 1-040-942-000 (P0-3) ==========
    ErrorCode BANK_ACCOUNT_NOT_EXISTS = new ErrorCode(1_040_942_000, "银行账户不存在");
    ErrorCode BANK_ACCOUNT_NOT_ENABLE = new ErrorCode(1_040_942_001, "银行账户({})未启用");
    ErrorCode BANK_ACCOUNT_BALANCE_NOT_ENOUGH = new ErrorCode(1_040_942_002, "银行账户({})余额不足，当前余额：{}，所需金额：{}");

    // ========== ERP 资金计划 1-040-943-000 (P0-3) ==========
    ErrorCode FUND_PLAN_NOT_EXISTS = new ErrorCode(1_040_943_000, "资金计划不存在");

    // ========== ERP 现金流 1-040-944-000 (P0-3) ==========
    ErrorCode CASH_FLOW_NOT_EXISTS = new ErrorCode(1_040_944_000, "现金流记录不存在");

    // ========== ERP 财务报表 1-040-945-000 (P0-4) ==========
    ErrorCode FINANCIAL_STATEMENT_PERIOD_INVALID = new ErrorCode(1_040_945_000, "财务报表期间参数无效：{}");

    // ========== ERP VMI 供应商管理库存 1-030-800-000 ==========
    ErrorCode VMI_INVENTORY_NOT_EXISTS = new ErrorCode(1_030_800_000, "VMI 库存不存在");
    ErrorCode VMI_REPLENISHMENT_NOT_EXISTS = new ErrorCode(1_030_800_001, "VMI 补货建议不存在");
    ErrorCode VMI_REPLENISHMENT_NOT_CONVERTIBLE = new ErrorCode(1_030_800_002, "VMI 补货建议({})非待处理状态，无法转采购订单");

    // ========== ERP CPFR 联合计划预测补货 1-030-801-000 ==========
    ErrorCode CPFR_FORECAST_NOT_EXISTS = new ErrorCode(1_030_801_000, "CPFR 预测不存在");
    ErrorCode CPFR_EXCEPTION_NOT_EXISTS = new ErrorCode(1_030_801_001, "CPFR 异常不存在");

    // ========== ERP MRP 物料需求计划 1-040-950-000 ==========
    ErrorCode MRP_PLAN_NOT_EXISTS = new ErrorCode(1_040_950_000, "MRP 物料需求计划不存在");
    ErrorCode MRP_PLAN_NO_DUPLICATE = new ErrorCode(1_040_950_001, "MRP 物料需求计划编号已存在：{}");
    ErrorCode MRP_PLAN_STATUS_INVALID = new ErrorCode(1_040_950_002, "MRP 物料需求计划状态不正确，无法执行该操作");
    ErrorCode MRP_PLAN_NOT_EXECUTABLE = new ErrorCode(1_040_950_003, "只有草稿/已计算状态的 MRP 计划才能执行计算");
    ErrorCode MRP_PLAN_NOT_CONFIRMABLE = new ErrorCode(1_040_950_004, "只有已计算状态的 MRP 计划才能确认");
    ErrorCode MRP_RESULT_NOT_EXISTS = new ErrorCode(1_040_951_000, "MRP 物料需求计划结果不存在");

    // ========== ERP 客户信用额度 1-040-952-000 ==========
    ErrorCode CREDIT_LIMIT_NOT_EXISTS = new ErrorCode(1_040_952_000, "客户信用额度不存在");
    ErrorCode CREDIT_LIMIT_NOT_ENOUGH = new ErrorCode(1_040_952_001, "客户({})信用额度不足，可用额度：{}");
    ErrorCode CREDIT_LIMIT_FROZEN = new ErrorCode(1_040_952_002, "客户({})信用额度已冻结，无法下单");
    ErrorCode CREDIT_LIMIT_CUSTOMER_DUPLICATE = new ErrorCode(1_040_952_003, "客户({})已存在信用额度记录");

    // ========== ERP 供应商评估 1-040-953-000 ==========
    ErrorCode SUPPLIER_EVALUATION_NOT_EXISTS = new ErrorCode(1_040_953_000, "供应商评估不存在");
    ErrorCode SUPPLIER_EVALUATION_PERIOD_DUPLICATE = new ErrorCode(1_040_953_001, "供应商({})在周期({})已存在评估记录");
    ErrorCode SUPPLIER_EVALUATION_ITEM_NOT_EXISTS = new ErrorCode(1_040_954_000, "供应商评估指标项不存在");

    // ========== ERP 出纳管理 1-040-955-000 ==========
    ErrorCode CASHIER_NOT_EXISTS = new ErrorCode(1_040_955_000, "出纳单不存在");
    ErrorCode CASHIER_NO_EXISTS = new ErrorCode(1_040_955_001, "生成出纳单号失败，请重新提交");
    ErrorCode CASHIER_STATUS_INVALID = new ErrorCode(1_040_955_002, "出纳单状态不正确，无法执行该操作");

}
