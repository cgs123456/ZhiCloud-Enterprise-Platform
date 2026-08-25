package cn.zhicloud.module.tms.controller.admin.vehicle.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Schema(description = "管理后台 - TMS 车队运营分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TmsFleetOperationPageReqVO extends PageParam {

    @Schema(description = "车辆编号", example = "1")
    private Long vehicleId;

    @Schema(description = "运营日期范围")
    private LocalDate[] operationDate;

}
