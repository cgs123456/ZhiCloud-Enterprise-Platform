package cn.iocoder.yudao.module.erp.service.finance.tax;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 金税四期接口占位实现
 *
 * <p><b>⚠️ WARN：金税四期接口占位实现，生产环境必须替换为真实 SDK！</b>
 *
 * <p>当前为占位实现，仅记录日志并返回模拟结果，<b>不会真实调用</b>金税四期系统。
 * 真实接入需要企业数字证书（UKey）、税控设备以及金税 SDK，请替换为本接口的真实实现。
 *
 * <p>开关：{@code yudao.erp.golden-tax.enabled}，默认关闭（false）。
 * 仅在显式设置为 true 时，本占位实现才会被加载（便于开发/演示）。
 *
 * @deprecated 金税四期接口占位实现，生产环境必须替换为真实 SDK。请在 {@code yudao.erp.golden-tax.enabled=true}
 * 配合真实证书后，替换为接入金税 SDK 的实现类。
 */
@Deprecated
@Slf4j
@Component
@ConditionalOnProperty(name = "yudao.erp.golden-tax.enabled", havingValue = "true")
public class GoldenTaxApiStub implements GoldenTaxApi {

    @Override
    public Map<String, Object> issueInvoice(ErpTaxInvoiceDO invoice) {
        log.warn("[GoldenTaxApiStub.issueInvoice][占位调用 发票号({}) 发票代码({}) 价税合计({})] 生产环境必须替换为真实金税 SDK",
                invoice.getInvoiceNo(), invoice.getInvoiceCode(), invoice.getAmountWithTax());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "金税四期接口未接入，使用占位实现");
        result.put("invoiceNo", invoice.getInvoiceNo());
        result.put("invoiceCode", invoice.getInvoiceCode());
        return result;
    }

    @Override
    public Map<String, Object> revokeInvoice(String invoiceNo, String invoiceCode) {
        log.warn("[GoldenTaxApiStub.revokeInvoice][占位调用 发票号({}) 发票代码({})] 生产环境必须替换为真实金税 SDK",
                invoiceNo, invoiceCode);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "金税四期接口未接入，使用占位实现");
        return result;
    }

    @Override
    public Map<String, Object> redInvoice(String originalInvoiceNo, String originalInvoiceCode,
                                          BigDecimal redAmount) {
        log.warn("[GoldenTaxApiStub.redInvoice][占位调用 原发票号({}) 原发票代码({}) 红冲金额({})] 生产环境必须替换为真实金税 SDK",
                originalInvoiceNo, originalInvoiceCode, redAmount);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "金税四期接口未接入，使用占位实现");
        return result;
    }

    @Override
    public Map<String, Object> queryInvoiceStatus(String invoiceNo, String invoiceCode) {
        log.warn("[GoldenTaxApiStub.queryInvoiceStatus][占位调用 发票号({}) 发票代码({})] 生产环境必须替换为真实金税 SDK",
                invoiceNo, invoiceCode);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "金税四期接口未接入，使用占位实现");
        result.put("status", "ISSUED");
        return result;
    }

}
