package cn.zhicloud.module.mes.controller.admin.energy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - MES 能源消耗新增/修改 Request VO")
@Data
public class MesEnergyConsumptionSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "车间编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "车间编号不能为空")
    private Long workshopId;

    @Schema(description = "工位编号", example = "2048")
    private Long workstationId;

    @Schema(description = "能源类型（10 电 / 20 水 / 30 天然气 / 40 蒸汽 / 50 压缩空气）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "能源类型不能为空")
    private Integer energyType;

    @Schema(description = "统计日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @NotNull(message = "统计日期不能为空")
    private LocalDate recordDate;

    @Schema(description = "消耗量", requiredMode = Schema.RequiredMode.REQUIRED, example = "1500.5")
    @NotNull(message = "消耗量不能为空")
    private BigDecimal consumption;

    @Schema(description = "单位", example = "kWh")
    private String unit;

    @Schema(description = "单价", example = "0.85")
    private BigDecimal unitPrice;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
