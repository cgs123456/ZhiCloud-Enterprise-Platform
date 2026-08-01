package cn.iocoder.yudao.module.qms.controller.admin.msa.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS MSA 研究 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MsaStudyRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "研究编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "MSA20240101001")
    @ExcelProperty("研究编号")
    private String studyNo;

    @Schema(description = "研究类型", example = "10")
    @ExcelProperty(value = "研究类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MSA_STUDY_TYPE)
    private Integer studyType;

    @Schema(description = "特性名称", example = "外径")
    @ExcelProperty("特性名称")
    private String characteristicName;

    @Schema(description = "测量设备 ID", example = "1024")
    private Long equipmentId;

    @Schema(description = "评价人数量", example = "3")
    @ExcelProperty("评价人数量")
    private Integer appraiserCount;

    @Schema(description = "试验次数", example = "3")
    @ExcelProperty("试验次数")
    private Integer trialCount;

    @Schema(description = "零件数量", example = "10")
    @ExcelProperty("零件数量")
    private Integer partCount;

    @Schema(description = "状态", example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.MSA_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
