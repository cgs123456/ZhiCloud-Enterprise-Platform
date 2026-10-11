package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工紧急联系人 VO（列表 / 新增 / 修改共用）")
@Data
public class HrmEmployeeContactVO {

    @Schema(description = "联系人编号")
    private Long id;

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "联系人姓名")
    @NotEmpty(message = "联系人姓名不能为空")
    private String name;

    @Schema(description = "与员工关系")
    private String relation;

    @Schema(description = "联系人电话")
    private String phone;

    @Schema(description = "联系人工作单位")
    private String workUnit;

    @Schema(description = "联系人职务")
    private String postName;

    @Schema(description = "联系人地址")
    private String address;

}
