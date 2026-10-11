package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 招聘候选人职位更新 Request VO")
@Data
public class HrmRecruitCandidateUpdatePostReqVO {

    @Schema(description = "候选人编号")
    @NotNull(message = "候选人编号不能为空")
    private Long id;

    @Schema(description = "应聘职位编号")
    @NotNull(message = "职位编号不能为空")
    private Long postId;

}
