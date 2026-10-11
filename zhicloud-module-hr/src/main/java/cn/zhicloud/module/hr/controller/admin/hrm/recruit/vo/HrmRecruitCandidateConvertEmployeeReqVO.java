package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 招聘候选人转员工 Request VO")
@Data
public class HrmRecruitCandidateConvertEmployeeReqVO {

    @Schema(description = "候选人编号")
    @NotNull(message = "候选人编号不能为空")
    private Long id;

    @Schema(description = "工号")
    private String jobNumber;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "直属上级员工编号")
    private Long leaderEmployeeId;

    @Schema(description = "聘用形式：1正式 2非正式")
    private Integer type;

    @Schema(description = "员工状态")
    private Integer status;

    @Schema(description = "入职时间")
    private LocalDateTime entryTime;

    @Schema(description = "试用期（月）")
    private Integer probation;

    @Schema(description = "职位名称")
    private String postName;

    @Schema(description = "岗位职级")
    private String postLevel;

    @Schema(description = "工作城市")
    private String workCity;

    @Schema(description = "工作地点")
    private String workAddress;

    @Schema(description = "备注")
    private String remark;

}
