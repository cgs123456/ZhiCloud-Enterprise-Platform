package cn.zhicloud.module.erp.controller.admin.finance.cashier.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 出纳单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCashierPageReqVO extends PageParam {

    @Schema(description = "出纳单号", example = "CND20260701000001")
    private String no;

    @Schema(description = "出纳类型 10收款/20付款/30内部转账", example = "10")
    private Integer cashierType;

    @Schema(description = "银行账户编号", example = "1")
    private Long bankAccountId;

    @Schema(description = "支付方式 10现金/20转账/30支票/40网银", example = "40")
    private Integer paymentMethod;

    @Schema(description = "状态 10待处理/20已提交银行/30已到账/40已退回", example = "10")
    private Integer status;

    @Schema(description = "对方名称", example = "供应商A")
    private String counterpartyName;

    @Schema(description = "关联业务单号", example = "XSDD20260701000001")
    private String businessOrderNo;

    @Schema(description = "支付日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] paymentDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] createTime;

}
