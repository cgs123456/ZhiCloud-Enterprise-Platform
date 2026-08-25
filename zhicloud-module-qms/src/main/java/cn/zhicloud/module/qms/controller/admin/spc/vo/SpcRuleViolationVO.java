package cn.zhicloud.module.qms.controller.admin.spc.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * SPC Western Electric 规则违反记录 VO
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - SPC 规则违反记录 Response VO")
@Data
public class SpcRuleViolationVO {

    @Schema(description = "规则编号（1-8）")
    private Integer ruleCode;

    @Schema(description = "规则描述")
    private String ruleDescription;

    @Schema(description = "违反规则的起始样本索引（从 0 开始）")
    private int startIndex;

    @Schema(description = "违反规则的结束样本索引")
    private int endIndex;

    @Schema(description = "涉及样本数")
    private int affectedCount;

}
