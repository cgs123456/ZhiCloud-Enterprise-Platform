package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 员工自助端 - 绩效考核分页 Request VO
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 绩效考核分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmPortalPerformanceAssessmentPageReqVO extends PageParam {

    @Schema(description = "关键字（考核名称，模糊匹配）")
    private String search;

    @Schema(description = "绩效计划编号")
    private Long planId;

    @Schema(description = "当前阶段")
    private Integer stageType;

    @Schema(description = "考核状态")
    private Integer status;

    @Schema(description = "任务状态：1已处理 2待处理 5已申诉")
    private Integer stageStatus;

}
