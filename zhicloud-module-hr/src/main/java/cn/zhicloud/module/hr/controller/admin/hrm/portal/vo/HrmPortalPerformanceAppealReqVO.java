package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 员工自助端 - 提交绩效申诉 Request VO
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 提交绩效申诉 Request VO")
@Data
public class HrmPortalPerformanceAppealReqVO {

    @Schema(description = "员工绩效考核编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工绩效考核编号不能为空")
    private Long assessmentId;

    @Schema(description = "申诉原因", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "申诉原因不能为空")
    private String appealReason;

    @Schema(description = "申诉附件地址列表")
    private List<String> appealFileUrls;

    @Schema(description = "退回评分阶段编号列表")
    private List<Long> reviewStageIds;

}
