package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - HRM 工资条发放 Request VO")
@Data
public class HrmSalarySlipSendRecordCreateReqVO {

    @Schema(description = "月度工资表编号")
    @NotNull(message = "月度工资表编号不能为空")
    private Long monthRecordId;

    @Schema(description = "是否隐藏空工资项")
    private Boolean hideEmpty;

    @Schema(description = "本次发放的工资条模板项")
    private List<HrmSalarySlipTemplateOptionVO> options;

    @Schema(description = "是否发放全部筛选结果")
    private Boolean all;

    @Schema(description = "员工编号数组")
    private List<Long> employeeIds;

    @Schema(description = "员工姓名、工号或手机号")
    private String search;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "是否已发送")
    private Boolean sent;

}
