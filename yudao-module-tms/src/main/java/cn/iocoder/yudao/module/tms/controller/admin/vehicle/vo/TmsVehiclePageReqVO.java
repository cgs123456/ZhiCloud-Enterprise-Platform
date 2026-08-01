package cn.iocoder.yudao.module.tms.controller.admin.vehicle.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - TMS 车辆分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TmsVehiclePageReqVO extends PageParam {

    @Schema(description = "车牌号", example = "京A12345")
    private String plateNo;

    @Schema(description = "承运商编号", example = "2048")
    private Long carrierId;

    @Schema(description = "状态（10 可用 / 20 运输中 / 30 维修中）", example = "10")
    private Integer status;

}
