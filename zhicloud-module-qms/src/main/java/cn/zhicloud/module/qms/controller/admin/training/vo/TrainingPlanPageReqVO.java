package cn.zhicloud.module.qms.controller.admin.training.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 培训计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TrainingPlanPageReqVO extends PageParam {

    @Schema(description = "计划编号", example = "TP202401001")
    private String planNo;

    @Schema(description = "计划名称", example = "2024 年度质量培训")
    private String planName;

    @Schema(description = "年度", example = "2024")
    private Integer year;

    @Schema(description = "状态", example = "10")
    private Integer status;

}