package cn.iocoder.yudao.module.erp.service.finance.tax;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceDO;
import cn.iocoder.yudao.module.erp.enums.finance.tax.ErpInvoiceTypeEnum;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * ERP 增值税计算 Service 实现类
 *
 * <p>增值税计算公式：
 * <ul>
 *   <li>销项税额 = ∑(销项专票税额 + 销项普票税额)</li>
 *   <li>进项税额 = ∑(进项专票税额 + 进项普票税额)</li>
 *   <li>应纳增值税额 = 销项税额 - 进项税额</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Service
@Validated
public class ErpVatCalculationServiceImpl implements ErpVatCalculationService {

    @Resource
    private ErpTaxInvoiceService taxInvoiceService;

    @Override
    public BigDecimal calculateOutputTax(LocalDate startDate, LocalDate endDate) {
        // 查询期间内所有销项专票和销项普票
        List<ErpTaxInvoiceDO> outputSpecial = taxInvoiceService.getTaxInvoiceListByPeriod(
                startDate, endDate, ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType());
        List<ErpTaxInvoiceDO> outputNormal = taxInvoiceService.getTaxInvoiceListByPeriod(
                startDate, endDate, ErpInvoiceTypeEnum.OUTPUT_NORMAL.getType());
        // 汇总税额
        BigDecimal total = BigDecimal.ZERO;
        total = sumTaxAmount(total, outputSpecial);
        total = sumTaxAmount(total, outputNormal);
        return total;
    }

    @Override
    public BigDecimal calculateInputTax(LocalDate startDate, LocalDate endDate) {
        // 查询期间内所有进项专票和进项普票
        List<ErpTaxInvoiceDO> inputSpecial = taxInvoiceService.getTaxInvoiceListByPeriod(
                startDate, endDate, ErpInvoiceTypeEnum.INPUT_SPECIAL.getType());
        List<ErpTaxInvoiceDO> inputNormal = taxInvoiceService.getTaxInvoiceListByPeriod(
                startDate, endDate, ErpInvoiceTypeEnum.INPUT_NORMAL.getType());
        // 汇总税额
        BigDecimal total = BigDecimal.ZERO;
        total = sumTaxAmount(total, inputSpecial);
        total = sumTaxAmount(total, inputNormal);
        return total;
    }

    @Override
    public BigDecimal calculatePayableTax(LocalDate startDate, LocalDate endDate) {
        BigDecimal outputTax = calculateOutputTax(startDate, endDate);
        BigDecimal inputTax = calculateInputTax(startDate, endDate);
        return outputTax.subtract(inputTax);
    }

    private BigDecimal sumTaxAmount(BigDecimal initial, List<ErpTaxInvoiceDO> invoices) {
        BigDecimal result = initial;
        if (invoices == null || invoices.isEmpty()) {
            return result;
        }
        for (ErpTaxInvoiceDO invoice : invoices) {
            if (invoice.getTaxAmount() != null) {
                result = result.add(invoice.getTaxAmount());
            }
        }
        return result;
    }

}
