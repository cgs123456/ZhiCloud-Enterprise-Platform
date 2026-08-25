package cn.zhicloud.module.erp.controller.admin.finance.vo.fundplan;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 资金计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpFundPlanPageReqVO extends PageParam {

    @Schema(description = "计划期间", example = "2026-07")
    private String planPeriod;

    @Schema(description = "计划类型（10 收款 20 付款）", example = "10")
    private Integer planType;

    @Schema(description = "银行账户编号", example = "1")
    private Long bankAccountId;

}