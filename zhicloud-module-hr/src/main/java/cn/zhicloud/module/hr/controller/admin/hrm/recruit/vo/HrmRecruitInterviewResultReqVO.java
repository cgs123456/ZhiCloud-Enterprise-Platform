package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 招聘面试结果更新 Request VO")
@Data
public class HrmRecruitInterviewResultReqVO {

    @Schema(description = "面试编号")
    @NotNull(message = "面试编号不能为空")
    private Long id;

    @Schema(description = "面试结果")
    @NotNull(message = "面试结果不能为空")
    private Integer result;

    @Schema(description = "面试评价")
    private String evaluate;

    @Schema(description = "取消原因")
    private String cancelReason;

}
