package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * HRM 员工档案导出 Excel 行 VO
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - HRM 员工档案导出 Excel 行 VO")
@Data
public class HrmEmployeeExportExcelVO {

    @ExcelProperty("员工编号")
    private Long id;

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

    @ExcelProperty("部门名称")
    private String deptName;

    @ExcelProperty("职位名称")
    private String postName;

    @ExcelProperty("聘用形式（1正式 2非正式）")
    private Integer type;

    @ExcelProperty("入职状态（1在职 2待入职 3待离职 4离职）")
    private Integer entryStatus;

    @ExcelProperty("入职时间")
    private LocalDateTime entryTime;

    @ExcelProperty("备注")
    private String remark;

}
