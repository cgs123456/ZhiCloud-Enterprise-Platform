package cn.iocoder.yudao.module.qms.controller.admin.instrument.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 计量器具校准记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsInstrumentCalibrationRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "器具 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("器具 ID")
    private Long instrumentId;

    @Schema(description = "校准证书编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CAL20240101001")
    @ExcelProperty("校准证书编号")
    private String calibrationNo;

    @Schema(description = "校准日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @ExcelProperty("校准日期")
    private LocalDate calibrationDate;

    @Schema(description = "校准机构", example = "中国计量科学研究院")
    @ExcelProperty("校准机构")
    private String calibrationOrganization;

    @Schema(description = "校准结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "校准结果", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.CALIBRATION_RESULT)
    private Integer calibrationResult;

    @Schema(description = "校准证书附件 URL", example = "https://www.example.com/cert.pdf")
    private String calibrationCertificateUrl;

    @Schema(description = "偏差值", example = "0.001")
    @ExcelProperty("偏差值")
    private BigDecimal deviation;

    @Schema(description = "不确定度", example = "U=0.002mm (k=2)")
    @ExcelProperty("不确定度")
    private String uncertainty;

    @Schema(description = "下次校准日期", example = "2025-01-01")
    @ExcelProperty("下次校准日期")
    private LocalDate nextCalibrationDate;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
