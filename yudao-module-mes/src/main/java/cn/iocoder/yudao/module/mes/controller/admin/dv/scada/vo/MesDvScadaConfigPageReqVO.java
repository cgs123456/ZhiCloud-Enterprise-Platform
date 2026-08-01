package cn.iocoder.yudao.module.mes.controller.admin.dv.scada.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - MES SCADA 设备配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesDvScadaConfigPageReqVO extends PageParam {

    @Schema(description = "MES 设备编号", example = "100")
    private Long machineryId;

    @Schema(description = "IoT 平台设备 PK", example = "CNC-001")
    private String iotDevicePk;

    @Schema(description = "SCADA 协议类型", example = "MQTT")
    private String protocolType;

    @Schema(description = "是否启用", example = "0")
    private Integer enabled;

}
