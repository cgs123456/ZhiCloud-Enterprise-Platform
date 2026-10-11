package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 招聘候选人新增/修改 Request VO")
@Data
public class HrmRecruitCandidateSaveReqVO {

    @Schema(description = "候选人编号")
    private Long id;

    @Schema(description = "候选人姓名")
    @NotEmpty(message = "候选人姓名不能为空")
    private String name;

    @Schema(description = "手机号码")
    @NotEmpty(message = "手机号码不能为空")
    private String mobile;

    @Schema(description = "性别")
    private Integer sex;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "应聘职位编号")
    private Long postId;

    @Schema(description = "用人部门编号")
    private Long deptId;

    @Schema(description = "招聘负责人员工编号")
    private Long ownerEmployeeId;

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

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "候选人状态")
    private Integer status;

    @Schema(description = "简历附件地址数组")
    private List<String> resumeUrls;

    @Schema(description = "入职时间")
    private LocalDateTime entryTime;

}
