package cn.iocoder.yudao.module.tms.controller.admin.tracking.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - TMS 跟踪事件上报 Request VO")
@Data
public class TmsTrackingEventSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "运单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "运单编号不能为空")
    private Long shipmentId;

    @Schema(description = "事件类型（10 发车 / 20 到达站点 / 30 签收 / 40 异常报告）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "事件类型不能为空")
    private Integer eventType;

    @Schema(description = "事件时间")
    private LocalDateTime eventTime;

    @Schema(description = "当前位置", example = "北京市朝阳区")
    private String location;

    @Schema(description = "经度", example = "116.4074")
    private BigDecimal longitude;

    @Schema(description = "纬度", example = "39.9042")
    private BigDecimal latitude;

    @Schema(description = "描述", example = "已发车")
    private String description;

}
