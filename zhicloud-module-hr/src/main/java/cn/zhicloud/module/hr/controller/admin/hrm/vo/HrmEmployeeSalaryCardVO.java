package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工工资卡 VO（get/save 共用）")
@Data
public class HrmEmployeeSalaryCardVO {

    @Schema(description = "工资卡编号（员工 id）")
    private Long id;

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "银行卡号")
    private String bankCardNumber;

    @Schema(description = "开户地区编号")
    private Long bankAreaId;

    @Schema(description = "开户地区名称")
    private String bankAreaName;

    @Schema(description = "银行名称")
    private String bankName;

    @Schema(description = "开户支行名称")
    private String bankBranchName;

}
