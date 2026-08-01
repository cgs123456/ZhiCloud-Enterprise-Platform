package cn.iocoder.yudao.module.qms.controller.admin.audit.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 审核不符合项分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsAuditNonconformityPageReqVO extends PageParam {

    @Schema(description = "审核报告 ID", example = "1024")
    private Long reportId;

    @Schema(description = "不符合项编号", example = "NC-2024-001")
    private String ncNo;

    @Schema(description = "严重程度", example = "20")
    private Integer severity;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "责任部门 ID", example = "2048")
    private Long responsibleDeptId;

}
