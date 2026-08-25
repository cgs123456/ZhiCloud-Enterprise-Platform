package cn.zhicloud.module.mes.controller.admin.dv.scada.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 设备数据采集记录 Response VO")
@Data
public class MesDvDeviceDataRecordRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "MES 设备编号", example = "100")
    private Long machineryId;

    @Schema(description = "MES 设备名称", example = "CNC 加工中心")
    private String machineryName;

    @Schema(description = "SCADA 配置编号", example = "10")
    private Long scadaConfigId;

    @Schema(description = "属性名称", example = "temperature")
    private String propertyName;

    @Schema(description = "属性值", example = "36.5")
    private String propertyValue;

    @Schema(description = "数据类型（10 数字 20 布尔 30 字符串）", example = "10")
    private Integer dataType;

    @Schema(description = "采集时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    @Schema(description = "状态（10 正常 20 异常）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
