package cn.iocoder.yudao.module.erp.controller.admin.sale.credit.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 客户信用额度 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpCreditLimitRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long customerId;

    @Schema(description = "客户名称", example = "张三")
    @ExcelProperty("客户名称")
    private String customerName;

    @Schema(description = "信用额度", example = "100000.00")
    @ExcelProperty("信用额度")
    private BigDecimal creditLimit;

    @Schema(description = "已用额度", example = "20000.00")
    @ExcelProperty("已用额度")
    private BigDecimal usedAmount;

    @Schema(description = "可用额度", example = "80000.00")
    @ExcelProperty("可用额度")
    private BigDecimal availableAmount;

    @Schema(description = "逾期金额", example = "0.00")
    @ExcelProperty("逾期金额")
    private BigDecimal overdueAmount;

    @Schema(description = "预警比例", example = "80")
    @ExcelProperty("预警比例")
    private BigDecimal warningRatio;

    @Schema(description = "状态 10正常/20预警/30冻结", example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建人", example = "芋道")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
