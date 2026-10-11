package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 员工自助端 - 绩效指标填写 Request VO
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 绩效指标填写 Request VO")
@Data
public class HrmPortalPerformanceQuotaFillReqVO {

    @Schema(description = "员工绩效考核编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工绩效考核编号不能为空")
    private Long assessmentId;

    @Schema(description = "运行阶段编号")
    private Long stageId;

    @Schema(description = "指标列表")
    private List<HrmPortalPerformanceQuotaValueVO> quotas;

}
