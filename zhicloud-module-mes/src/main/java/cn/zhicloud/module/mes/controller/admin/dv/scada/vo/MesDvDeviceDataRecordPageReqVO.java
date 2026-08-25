package cn.zhicloud.module.mes.controller.admin.dv.scada.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES 设备数据采集记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesDvDeviceDataRecordPageReqVO extends PageParam {

    @Schema(description = "MES 设备编号", example = "100")
    private Long machineryId;

    @Schema(description = "SCADA 配置编号", example = "10")
    private Long scadaConfigId;

    @Schema(description = "属性名称", example = "temperature")
    private String propertyName;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "采集时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] collectTime;

}
