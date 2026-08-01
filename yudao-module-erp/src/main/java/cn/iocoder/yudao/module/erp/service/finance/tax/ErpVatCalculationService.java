package cn.iocoder.yudao.module.erp.service.finance.tax;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 增值税计算 Service 接口
 *
 * <p>提供增值税申报所需的关键数据计算：
 * <ul>
 *   <li>销项税额：销项发票（销项专票 + 销项普票）的税额合计</li>
 *   <li>进项税额：进项发票（进项专票 + 进项普票）的税额合计</li>
 *   <li>应纳税额 = 销项税额 - 进项税额（当为正数时为应补退税额；为负数时为期末留抵）</li>
 * </ul>
 *
 * @author 芋道源码
 */
public interface ErpVatCalculationService {

    /**
     * 计算指定期间的销项税额
     *
     * @param startDate 期间起始日期
     * @param endDate 期间结束日期
     * @return 销项税额
     */
    BigDecimal calculateOutputTax(LocalDate startDate, LocalDate endDate);

    /**
     * 计算指定期间的进项税额
     *
     * @param startDate 期间起始日期
     * @param endDate 期间结束日期
     * @return 进项税额
     */
    BigDecimal calculateInputTax(LocalDate startDate, LocalDate endDate);

    /**
     * 计算指定期间的应纳增值税额
     *
     * <p>应纳税额 = 销项税额 - 进项税额
     *
     * @param startDate 期间起始日期
     * @param endDate 期间结束日期
     * @return 应纳增值税额（正数：应补退；负数：期末留抵）
     */
    BigDecimal calculatePayableTax(LocalDate startDate, LocalDate endDate);

}
