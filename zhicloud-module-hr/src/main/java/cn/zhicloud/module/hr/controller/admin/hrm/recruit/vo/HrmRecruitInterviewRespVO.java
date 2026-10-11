package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 招聘面试 Response VO")
@Data
public class HrmRecruitInterviewRespVO {

    @Schema(description = "面试编号")
    private Long id;

    @Schema(description = "候选人编号")
    private Long candidateId;

    @Schema(description = "面试方式：1现场 2电话 3视频")
    private Integer type;

    @Schema(description = "面试轮次")
    private Integer stageNumber;

    @Schema(description = "主面试官员工编号")
    private Long interviewEmployeeId;

    @Schema(description = "主面试官姓名")
    private String interviewEmployeeName;

    @Schema(description = "其他面试官员工编号数组")
    private List<Long> otherInterviewEmployeeIds;

    @Schema(description = "其他面试官姓名数组")
    private List<String> otherInterviewEmployeeNames;

    @Schema(description = "面试时间")
    private LocalDateTime interviewTime;

    @Schema(description = "面试地址")
    private String address;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "面试结果：1未完成 2通过 3未通过 4取消")
    private Integer result;

    @Schema(description = "面试评价")
    private String evaluate;

    @Schema(description = "取消原因")
    private String cancelReason;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
