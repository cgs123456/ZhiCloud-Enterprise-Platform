package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 从后台用户批量创建员工档案 Request VO")
@Data
public class HrmEmployeeCreateFromUserReqVO {

    @Schema(description = "后台用户编号")
    @NotNull(message = "后台用户编号不能为空")
    private Long userId;

    @Schema(description = "工号")
    @NotEmpty(message = "工号不能为空")
    private String jobNumber;

    @Schema(description = "员工手机号")
    @NotEmpty(message = "员工手机号不能为空")
    private String mobile;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "直属上级员工编号")
    private Long leaderEmployeeId;

    @Schema(description = "聘用形式：1正式 2非正式")
    @NotNull(message = "聘用形式不能为空")
    private Integer type;

    @Schema(description = "非正式员工状态：2试用 3实习 4兼职 5劳务 6顾问 7返聘 8外包")
    private Integer status;

    @Schema(description = "入职时间")
    @NotNull(message = "入职时间不能为空")
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
