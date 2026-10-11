package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 招聘面试新增/修改 Request VO")
@Data
public class HrmRecruitInterviewSaveReqVO {

    @Schema(description = "面试编号")
    private Long id;

    @Schema(description = "候选人编号")
    @NotNull(message = "候选人编号不能为空")
    private Long candidateId;

    @Schema(description = "面试方式")
    private Integer type;

    @Schema(description = "面试轮次")
    private Integer stageNumber;

    @Schema(description = "主面试官员工编号")
    private Long interviewEmployeeId;

    @Schema(description = "其他面试官员工编号数组")
    private List<Long> otherInterviewEmployeeIds;

    @Schema(description = "面试时间")
    private LocalDateTime interviewTime;

    @Schema(description = "面试地址")
    private String address;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "面试结果")
    private Integer result;

    @Schema(description = "面试评价")
    private String evaluate;

    @Schema(description = "取消原因")
    private String cancelReason;

}
