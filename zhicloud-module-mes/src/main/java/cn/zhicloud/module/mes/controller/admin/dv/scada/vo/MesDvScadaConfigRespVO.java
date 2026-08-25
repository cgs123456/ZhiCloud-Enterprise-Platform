package cn.zhicloud.module.mes.controller.admin.dv.scada.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES SCADA 设备配置 Response VO")
@Data
public class MesDvScadaConfigRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "MES 设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long machineryId;

    @Schema(description = "MES 设备名称", example = "CNC 加工中心")
    private String machineryName;

    @Schema(description = "IoT 平台设备 PK", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNC-001")
    private String iotDevicePk;

    @Schema(description = "IoT 平台产品编号", example = "200")
    private Long iotProductId;

    @Schema(description = "SCADA 协议类型", example = "MQTT")
    private String protocolType;

    @Schema(description = "点位映射配置（JSON）")
    private String pointConfig;

    @Schema(description = "是否启用", example = "0")
    private Integer enabled;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
