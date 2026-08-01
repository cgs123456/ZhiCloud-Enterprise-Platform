package cn.iocoder.yudao.module.tms.controller.admin.gps.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - TMS GPS 定位 Response VO")
@Data
public class TmsGpsPositionRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "车辆编号", example = "3072")
    private Long vehicleId;

    @Schema(description = "运单编号", example = "2048")
    private Long shipmentId;

    @Schema(description = "经度", example = "116.4074")
    private BigDecimal longitude;

    @Schema(description = "纬度", example = "39.9042")
    private BigDecimal latitude;

    @Schema(description = "速度（km/h）", example = "65.5")
    private BigDecimal speed;

    @Schema(description = "方向（0-360度，0=正北）", example = "180")
    private BigDecimal direction;

    @Schema(description = "上报时间")
    private LocalDateTime reportTime;

    @Schema(description = "位置描述", example = "北京市朝阳区建国路88号")
    private String locationDesc;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
