package cn.zhicloud.module.wms.controller.admin.order.check.vo.cycle;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - WMS 循环盘点计划保存 Request VO")
@Data
public class WmsCheckCyclePlanSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "仓库不能为空")
    private Long warehouseId;

    @Schema(description = "ABC 分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "A")
    @NotBlank(message = "ABC 分类不能为空")
    @Size(max = 8, message = "ABC 分类长度不能超过 8 个字符")
    private String abcClassification;

    @Schema(description = "循环周期天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "循环周期天数不能为空")
    private Integer cycleDays;

    @Schema(description = "下次盘点日期", example = "2026-08-30")
    private LocalDate nextCheckDate;

    @Schema(description = "是否启用", example = "1")
    private Integer enabled;

    @Schema(description = "备注", example = "备注")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}