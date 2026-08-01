package cn.iocoder.yudao.module.mes.controller.admin.energy.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDate;

@Schema(description = "管理后台 - MES 能源消耗分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesEnergyConsumptionPageReqVO extends PageParam {

    @Schema(description = "车间编号", example = "1024")
    private Long workshopId;

    @Schema(description = "工位编号", example = "2048")
    private Long workstationId;

    @Schema(description = "能源类型（10 电 / 20 水 / 30 天然气 / 40 蒸汽 / 50 压缩空气）", example = "10")
    private Integer energyType;

    @Schema(description = "统计日期范围")
    private LocalDate[] recordDate;

}
