package cn.iocoder.yudao.module.qms.controller.admin.capa.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS CAPA 文档 Response VO")
@Data
@ExcelIgnoreUnannotated
public class CAPADocumentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "CAPA 单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CAPA20240101001")
    @ExcelProperty("CAPA 单号")
    private String capaNo;

    @Schema(description = "来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "来源", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.CAPA_SOURCE)
    private Integer source;

    @Schema(description = "优先级", example = "20")
    @ExcelProperty(value = "优先级", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.CAPA_PRIORITY)
    private Integer priority;

    @Schema(description = "当前阶段", example = "10")
    @ExcelProperty(value = "当前阶段", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.CAPA_STAGE)
    private Integer stage;

    @Schema(description = "问题描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "产品外观不合格")
    @ExcelProperty("问题描述")
    private String problem;

    @Schema(description = "原因", example = "操作不当")
    @ExcelProperty("原因")
    private String cause;

    @Schema(description = "根本原因分析", example = "培训不足")
    @ExcelProperty("根本原因分析")
    private String rootCauseAnalysis;

    @Schema(description = "纠正措施", example = "重新培训")
    @ExcelProperty("纠正措施")
    private String correctiveAction;

    @Schema(description = "预防措施", example = "建立培训计划")
    @ExcelProperty("预防措施")
    private String preventiveAction;

    @Schema(description = "责任人", example = "芋头")
    @ExcelProperty("责任人")
    private String responsiblePerson;

    @Schema(description = "截止日期")
    @ExcelProperty("截止日期")
    private LocalDateTime dueDate;

    @Schema(description = "关闭日期")
    @ExcelProperty("关闭日期")
    private LocalDateTime closeDate;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.CAPA_STATUS)
    private Integer status;

    @Schema(description = "有效性验证结果", example = "20")
    @ExcelProperty(value = "验证结果", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.CAPA_VERIFICATION_RESULT)
    private Integer verificationResult;

    @Schema(description = "有效性验证意见", example = "措施有效，未再发生同类问题")
    @ExcelProperty("验证意见")
    private String verificationComment;

    @Schema(description = "验证人", example = "芋头")
    @ExcelProperty("验证人")
    private String verifiedBy;

    @Schema(description = "验证时间")
    @ExcelProperty("验证时间")
    private LocalDateTime verifiedTime;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
