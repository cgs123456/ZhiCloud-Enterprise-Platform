package cn.iocoder.yudao.module.hr.controller.admin.employee.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.hr.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 员工 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HrEmployeeRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "工号", requiredMode = Schema.RequiredMode.REQUIRED, example = "EMP001")
    @ExcelProperty("工号")
    private String empNo;

    @Schema(description = "姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @ExcelProperty("姓名")
    private String name;

    @Schema(description = "性别", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "性别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.HR_GENDER)
    private Integer gender;

    @Schema(description = "出生日期", example = "1990-01-01")
    @ExcelProperty("出生日期")
    private LocalDate birthDate;

    @Schema(description = "身份证号", example = "110101199001011234")
    @ExcelProperty("身份证号")
    private String idCard;

    @Schema(description = "联系电话", example = "13800138000")
    @ExcelProperty("联系电话")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@iocoder.cn")
    @ExcelProperty("邮箱")
    private String email;

    @Schema(description = "部门 ID", example = "2048")
    @ExcelProperty("部门 ID")
    private Long deptId;

    @Schema(description = "职位 ID", example = "4096")
    @ExcelProperty("职位 ID")
    private Long positionId;

    @Schema(description = "入职日期", example = "2024-01-01")
    @ExcelProperty("入职日期")
    private LocalDate hireDate;

    @Schema(description = "离职日期", example = "2024-12-31")
    @ExcelProperty("离职日期")
    private LocalDate leaveDate;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.HR_EMPLOYEE_STATUS)
    private Integer status;

    @Schema(description = "用工类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "用工类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.HR_EMPLOYMENT_TYPE)
    private Integer employmentType;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}