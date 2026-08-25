package cn.zhicloud.module.hr.controller.admin.employee.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.hr.enums.employee.HrEmployeeStatusEnum;
import cn.zhicloud.module.hr.enums.employee.HrEmploymentTypeEnum;
import cn.zhicloud.module.hr.enums.employee.HrGenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - HR 员工新增/修改 Request VO")
@Data
public class HrEmployeeSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "工号", requiredMode = Schema.RequiredMode.REQUIRED, example = "EMP001")
    @NotEmpty(message = "工号不能为空")
    private String empNo;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "姓名不能为空")
    private String name;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "性别不能为空")
    @InEnum(HrGenderEnum.class)
    private Integer gender;

    @Schema(description = "出生日期", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "身份证号", example = "110101199001011234")
    private String idCard;

    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@zhicloud.cn")
    private String email;

    @Schema(description = "部门 ID", example = "2048")
    private Long deptId;

    @Schema(description = "职位 ID", example = "4096")
    private Long positionId;

    @Schema(description = "入职日期", example = "2024-01-01")
    private LocalDate hireDate;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "状态不能为空")
    @InEnum(HrEmployeeStatusEnum.class)
    private Integer status;

    @Schema(description = "用工类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "用工类型不能为空")
    @InEnum(HrEmploymentTypeEnum.class)
    private Integer employmentType;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "1")
    private Integer sort;

}