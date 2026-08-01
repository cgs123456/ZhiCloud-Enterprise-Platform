package cn.iocoder.yudao.module.qms.controller.admin.audit.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 审核报告 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsAuditReportRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "审核计划 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("审核计划 ID")
    private Long planId;

    @Schema(description = "报告编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "AR-2024-001")
    @ExcelProperty("报告编号")
    private String reportNo;

    @Schema(description = "审核总结", example = "本次审核共发现 5 项不符合")
    @ExcelProperty("审核总结")
    private String auditSummary;

    @Schema(description = "审核结论", example = "20")
    @ExcelProperty(value = "审核结论", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.AUDIT_CONCLUSION)
    private Integer conclusion;

    @Schema(description = "发现的不符合项数", example = "5")
    @ExcelProperty("发现的不符合项数")
    private Integer issueCount;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
