package cn.iocoder.yudao.module.qms.controller.admin.audit.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 审核计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsAuditPlanPageReqVO extends PageParam {

    @Schema(description = "计划编号", example = "AUDIT-2024-001")
    private String planNo;

    @Schema(description = "审核标题", example = "2024 年度内审")
    private String title;

    @Schema(description = "审核类型", example = "10")
    private Integer auditType;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "主审 ID", example = "1024")
    private Long leadAuditorId;

    @Schema(description = "审核开始日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] auditStartDate;

}
