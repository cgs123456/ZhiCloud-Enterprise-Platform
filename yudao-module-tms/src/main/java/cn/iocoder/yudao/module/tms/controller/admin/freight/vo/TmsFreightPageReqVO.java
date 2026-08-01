package cn.iocoder.yudao.module.tms.controller.admin.freight.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - TMS 运费结算单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TmsFreightPageReqVO extends PageParam {

    @Schema(description = "结算单号", example = "FRT20240101001")
    private String no;

    @Schema(description = "运单编号", example = "1024")
    private Long shipmentId;

    @Schema(description = "承运商编号", example = "2048")
    private Long carrierId;

    @Schema(description = "计费方式（10 按重量 / 20 按体积 / 30 按件数 / 40 整车一口价 / 50 里程计费）", example = "10")
    private Integer billingMethod;

    @Schema(description = "结算状态（10 待审核 / 20 已审核 / 30 已结算 / 40 已驳回）", example = "10")
    private Integer status;

}
