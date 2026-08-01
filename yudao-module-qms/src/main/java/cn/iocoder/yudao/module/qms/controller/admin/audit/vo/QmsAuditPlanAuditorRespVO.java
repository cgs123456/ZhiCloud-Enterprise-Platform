package cn.iocoder.yudao.module.qms.controller.admin.audit.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 审核组成员 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsAuditPlanAuditorRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "审核计划 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("审核计划 ID")
    private Long planId;

    @Schema(description = "审核员 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @ExcelProperty("审核员 ID")
    private Long auditorId;

    @Schema(description = "角色", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "角色", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.AUDITOR_ROLE)
    private Integer role;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
