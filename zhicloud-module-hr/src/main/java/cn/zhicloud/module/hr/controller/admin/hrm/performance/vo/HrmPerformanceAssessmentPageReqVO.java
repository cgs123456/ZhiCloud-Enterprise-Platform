package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - HRM 员工绩效考核分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmPerformanceAssessmentPageReqVO extends PageParam {

    @Schema(description = "绩效计划编号")
    private Long planId;

    @Schema(description = "关键字（员工姓名/工号，模糊匹配）")
    private String search;

    @Schema(description = "部门编号数组")
    private Long[] deptIds;

    @Schema(description = "考核状态")
    private Integer status;

    @Schema(description = "当前阶段")
    private Integer stageType;

}
