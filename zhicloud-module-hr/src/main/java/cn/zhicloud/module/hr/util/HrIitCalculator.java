package cn.zhicloud.module.hr.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * HR 个税计算工具类（Individual Income Tax）
 * <p>
 * 采用中国个人所得税累计预扣预缴法。
 * <p>
 * 预扣率表（按年）：
 * ≤36000          3%   速算扣除数 0
 * 36000-144000     10%  速算扣除数 2520
 * 144000-300000    20%  速算扣除数 16920
 * 300000-420000    25%  速算扣除数 31920
 * 420000-660000    30%  速算扣除数 52920
 * 660000-960000    35%  速算扣除数 85920
 * >960000          45%  速算扣除数 181920
 *
 * @author zhicloud
 */
public class HrIitCalculator {

    /**
     * 个税起征点（每月减除费用）
     */
    public static final BigDecimal MONTHLY_DEDUCTION = new BigDecimal("5000");

    /**
     * 预扣率档位：累计预扣预缴应纳税所得额上限
     */
    private static final BigDecimal[] BRACKETS = {
            new BigDecimal("36000"),
            new BigDecimal("144000"),
            new BigDecimal("300000"),
            new BigDecimal("420000"),
            new BigDecimal("660000"),
            new BigDecimal("960000")
    };

    /**
     * 预扣率
     */
    private static final BigDecimal[] RATES = {
            new BigDecimal("0.03"),
            new BigDecimal("0.10"),
            new BigDecimal("0.20"),
            new BigDecimal("0.25"),
            new BigDecimal("0.30"),
            new BigDecimal("0.35"),
            new BigDecimal("0.45")
    };

    /**
     * 速算扣除数
     */
    private static final BigDecimal[] QUICK_DEDUCTIONS = {
            BigDecimal.ZERO,
            new BigDecimal("2520"),
            new BigDecimal("16920"),
            new BigDecimal("31920"),
            new BigDecimal("52920"),
            new BigDecimal("85920"),
            new BigDecimal("181920")
    };

    /**
     * 累计预扣预缴法计算本月应预扣预缴税额。
     * <p>
     * 累计预扣预缴应纳税所得额 = 累计收入 - 累计免税收入 - 累计减除费用(5000×月数) - 累计专项扣除 - 累计专项附加扣除
     * <p>
     * 本期应预扣预缴税额 = (累计预扣预缴应纳税所得额 × 预扣率 - 速算扣除数) - 累计减免税额 - 累计已预扣预缴税额
     *
     * @param monthlyTaxableIncome    本月应纳税所得额（本月收入 - 5000 - 本月专项扣除等）。若为负视为 0
     * @param cumulativeTaxableIncome 截至上月累计预扣预缴应纳税所得额
     * @param cumulativeDeductedTax   截至上月累计已预扣预缴税额
     * @param year                    年份
     * @param month                   月份(1-12)
     * @return 本月应预扣预缴税额（不小于 0）
     */
    public static BigDecimal calculateMonthlyIIT(BigDecimal monthlyTaxableIncome,
                                                 BigDecimal cumulativeTaxableIncome,
                                                 BigDecimal cumulativeDeductedTax,
                                                 int year, int month) {
        if (monthlyTaxableIncome == null) {
            monthlyTaxableIncome = BigDecimal.ZERO;
        }
        if (cumulativeTaxableIncome == null) {
            cumulativeTaxableIncome = BigDecimal.ZERO;
        }
        if (cumulativeDeductedTax == null) {
            cumulativeDeductedTax = BigDecimal.ZERO;
        }
        // 1. 累计预扣预缴应纳税所得额 = 之前累计 + 本月新增
        BigDecimal newCumulativeTaxableIncome = cumulativeTaxableIncome.add(monthlyTaxableIncome);
        if (newCumulativeTaxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        // 2. 累计应纳税额 = 累计预扣预缴应纳税所得额 × 预扣率 - 速算扣除数
        BigDecimal cumulativeTax = getCumulativeTax(newCumulativeTaxableIncome);
        // 3. 本期应预扣预缴税额 = 累计应纳税额 - 累计已预扣预缴税额
        BigDecimal monthlyTax = cumulativeTax.subtract(cumulativeDeductedTax);
        // 4. 不小于 0
        if (monthlyTax.compareTo(BigDecimal.ZERO) < 0) {
            monthlyTax = BigDecimal.ZERO;
        }
        return monthlyTax.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 根据累计预扣预缴应纳税所得额，计算累计应纳税额。
     *
     * @param cumulativeTaxableIncome 累计预扣预缴应纳税所得额
     * @return 累计应纳税额
     */
    public static BigDecimal getCumulativeTax(BigDecimal cumulativeTaxableIncome) {
        if (cumulativeTaxableIncome == null || cumulativeTaxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        int idx = 0;
        for (int i = 0; i < BRACKETS.length; i++) {
            if (cumulativeTaxableIncome.compareTo(BRACKETS[i]) <= 0) {
                idx = i;
                break;
            }
            idx = BRACKETS.length;
        }
        BigDecimal tax = cumulativeTaxableIncome.multiply(RATES[idx])
                .subtract(QUICK_DEDUCTIONS[idx]);
        if (tax.compareTo(BigDecimal.ZERO) < 0) {
            tax = BigDecimal.ZERO;
        }
        return tax;
    }

    /**
     * 获取本月减除费用（5000 × 月数）
     *
     * @param month 月份(1-12)
     * @return 累计减除费用
     */
    public static BigDecimal getCumulativeDeduction(int month) {
        return MONTHLY_DEDUCTION.multiply(new BigDecimal(month));
    }

}