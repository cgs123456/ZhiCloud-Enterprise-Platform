package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工异动 Request VO（转正/调岗/晋升/降级/转全职共用）")
@Data
public class HrmEmployeeChangeReqVO {

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "异动原因：1组织架构调整 2个人申请 3工作安排 4违规违纪 5绩效不达标 6身体原因 7不适应岗位")
    private Integer reason;

    @Schema(description = "新部门编号；未填写表示不变")
    private Long newDeptId;

    @Schema(description = "新岗位名称；未填写表示不变")
    private String newPostName;

    @Schema(description = "新职级；未填写表示不变")
    private String newPostLevel;

    @Schema(description = "新工作地点；未填写表示不变")
    private String newWorkAddress;

    @Schema(description = "新直属上级员工编号；未填写表示不变")
    private Long newLeaderEmployeeId;

    @Schema(description = "试用期（月，转全职时使用）")
    private Integer probation;

    @Schema(description = "生效时间")
    private LocalDateTime effectTime;

    @Schema(description = "备注")
    private String remark;

}
