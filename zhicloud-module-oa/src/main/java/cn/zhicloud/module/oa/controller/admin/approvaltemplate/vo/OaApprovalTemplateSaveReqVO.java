package cn.zhicloud.module.oa.controller.admin.approvaltemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - OA 审批模板新增/修改 Request VO")
@Data
public class OaApprovalTemplateSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "LEAVE")
    @NotEmpty(message = "模板编码不能为空")
    private String code;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "请假审批")
    @NotEmpty(message = "模板名称不能为空")
    private String name;

    @Schema(description = "分类（通用/人事/财务/采购/合同/其他）", example = "人事")
    private String category;

    @Schema(description = "BPM 流程定义 KEY", requiredMode = Schema.RequiredMode.REQUIRED, example = "oa_leave")
    @NotEmpty(message = "BPM 流程定义 KEY 不能为空")
    private String processDefinitionKey;

    @Schema(description = "表单 JSON Schema（字段定义）", example = "{\"fields\":[]}")
    private String formSchema;

    @Schema(description = "表单 UI Schema（布局/校验）", example = "{\"layout\":\"vertical\"}")
    private String formUiSchema;

    @Schema(description = "描述", example = "员工请假审批模板")
    private String description;

    @Schema(description = "图标", example = "leave")
    private String icon;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态（0 启用 1 停用）", example = "0")
    private Integer status;

}
