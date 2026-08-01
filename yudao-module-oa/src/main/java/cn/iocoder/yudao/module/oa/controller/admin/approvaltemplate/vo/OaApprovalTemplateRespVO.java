package cn.iocoder.yudao.module.oa.controller.admin.approvaltemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 审批模板 Response VO")
@Data
public class OaApprovalTemplateRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "模板编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "LEAVE")
    private String code;

    @Schema(description = "模板名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "请假审批")
    private String name;

    @Schema(description = "分类（通用/人事/财务/采购/合同/其他）", example = "人事")
    private String category;

    @Schema(description = "BPM 流程定义 KEY", requiredMode = Schema.RequiredMode.REQUIRED, example = "oa_leave")
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

    @Schema(description = "使用次数", example = "10")
    private Integer usageCount;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
