package cn.zhicloud.module.erp.controller.admin.finance.vo.cashflow;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 现金流记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCashFlowPageReqVO extends PageParam {

    @Schema(description = "业务类型（10 收款 20 付款）", example = "10")
    private Integer bizType;

    @Schema(description = "银行账户编号", example = "1")
    private Long bankAccountId;

    @Schema(description = "发生日期-起始", example = "2026-07-01")
    private LocalDate occurDateStart;

    @Schema(description = "发生日期-结束", example = "2026-07-31")
    private LocalDate occurDateEnd;

}