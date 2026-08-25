package cn.zhicloud.module.wms.controller.admin.order.check.vo.cycle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - WMS 循环盘点计划 Response VO")
@Data
public class WmsCheckCyclePlanRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long warehouseId;

    @Schema(description = "仓库名称", example = "北京仓")
    private String warehouseName;

    @Schema(description = "ABC 分类", requiredMode = Schema.RequiredMode.REQUIRED, example = "A")
    private String abcClassification;

    @Schema(description = "循环周期天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    private Integer cycleDays;

    @Schema(description = "下次盘点日期", example = "2026-08-30")
    private LocalDate nextCheckDate;

    @Schema(description = "是否启用", example = "1")
    private Integer enabled;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}