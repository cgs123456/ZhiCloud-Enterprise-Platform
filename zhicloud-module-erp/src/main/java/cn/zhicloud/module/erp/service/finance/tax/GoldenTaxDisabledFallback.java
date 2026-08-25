package cn.zhicloud.module.erp.service.finance.tax;

import cn.zhicloud.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GOLDEN_TAX_NOT_ENABLED;

/**
 * 金税四期接口默认回退实现（禁用态）
 *
 * <p>当 {@code zhicloud.erp.golden-tax.enabled} 未启用（默认 false 或缺省）时加载本实现。
 * 所有方法抛出 {@link cn.zhicloud.framework.common.exception.ServiceException}，
 * 提示"金税接口未启用，请在配置中开启"，避免在未接入金税 SDK 的情况下静默返回 mock 数据。
 *
 * <p>如需启用金税四期开票能力，请在 application.yaml 配置：
 * <pre>
 * zhicloud:
 *   erp:
 *     golden-tax:
 *       enabled: true
 * </pre>
 * 并替换 {@link GoldenTaxApiStub} 为接入真实金税 SDK 的实现。
 *
 * @author 智云
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "zhicloud.erp.golden-tax.enabled", havingValue = "false", matchIfMissing = true)
public class GoldenTaxDisabledFallback implements GoldenTaxApi {

    @Override
    public Map<String, Object> issueInvoice(ErpTaxInvoiceDO invoice) {
        log.warn("[GoldenTaxDisabledFallback.issueInvoice][金税接口未启用，拒绝开票 发票号({})]", invoice.getInvoiceNo());
        throw exception(GOLDEN_TAX_NOT_ENABLED);
    }

    @Override
    public Map<String, Object> revokeInvoice(String invoiceNo, String invoiceCode) {
        log.warn("[GoldenTaxDisabledFallback.revokeInvoice][金税接口未启用，拒绝作废 发票号({})]", invoiceNo);
        throw exception(GOLDEN_TAX_NOT_ENABLED);
    }

    @Override
    public Map<String, Object> redInvoice(String originalInvoiceNo, String originalInvoiceCode,
                                          BigDecimal redAmount) {
        log.warn("[GoldenTaxDisabledFallback.redInvoice][金税接口未启用，拒绝红冲 原发票号({})]", originalInvoiceNo);
        throw exception(GOLDEN_TAX_NOT_ENABLED);
    }

    @Override
    public Map<String, Object> queryInvoiceStatus(String invoiceNo, String invoiceCode) {
        log.warn("[GoldenTaxDisabledFallback.queryInvoiceStatus][金税接口未启用，拒绝查询 发票号({})]", invoiceNo);
        throw exception(GOLDEN_TAX_NOT_ENABLED);
    }

}