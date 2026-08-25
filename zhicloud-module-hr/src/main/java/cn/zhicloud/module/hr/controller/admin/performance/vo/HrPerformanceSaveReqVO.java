package cn.zhicloud.module.hr.controller.admin.performance.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.hr.enums.performance.HrPerformanceGradeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - HR 绩效记录新增/修改 Request VO")
@Data
public class HrPerformanceSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @Schema(description = "考核周期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024Q1")
    @NotEmpty(message = "考核周期不能为空")
    private String period;

    @Schema(description = "考核得分", example = "95.5")
    private BigDecimal score;

    @Schema(description = "考核等级", example = "10")
    @InEnum(HrPerformanceGradeEnum.class)
    private Integer grade;

    @Schema(description = "考核人 ID", example = "4096")
    private Long evaluatorId;

    @Schema(description = "考核日期", example = "2024-03-31")
    private LocalDate evaluationDate;

    @Schema(description = "考核意见", example = "表现优秀")
    private String comment;

    @Schema(description = "备注", example = "随便")
    private String remark;

}