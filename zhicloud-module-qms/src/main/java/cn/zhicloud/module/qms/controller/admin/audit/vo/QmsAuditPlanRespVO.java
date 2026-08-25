package cn.zhicloud.module.qms.controller.admin.audit.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 审核计划 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsAuditPlanRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "AUDIT-2024-001")
    @ExcelProperty("计划编号")
    private String planNo;

    @Schema(description = "审核类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "审核类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.AUDIT_TYPE)
    private Integer auditType;

    @Schema(description = "审核标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024 年度内审")
    @ExcelProperty("审核标题")
    private String title;

    @Schema(description = "审核依据", example = "ISO 9001:2015")
    @ExcelProperty("审核依据")
    private String auditStandard;

    @Schema(description = "审核范围", example = "全公司")
    @ExcelProperty("审核范围")
    private String auditScope;

    @Schema(description = "审核目的", example = "验证质量管理体系有效性")
    @ExcelProperty("审核目的")
    private String auditPurpose;

    @Schema(description = "主审 ID", example = "1024")
    private Long leadAuditorId;

    @Schema(description = "审核开始日期", example = "2024-03-01")
    @ExcelProperty("审核开始日期")
    private LocalDate auditStartDate;

    @Schema(description = "审核结束日期", example = "2024-03-05")
    @ExcelProperty("审核结束日期")
    private LocalDate auditEndDate;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.AUDIT_PLAN_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
