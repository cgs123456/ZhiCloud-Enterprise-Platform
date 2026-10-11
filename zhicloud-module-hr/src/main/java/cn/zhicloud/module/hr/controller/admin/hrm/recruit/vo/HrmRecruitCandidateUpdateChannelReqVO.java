package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 招聘候选人渠道更新 Request VO")
@Data
public class HrmRecruitCandidateUpdateChannelReqVO {

    @Schema(description = "候选人编号")
    @NotNull(message = "候选人编号不能为空")
    private Long id;

    @Schema(description = "招聘渠道编号")
    @NotNull(message = "渠道编号不能为空")
    private Long channelId;

}
