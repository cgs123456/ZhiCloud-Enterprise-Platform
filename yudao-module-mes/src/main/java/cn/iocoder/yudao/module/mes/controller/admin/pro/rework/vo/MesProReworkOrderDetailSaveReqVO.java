package cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 返工工单明细新增/修改 Request VO")
@Data
public class MesProReworkOrderDetailSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "返工工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "返工工单不能为空")
    private Long reworkOrderId;

    @Schema(description = "缺陷描述", example = "尺寸超差 0.5mm")
    private String defectDescription;

    @Schema(description = "缺陷数量", example = "5.00")
    private BigDecimal defectQuantity;

    @Schema(description = "缺陷类型（10 尺寸不良 / 20 外观不良 / 30 功能不良 / 40 性能不良 / 50 其他）", example = "10")
    private Integer defectType;

    @Schema(description = "处理方式（10 返修 / 20 降级 / 30 报废 / 40 重新加工）", example = "10")
    private Integer repairMethod;

    @Schema(description = "处理描述", example = "重新焊接")
    private String repairDescription;

    @Schema(description = "已处理数量", example = "5.00")
    private BigDecimal repairedQuantity;

    @Schema(description = "显示顺序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
