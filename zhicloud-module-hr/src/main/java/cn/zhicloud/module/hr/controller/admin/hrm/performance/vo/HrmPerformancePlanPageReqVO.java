package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - HRM 绩效计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmPerformancePlanPageReqVO extends PageParam {

    @Schema(description = "计划名称，模糊匹配")
    private String name;

    @Schema(description = "计划状态")
    private Integer status;

    @Schema(description = "考核周期类型")
    private Integer cycleType;

    @Schema(description = "计划状态数组")
    private Integer[] statuses;

}
