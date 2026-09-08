package cn.zhicloud.module.aimultiagent.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - 多 Agent 技能详情响应 VO
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - 多 Agent 技能详情")
@Data
public class MultiAgentSkillRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "所属分组编号", example = "1")
    private Long groupId;

    @Schema(description = "所属分组编码", example = "wms")
    private String groupCode;

    @Schema(description = "所属分组名称", example = "仓储物流")
    private String groupName;

    @Schema(description = "技能编码", example = "wms:receipt_order_list")
    private String code;

    @Schema(description = "技能名称", example = "收货单列表")
    private String name;

    @Schema(description = "绑定的工具名", example = "wms_receipt_order_list")
    private String toolName;

    @Schema(description = "技能描述", example = "查询 WMS 收货单真实数据")
    private String description;

    @Schema(description = "技能配置 JSON")
    private String configJson;

    @Schema(description = "状态（0开启 1关闭）", example = "0")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
