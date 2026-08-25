package cn.zhicloud.module.qms.controller.admin.audit.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.qms.enums.audit.QmsNcSeverityEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 审核不符合项新增/修改 Request VO")
@Data
public class QmsAuditNonconformitySaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "审核报告 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "审核报告 ID 不能为空")
    private Long reportId;

    @Schema(description = "不符合项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "NC-2024-001")
    @NotEmpty(message = "不符合项编号不能为空")
    private String ncNo;

    @Schema(description = "严重程度", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "严重程度不能为空")
    @InEnum(QmsNcSeverityEnum.class)
    private Integer severity;

    @Schema(description = "不符合描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "未按程序文件执行首检")
    @NotEmpty(message = "不符合描述不能为空")
    private String description;

    @Schema(description = "不符合条款", example = "ISO 9001 8.2.1")
    private String clause;

    @Schema(description = "责任部门 ID", example = "2048")
    private Long responsibleDeptId;

    @Schema(description = "整改截止日期", example = "2024-04-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate correctiveActionDeadline;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
