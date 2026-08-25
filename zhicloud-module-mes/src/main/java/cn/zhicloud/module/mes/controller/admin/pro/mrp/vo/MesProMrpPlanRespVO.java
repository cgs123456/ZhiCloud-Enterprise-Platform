package cn.zhicloud.module.mes.controller.admin.pro.mrp.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.mes.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES MRP 计划 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesProMrpPlanRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "MRP 计划编号", example = "MRP-001")
    @ExcelProperty("MRP 计划编号")
    private String planNo;

    @Schema(description = "计划日期")
    @ExcelProperty("计划日期")
    private LocalDateTime planDate;

    @Schema(description = "状态", example = "0")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MES_PRO_MRP_PLAN_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
