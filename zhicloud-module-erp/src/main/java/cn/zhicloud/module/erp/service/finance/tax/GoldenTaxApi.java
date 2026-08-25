package cn.zhicloud.module.erp.service.finance.tax;

import cn.zhicloud.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceDO;

import java.util.Map;

/**
 * 金税四期接口（占位）
 *
 * <p>用于与国家税务总局金税四期系统对接，提供发票开具、查询、作废、红冲等能力。
 * 当前为占位实现，后续接入金税 SDK 时替换为真实调用。
 *
 * @author 智云
 */
public interface GoldenTaxApi {

    /**
     * 发票开具（调用金税四期开票接口）
     *
     * @param invoice 发票信息
     * @return 金税系统返回的结果（含发票号码、校验码等）
     */
    Map<String, Object> issueInvoice(ErpTaxInvoiceDO invoice);

    /**
     * 发票作废
     *
     * @param invoiceNo 发票号
     * @param invoiceCode 发票代码
     * @return 作废结果
     */
    Map<String, Object> revokeInvoice(String invoiceNo, String invoiceCode);

    /**
     * 发票红冲（开具红字发票）
     *
     * @param originalInvoiceNo 原发票号
     * @param originalInvoiceCode 原发票代码
     * @param redAmount 红冲金额
     * @return 红冲结果
     */
    Map<String, Object> redInvoice(String originalInvoiceNo, String originalInvoiceCode,
                                   java.math.BigDecimal redAmount);

    /**
     * 查询发票状态
     *
     * @param invoiceNo 发票号
     * @param invoiceCode 发票代码
     * @return 发票状态信息
     */
    Map<String, Object> queryInvoiceStatus(String invoiceNo, String invoiceCode);

}
