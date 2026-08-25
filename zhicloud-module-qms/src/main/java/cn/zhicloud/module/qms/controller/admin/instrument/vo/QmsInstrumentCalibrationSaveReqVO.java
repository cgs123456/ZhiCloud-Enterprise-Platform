package cn.zhicloud.module.qms.controller.admin.instrument.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.qms.enums.instrument.QmsCalibrationResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 计量器具校准记录新增/修改 Request VO")
@Data
public class QmsInstrumentCalibrationSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "器具 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "器具 ID 不能为空")
    private Long instrumentId;

    @Schema(description = "校准证书编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CAL20240101001")
    @NotEmpty(message = "校准证书编号不能为空")
    private String calibrationNo;

    @Schema(description = "校准日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @NotNull(message = "校准日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate calibrationDate;

    @Schema(description = "校准机构", example = "中国计量科学研究院")
    private String calibrationOrganization;

    @Schema(description = "校准结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "校准结果不能为空")
    @InEnum(QmsCalibrationResultEnum.class)
    private Integer calibrationResult;

    @Schema(description = "校准证书附件 URL", example = "https://www.example.com/cert.pdf")
    private String calibrationCertificateUrl;

    @Schema(description = "偏差值", example = "0.001")
    private BigDecimal deviation;

    @Schema(description = "不确定度", example = "U=0.002mm (k=2)")
    private String uncertainty;

    @Schema(description = "下次校准日期", example = "2025-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate nextCalibrationDate;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
