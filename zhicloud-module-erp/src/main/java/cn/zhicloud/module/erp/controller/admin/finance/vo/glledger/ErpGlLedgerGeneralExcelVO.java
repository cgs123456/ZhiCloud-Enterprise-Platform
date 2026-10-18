package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理后台 - ERP 账表总账 Excel VO
 *
 * @author 智云
 */
@Data
public class ErpGlLedgerGeneralExcelVO {

    @ExcelProperty("科目编码")
    private String subjectCode;

    @ExcelProperty("科目名称")
    private String subjectName;

    @ExcelProperty("期间")
    private String period;

    @ExcelProperty("摘要")
    private String digest;

    @ExcelProperty("借方")
    private BigDecimal debitAmount;

    @ExcelProperty("贷方")
    private BigDecimal creditAmount;

    @ExcelProperty("方向")
    private String balanceDirection;

    @ExcelProperty("余额")
    private BigDecimal balance;

}
