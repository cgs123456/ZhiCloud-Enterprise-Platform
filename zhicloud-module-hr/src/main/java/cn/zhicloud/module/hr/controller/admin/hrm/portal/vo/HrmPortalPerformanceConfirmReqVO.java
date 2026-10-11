package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 员工自助端 - 绩效确认 Request VO（目标确认 / 结果确认共用）
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 绩效确认 Request VO")
@Data
public class HrmPortalPerformanceConfirmReqVO {

    @Schema(description = "员工绩效考核编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工绩效考核编号不能为空")
    private Long assessmentId;

    @Schema(description = "运行阶段编号")
    private Long stageId;

    @Schema(description = "是否通过：0驳回 1通过", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "确认结果不能为空")
    private Integer pass;

    @Schema(description = "确认意见")
    private String comment;

}
