package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理后台 - ERP 账表科目余额表 Excel VO（树按 DFS 序扁平化，附科目级次保留层级语境）
 *
 * @author 智云
 */
@Data
public class ErpGlLedgerSubjectBalanceExcelVO {

    @ExcelProperty("科目级次")
    private Integer level;

    @ExcelProperty("科目编码")
    private String subjectCode;

    @ExcelProperty("科目名称")
    private String subjectName;

    @ExcelProperty("期初借方")
    private BigDecimal openingDebitAmount;

    @ExcelProperty("期初贷方")
    private BigDecimal openingCreditAmount;

    @ExcelProperty("本期借方")
    private BigDecimal periodDebitAmount;

    @ExcelProperty("本期贷方")
    private BigDecimal periodCreditAmount;

    @ExcelProperty("本年累计借方")
    private BigDecimal yearDebitAmount;

    @ExcelProperty("本年累计贷方")
    private BigDecimal yearCreditAmount;

    @ExcelProperty("期末借方")
    private BigDecimal endingDebitAmount;

    @ExcelProperty("期末贷方")
    private BigDecimal endingCreditAmount;

}
