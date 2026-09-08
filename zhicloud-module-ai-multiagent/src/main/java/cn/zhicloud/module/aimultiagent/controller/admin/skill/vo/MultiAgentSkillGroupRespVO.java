package cn.zhicloud.module.aimultiagent.controller.admin.skill.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 管理后台 - 多 Agent 技能分组响应 VO（携带下属技能列表）
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - 多 Agent 技能分组（含技能）")
@Data
public class MultiAgentSkillGroupRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "分组编码", example = "wms")
    private String code;

    @Schema(description = "分组名称", example = "仓储物流")
    private String name;

    @Schema(description = "分组描述", example = "WMS 仓储物流模块技能")
    private String description;

    @Schema(description = "状态（0开启 1关闭）", example = "0")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "下属技能列表")
    private List<MultiAgentSkillRespVO> skills;

}
