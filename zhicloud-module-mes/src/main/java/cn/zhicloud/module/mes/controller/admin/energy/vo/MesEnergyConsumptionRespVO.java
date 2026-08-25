package cn.zhicloud.module.mes.controller.admin.energy.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 能源消耗 Response VO")
@Data
public class MesEnergyConsumptionRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "车间编号", example = "1024")
    private Long workshopId;

    @Schema(description = "工位编号", example = "2048")
    private Long workstationId;

    @Schema(description = "能源类型（10 电 / 20 水 / 30 天然气 / 40 蒸汽 / 50 压缩空气）", example = "10")
    private Integer energyType;

    @Schema(description = "统计日期", example = "2024-01-01")
    private LocalDate recordDate;

    @Schema(description = "消耗量", example = "1500.5")
    private BigDecimal consumption;

    @Schema(description = "单位", example = "kWh")
    private String unit;

    @Schema(description = "单价", example = "0.85")
    private BigDecimal unitPrice;

    @Schema(description = "总金额", example = "1275.43")
    private BigDecimal totalAmount;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
