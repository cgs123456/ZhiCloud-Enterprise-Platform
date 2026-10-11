package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - HRM 薪资组分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmSalaryGroupPageReqVO extends PageParam {

    @Schema(description = "薪资组名称，模糊匹配")
    private String name;

    @Schema(description = "计税规则编号")
    private Long taxRuleId;

}
