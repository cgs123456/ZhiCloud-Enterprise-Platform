package cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ERP 期初余额 Excel VO（导出 / 导入模板 / 导入共用，C6③ / ADR 265）
 *
 * <p>导入口径：按科目编码匹配（必须存在、末级、本租户）；四金额全空的行跳过
 * （模板未动过）；任一金额非空的行按 null=0 写入（显式填 0 = 清零）。
 *
 * @author 智云
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErpGlInitialBalanceExcelVO {

    @ExcelProperty("科目编码")
    private String subjectCode;

    @ExcelProperty("科目名称")
    private String subjectName;

    @ExcelProperty("期初余额")
    private BigDecimal openingAmount;

    @ExcelProperty("本年累计借方")
    private BigDecimal yearDebitAmount;

    @ExcelProperty("本年累计贷方")
    private BigDecimal yearCreditAmount;

    @ExcelProperty("实际损益发生额")
    private BigDecimal profitLossAmount;

}
