package cn.zhicloud.module.qms.controller.admin.audit.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.qms.enums.audit.QmsAuditPlanStatusEnum;
import cn.zhicloud.module.qms.enums.audit.QmsAuditTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 审核计划新增/修改 Request VO")
@Data
public class QmsAuditPlanSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "计划编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "AUDIT-2024-001")
    @NotEmpty(message = "计划编号不能为空")
    private String planNo;

    @Schema(description = "审核类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "审核类型不能为空")
    @InEnum(QmsAuditTypeEnum.class)
    private Integer auditType;

    @Schema(description = "审核标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024 年度内审")
    @NotEmpty(message = "审核标题不能为空")
    private String title;

    @Schema(description = "审核依据", example = "ISO 9001:2015")
    private String auditStandard;

    @Schema(description = "审核范围", example = "全公司")
    private String auditScope;

    @Schema(description = "审核目的", example = "验证质量管理体系有效性")
    private String auditPurpose;

    @Schema(description = "主审 ID", example = "1024")
    private Long leadAuditorId;

    @Schema(description = "审核开始日期", example = "2024-03-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate auditStartDate;

    @Schema(description = "审核结束日期", example = "2024-03-05")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate auditEndDate;

    @Schema(description = "状态", example = "10")
    @InEnum(QmsAuditPlanStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
