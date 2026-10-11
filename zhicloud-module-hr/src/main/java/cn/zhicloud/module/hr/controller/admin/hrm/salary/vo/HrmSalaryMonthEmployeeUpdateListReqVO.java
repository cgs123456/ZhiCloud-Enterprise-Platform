package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - HRM 员工月度工资批量修改 Request VO")
@Data
public class HrmSalaryMonthEmployeeUpdateListReqVO {

    @Schema(description = "月度工资表编号")
    @NotNull(message = "月度工资表编号不能为空")
    private Long monthRecordId;

    @Schema(description = "员工编号数组")
    @NotEmpty(message = "员工编号数组不能为空")
    private List<Long> employeeIds;

    @Schema(description = "工资项值")
    private List<SalaryOptionValueVO> optionValues;

}
