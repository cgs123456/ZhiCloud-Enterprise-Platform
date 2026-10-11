package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HRM 员工薪资档案分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmSalaryEmployeeInfoPageReqVO extends PageParam {

    @Schema(description = "员工姓名，模糊匹配")
    private String employeeName;

    @Schema(description = "工号，模糊匹配")
    private String jobNumber;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "入职状态：1在职 2待入职 3待离职 4离职")
    private Integer entryStatus;

    @Schema(description = "员工状态：1正式 2试用 3实习 4兼职 5劳务 6顾问 7返聘 8外包")
    private Integer status;

    @Schema(description = "关键字（姓名/工号/手机号），模糊匹配")
    private String search;

}
