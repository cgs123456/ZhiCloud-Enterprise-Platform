package cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - MES SCADA 设备配置新增/修改 Request VO")
@Data
public class MesDvScadaConfigSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "MES 设备编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "MES 设备编号不能为空")
    private Long machineryId;

    @Schema(description = "IoT 平台设备 PK", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNC-001")
    @NotEmpty(message = "IoT 设备 PK 不能为空")
    private String iotDevicePk;

    @Schema(description = "IoT 平台产品编号", example = "200")
    private Long iotProductId;

    @Schema(description = "SCADA 协议类型（MQTT/MODBUS_TCP/OPC-UA）", requiredMode = Schema.RequiredMode.REQUIRED, example = "MQTT")
    @NotEmpty(message = "协议类型不能为空")
    private String protocolType;

    @Schema(description = "点位映射配置（JSON）")
    private String pointConfig;

    @Schema(description = "是否启用（0 启用 1 停用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "是否启用不能为空")
    private Integer enabled;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
