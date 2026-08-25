package cn.zhicloud.module.tms.controller.admin.freight.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Schema(description = "管理后台 - TMS 运费对账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TmsFreightReconciliationPageReqVO extends PageParam {

    @Schema(description = "对账单号", example = "FR001")
    private String no;

    @Schema(description = "承运商编号", example = "1")
    private Long carrierId;

    @Schema(description = "对账状态（0 待对账 / 10 已对账 / 20 有差异 / 30 已确认 / 40 已驳回）", example = "0")
    private Integer status;

    @Schema(description = "对账周期开始日期范围")
    private LocalDate[] periodStart;

}
