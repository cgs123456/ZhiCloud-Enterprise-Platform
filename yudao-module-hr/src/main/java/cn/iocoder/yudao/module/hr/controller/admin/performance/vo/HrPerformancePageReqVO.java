package cn.iocoder.yudao.module.hr.controller.admin.performance.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HR 绩效记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrPerformancePageReqVO extends PageParam {

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "考核周期", example = "2024Q1")
    private String period;

    @Schema(description = "考核等级", example = "10")
    private Integer grade;

}