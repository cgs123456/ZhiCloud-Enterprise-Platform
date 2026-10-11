package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HRM 员工再入职 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmEmployeeRehireReqVO extends HrmEmployeeSaveReqVO {

    @Schema(description = "员工编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

}
