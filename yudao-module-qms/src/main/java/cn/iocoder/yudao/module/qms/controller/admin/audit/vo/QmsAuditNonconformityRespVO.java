package cn.iocoder.yudao.module.qms.controller.admin.audit.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 审核不符合项 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsAuditNonconformityRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "审核报告 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("审核报告 ID")
    private Long reportId;

    @Schema(description = "不符合项编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "NC-2024-001")
    @ExcelProperty("不符合项编号")
    private String ncNo;

    @Schema(description = "严重程度", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @ExcelProperty(value = "严重程度", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.NC_SEVERITY)
    private Integer severity;

    @Schema(description = "不符合描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "未按程序文件执行首检")
    @ExcelProperty("不符合描述")
    private String description;

    @Schema(description = "不符合条款", example = "ISO 9001 8.2.1")
    @ExcelProperty("不符合条款")
    private String clause;

    @Schema(description = "责任部门 ID", example = "2048")
    private Long responsibleDeptId;

    @Schema(description = "整改截止日期", example = "2024-04-01")
    @ExcelProperty("整改截止日期")
    private LocalDate correctiveActionDeadline;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.NC_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
