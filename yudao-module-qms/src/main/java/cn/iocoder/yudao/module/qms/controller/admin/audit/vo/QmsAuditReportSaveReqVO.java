package cn.iocoder.yudao.module.qms.controller.admin.audit.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.audit.QmsAuditConclusionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 审核报告新增/修改 Request VO")
@Data
public class QmsAuditReportSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "审核计划 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "审核计划 ID 不能为空")
    private Long planId;

    @Schema(description = "报告编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "AR-2024-001")
    @NotEmpty(message = "报告编号不能为空")
    private String reportNo;

    @Schema(description = "审核总结", example = "本次审核共发现 5 项不符合")
    private String auditSummary;

    @Schema(description = "审核结论", example = "20")
    @InEnum(QmsAuditConclusionEnum.class)
    private Integer conclusion;

    @Schema(description = "发现的不符合项数", example = "5")
    private Integer issueCount;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
