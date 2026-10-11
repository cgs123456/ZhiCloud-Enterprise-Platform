package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 招聘候选人 Response VO")
@Data
public class HrmRecruitCandidateRespVO {

    @Schema(description = "候选人编号")
    private Long id;

    @Schema(description = "候选人姓名")
    private String name;

    @Schema(description = "手机号码")
    private String mobile;

    @Schema(description = "性别")
    private Integer sex;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "应聘职位编号")
    private Long postId;

    @Schema(description = "应聘职位名称")
    private String postName;

    @Schema(description = "应聘职位状态")
    private Integer postStatus;

    @Schema(description = "用人部门编号")
    private Long deptId;

    @Schema(description = "用人部门名称")
    private String deptName;

    @Schema(description = "招聘负责人员工编号")
    private Long ownerEmployeeId;

    @Schema(description = "招聘负责人姓名")
    private String ownerEmployeeName;

    @Schema(description = "面试轮次")
    private Integer stageNumber;

    @Schema(description = "工作年限")
    private Integer workTime;

    @Schema(description = "学历")
    private Integer education;

    @Schema(description = "毕业院校")
    private String graduateSchool;

    @Schema(description = "最近工作单位")
    private String latestWorkPlace;

    @Schema(description = "招聘渠道编号")
    private Long channelId;

    @Schema(description = "招聘渠道名称")
    private String channelName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "候选人状态")
    private Integer status;

    @Schema(description = "淘汰原因")
    private String eliminate;

    @Schema(description = "状态更新时间")
    private LocalDateTime statusUpdateTime;

    @Schema(description = "入职时间")
    private LocalDateTime entryTime;

    @Schema(description = "简历附件地址数组")
    private List<String> resumeUrls;

    @Schema(description = "当前面试编号")
    private Long interviewId;

    @Schema(description = "面试方式")
    private Integer interviewType;

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
    private String interviewAddress;

    @Schema(description = "面试结果")
    private Integer interviewResult;

    @Schema(description = "转入的员工编号")
    private Long employeeId;

    @Schema(description = "创建人用户编号")
    private String creator;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
