package cn.zhicloud.module.qms.controller.admin.audit.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 审核报告分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsAuditReportPageReqVO extends PageParam {

    @Schema(description = "审核计划 ID", example = "1024")
    private Long planId;

    @Schema(description = "报告编号", example = "AR-2024-001")
    private String reportNo;

    @Schema(description = "审核结论", example = "20")
    private Integer conclusion;

}
