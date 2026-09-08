package cn.zhicloud.module.aimultiagent.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理后台 - 多 Agent 技能新建/更新请求 VO
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - 多 Agent 技能新建/更新请求")
@Data
public class MultiAgentSkillSaveReqVO {

    @Schema(description = "所属分组编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "所属分组不能为空")
    private Long groupId;

    @Schema(description = "技能编码（组内唯一）", requiredMode = Schema.RequiredMode.REQUIRED, example = "wms:receipt_order_list")
    @NotBlank(message = "技能编码不能为空")
    private String code;

    @Schema(description = "技能名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "收货单列表")
    @NotBlank(message = "技能名称不能为空")
    private String name;

    @Schema(description = "绑定的 WorkerToolExecutor 工具名（为空表示纯提示词技能）", example = "wms_receipt_order_list")
    private String toolName;

    @Schema(description = "技能描述", example = "查询 WMS 收货单真实数据")
    private String description;

    @Schema(description = "技能配置 JSON（可选 endpoint_url，保存时做 SSRF 校验）", example = "{\"endpoint_url\":\"https://api.example.com/data\"}")
    private String configJson;

    @Schema(description = "状态（0开启 1关闭）", example = "0")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

}
