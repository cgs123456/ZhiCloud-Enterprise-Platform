package cn.zhicloud.module.oa.controller.admin.approvaltemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - OA 审批模板简化 Response VO")
@Data
public class OaApprovalTemplateSimpleRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "LEAVE")
    private String code;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "请假审批")
    private String name;

    @Schema(description = "分类", example = "人事")
    private String category;

    @Schema(description = "图标", example = "leave")
    private String icon;

}
