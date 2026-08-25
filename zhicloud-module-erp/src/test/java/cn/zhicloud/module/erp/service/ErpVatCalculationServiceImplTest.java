package cn.zhicloud.module.erp.service;

import cn.zhicloud.module.erp.dal.dataobject.finance.tax.ErpTaxInvoiceDO;
import cn.zhicloud.module.erp.enums.finance.tax.ErpInvoiceTypeEnum;
import cn.zhicloud.module.erp.service.finance.tax.ErpTaxInvoiceService;
import cn.zhicloud.module.erp.service.finance.tax.ErpVatCalculationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ErpVatCalculationServiceImpl} 的单元测试
 *
 * <p>覆盖增值税计算（销项税额 - 进项税额 = 应纳税额）。
 *
 * @author 智云
 */
@ExtendWith(MockitoExtension.class)
public class ErpVatCalculationServiceImplTest {

    @Mock
    private ErpTaxInvoiceService taxInvoiceService;

    @InjectMocks
    private ErpVatCalculationServiceImpl vatCalculationService;

    private static final LocalDate START = LocalDate.of(2026, 7, 1);
    private static final LocalDate END = LocalDate.of(2026, 7, 31);

    /**
     * 构建发票
     */
    private ErpTaxInvoiceDO buildInvoice(Integer type, BigDecimal taxAmount) {
        return ErpTaxInvoiceDO.builder()
                .id(1L).invoiceType(type).taxAmount(taxAmount)
                .amountWithoutTax(BigDecimal.ZERO).amountWithTax(BigDecimal.ZERO)
                .build();
    }

    // ==================== 销项税额 ====================

    @Test
    public void testCalculateOutputTax_normal() {
        // 销项专票税额 5000 + 销项普票税额 3000 = 8000
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType())))
                .thenReturn(Collections.singletonList(buildInvoice(
                        ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType(), new BigDecimal("5000"))));
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_NORMAL.getType())))
                .thenReturn(Collections.singletonList(buildInvoice(
                        ErpInvoiceTypeEnum.OUTPUT_NORMAL.getType(), new BigDecimal("3000"))));

        BigDecimal result = vatCalculationService.calculateOutputTax(START, END);

        assertEquals(0, new BigDecimal("8000").compareTo(result));
    }

    @Test
    public void testCalculateOutputTax_multipleInvoices() {
        // 多张销项专票
        List<ErpTaxInvoiceDO> specialInvoices = Arrays.asList(
                buildInvoice(ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType(), new BigDecimal("1000")),
                buildInvoice(ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType(), new BigDecimal("2000")),
                buildInvoice(ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType(), new BigDecimal("500")));
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType())))
                .thenReturn(specialInvoices);
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_NORMAL.getType())))
                .thenReturn(Collections.emptyList());

        BigDecimal result = vatCalculationService.calculateOutputTax(START, END);

        // 1000 + 2000 + 500 = 3500
        assertEquals(0, new BigDecimal("3500").compareTo(result));
    }

    @Test
    public void testCalculateOutputTax_emptyInvoices() {
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END), anyInt()))
                .thenReturn(Collections.emptyList());

        BigDecimal result = vatCalculationService.calculateOutputTax(START, END);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    public void testCalculateOutputTax_nullTaxAmount() {
        // 发票税额为 null，应按 0 处理
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType())))
                .thenReturn(Collections.singletonList(buildInvoice(
                        ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType(), null)));
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_NORMAL.getType())))
                .thenReturn(Collections.emptyList());

        BigDecimal result = vatCalculationService.calculateOutputTax(START, END);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    // ==================== 进项税额 ====================

    @Test
    public void testCalculateInputTax_normal() {
        // 进项专票税额 4000 + 进项普票税额 1000 = 5000
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.INPUT_SPECIAL.getType())))
                .thenReturn(Collections.singletonList(buildInvoice(
                        ErpInvoiceTypeEnum.INPUT_SPECIAL.getType(), new BigDecimal("4000"))));
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.INPUT_NORMAL.getType())))
                .thenReturn(Collections.singletonList(buildInvoice(
                        ErpInvoiceTypeEnum.INPUT_NORMAL.getType(), new BigDecimal("1000"))));

        BigDecimal result = vatCalculationService.calculateInputTax(START, END);

        assertEquals(0, new BigDecimal("5000").compareTo(result));
    }

    // ==================== 应纳增值税额 ====================

    @Test
    public void testCalculatePayableTax_outputExceedsInput() {
        // 销项 8000 - 进项 5000 = 应纳 3000
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType())))
                .thenReturn(Collections.singletonList(buildInvoice(
                        ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType(), new BigDecimal("8000"))));
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_NORMAL.getType())))
                .thenReturn(Collections.emptyList());
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.INPUT_SPECIAL.getType())))
                .thenReturn(Collections.singletonList(buildInvoice(
                        ErpInvoiceTypeEnum.INPUT_SPECIAL.getType(), new BigDecimal("5000"))));
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.INPUT_NORMAL.getType())))
                .thenReturn(Collections.emptyList());

        BigDecimal result = vatCalculationService.calculatePayableTax(START, END);

        // 应纳税额 = 8000 - 5000 = 3000
        assertEquals(0, new BigDecimal("3000").compareTo(result));
    }

    @Test
    public void testCalculatePayableTax_inputExceedsOutput_negative() {
        // 销项 2000 - 进项 5000 = -3000（期末留抵）
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType())))
                .thenReturn(Collections.singletonList(buildInvoice(
                        ErpInvoiceTypeEnum.OUTPUT_SPECIAL.getType(), new BigDecimal("2000"))));
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.OUTPUT_NORMAL.getType())))
                .thenReturn(Collections.emptyList());
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.INPUT_SPECIAL.getType())))
                .thenReturn(Collections.singletonList(buildInvoice(
                        ErpInvoiceTypeEnum.INPUT_SPECIAL.getType(), new BigDecimal("5000"))));
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END),
                eq(ErpInvoiceTypeEnum.INPUT_NORMAL.getType())))
                .thenReturn(Collections.emptyList());

        BigDecimal result = vatCalculationService.calculatePayableTax(START, END);

        // 应纳税额 = 2000 - 5000 = -3000（负数表示期末留抵）
        assertEquals(0, new BigDecimal("-3000").compareTo(result));
    }

    @Test
    public void testCalculatePayableTax_zeroOutputZeroInput() {
        // 无发票，应纳税额 = 0
        when(taxInvoiceService.getTaxInvoiceListByPeriod(eq(START), eq(END), anyInt()))
                .thenReturn(Collections.emptyList());

        BigDecimal result = vatCalculationService.calculatePayableTax(START, END);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

}
