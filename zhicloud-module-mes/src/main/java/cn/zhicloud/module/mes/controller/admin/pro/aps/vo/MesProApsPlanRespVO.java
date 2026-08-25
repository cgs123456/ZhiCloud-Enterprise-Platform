package cn.zhicloud.module.mes.controller.admin.pro.aps.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.mes.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 排产计划 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesProApsPlanRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "排产计划编号", example = "APS-001")
    @ExcelProperty("排产计划编号")
    private String planNo;

    @Schema(description = "生产工单编号", example = "100")
    @ExcelProperty("生产工单编号")
    private Long workOrderId;

    @Schema(description = "产品编号", example = "200")
    @ExcelProperty("产品编号")
    private Long productId;

    @Schema(description = "工位编号", example = "300")
    @ExcelProperty("工位编号")
    private Long workstationId;

    @Schema(description = "计划开始时间")
    @ExcelProperty("计划开始时间")
    private LocalDateTime plannedStartTime;

    @Schema(description = "计划结束时间")
    @ExcelProperty("计划结束时间")
    private LocalDateTime plannedEndTime;

    @Schema(description = "排产数量", example = "100.00")
    @ExcelProperty("排产数量")
    private BigDecimal quantity;

    @Schema(description = "优先级", example = "1")
    @ExcelProperty(value = "优先级", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MES_PRO_APS_PLAN_PRIORITY)
    private Integer priority;

    @Schema(description = "状态", example = "0")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MES_PRO_APS_PLAN_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
