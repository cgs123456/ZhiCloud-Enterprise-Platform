package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 员工自助端 - 绩效指标值 VO（填写 / 评分共用）
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 绩效指标值 VO")
@Data
public class HrmPortalPerformanceQuotaValueVO {

    @Schema(description = "考核指标编号")
    private Long quotaId;

    @Schema(description = "目标值")
    private String targetValue;

    @Schema(description = "实际值")
    private String actualValue;

    @Schema(description = "自评分")
    private BigDecimal selfScore;

    @Schema(description = "说明")
    private String comment;

}
