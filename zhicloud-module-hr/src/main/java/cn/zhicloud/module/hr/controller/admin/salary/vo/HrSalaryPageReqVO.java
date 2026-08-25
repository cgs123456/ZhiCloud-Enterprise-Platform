package cn.zhicloud.module.hr.controller.admin.salary.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HR 薪资记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrSalaryPageReqVO extends PageParam {

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "薪资月份", example = "202401")
    private String salaryMonth;

    @Schema(description = "状态", example = "10")
    private Integer status;

}