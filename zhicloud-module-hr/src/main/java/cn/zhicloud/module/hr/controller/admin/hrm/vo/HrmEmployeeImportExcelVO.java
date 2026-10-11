package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * HRM 员工档案导入 Excel 行 VO（同时用作导入模板列）
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - HRM 员工档案导入 Excel 行 VO")
@Data
public class HrmEmployeeImportExcelVO {

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("工号")
    private String jobNumber;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("性别（1男 2女）")
    private Integer sex;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("部门编号")
    private Long deptId;

    @ExcelProperty("职位名称")
    private String postName;

    @ExcelProperty("聘用形式（1正式 2非正式）")
    private Integer type;

    @ExcelProperty("员工状态（1正式 2试用 3实习 4兼职 5劳务 6顾问 7返聘 8外包）")
    private Integer status;

    @ExcelProperty("入职时间")
    private LocalDateTime entryTime;

    @ExcelProperty("证件号码")
    private String idNumber;

    @ExcelProperty("备注")
    private String remark;

}
