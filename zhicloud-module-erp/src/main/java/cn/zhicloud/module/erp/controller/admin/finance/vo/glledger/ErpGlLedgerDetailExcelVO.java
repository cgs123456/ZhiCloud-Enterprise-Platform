package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 管理后台 - ERP 账表明细账 Excel VO
 *
 * @author 智云
 */
@Data
public class ErpGlLedgerDetailExcelVO {

    @ExcelProperty("日期")
    private String accountDate;

    @ExcelProperty("凭证字号")
    private String voucherNumber;

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
