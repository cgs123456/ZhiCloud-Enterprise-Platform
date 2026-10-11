package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 员工自助端 - 处理绩效阶段 Request VO（结果审核 / 申诉处理共用）
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 处理绩效阶段 Request VO")
@Data
public class HrmPortalPerformanceHandleStageReqVO {

    @Schema(description = "员工绩效考核编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工绩效考核编号不能为空")
    private Long id;

    @Schema(description = "运行阶段编号")
    private Long stageId;

    @Schema(description = "是否通过：0驳回 1通过")
    private Integer pass;

    @Schema(description = "处理意见")
    private String comment;

}
