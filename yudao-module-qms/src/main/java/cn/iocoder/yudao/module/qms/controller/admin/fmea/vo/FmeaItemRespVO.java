package cn.iocoder.yudao.module.qms.controller.admin.fmea.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS FMEA 条目 Response VO")
@Data
@ExcelIgnoreUnannotated
public class FmeaItemRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "FMEA 文档 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("FMEA 文档 ID")
    private Long fmeaId;

    @Schema(description = "功能", example = "密封")
    @ExcelProperty("功能")
    private String function;

    @Schema(description = "失效模式", requiredMode = Schema.RequiredMode.REQUIRED, example = "密封不良")
    @ExcelProperty("失效模式")
    private String failureMode;

    @Schema(description = "失效后果", example = "泄漏")
    @ExcelProperty("失效后果")
    private String failureEffect;

    @Schema(description = "严重度 S（1-10）", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    @ExcelProperty("严重度 S")
    private Integer severity;

    @Schema(description = "潜在失效原因", example = "密封圈尺寸不符")
    @ExcelProperty("潜在失效原因")
    private String potentialCause;

    @Schema(description = "频度 O（1-10）", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @ExcelProperty("频度 O")
    private Integer occurrence;

    @Schema(description = "现行控制措施", example = "全检")
    @ExcelProperty("现行控制措施")
    private String currentControls;

    @Schema(description = "探测度 D（1-10）", requiredMode = Schema.RequiredMode.REQUIRED, example = "4")
    @ExcelProperty("探测度 D")
    private Integer detection;

    @Schema(description = "风险优先数 RPN = S * O * D", example = "160")
    @ExcelProperty("RPN")
    private Integer rpn;

    @Schema(description = "行动优先级（AIAG-VDA 2019）：HIGH/MEDIUM/LOW", example = "HIGH")
    @ExcelProperty("行动优先级 AP")
    private String actionPriority;

    @Schema(description = "风险等级名称", example = "中风险")
    @ExcelProperty("风险等级")
    private String riskLevelName;

    @Schema(description = "风险等级颜色", example = "yellow")
    private String riskLevelColor;

    @Schema(description = "建议措施", example = "更换密封圈供应商")
    @ExcelProperty("建议措施")
    private String actionRecommended;

    @Schema(description = "已采取措施", example = "已更换供应商")
    @ExcelProperty("已采取措施")
    private String actionTaken;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
