package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 招聘候选人状态数量 Response VO")
@Data
public class HrmRecruitCandidateStatusCountRespVO {

    @Schema(description = "候选人状态")
    private Integer status;

    @Schema(description = "数量")
    private Long count;

}
