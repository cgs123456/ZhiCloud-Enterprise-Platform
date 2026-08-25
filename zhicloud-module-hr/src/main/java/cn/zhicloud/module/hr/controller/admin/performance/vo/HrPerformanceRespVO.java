package cn.zhicloud.module.hr.controller.admin.performance.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.hr.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 绩效记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HrPerformanceRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @ExcelProperty("员工 ID")
    private Long employeeId;

    @Schema(description = "考核周期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024Q1")
    @ExcelProperty("考核周期")
    private String period;

    @Schema(description = "考核得分", example = "95.5")
    @ExcelProperty("考核得分")
    private BigDecimal score;

    @Schema(description = "考核等级", example = "10")
    @ExcelProperty(value = "考核等级", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.HR_PERFORMANCE_GRADE)
    private Integer grade;

    @Schema(description = "考核人 ID", example = "4096")
    @ExcelProperty("考核人 ID")
    private Long evaluatorId;

    @Schema(description = "考核日期", example = "2024-03-31")
    @ExcelProperty("考核日期")
    private LocalDate evaluationDate;

    @Schema(description = "考核意见", example = "表现优秀")
    @ExcelProperty("考核意见")
    private String comment;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}