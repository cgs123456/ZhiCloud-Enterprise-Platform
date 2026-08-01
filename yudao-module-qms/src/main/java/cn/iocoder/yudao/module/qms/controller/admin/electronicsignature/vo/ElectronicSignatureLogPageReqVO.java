package cn.iocoder.yudao.module.qms.controller.admin.electronicsignature.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - QMS 电子签名记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ElectronicSignatureLogPageReqVO extends PageParam {

    @Schema(description = "用户 ID", example = "1024")
    private Long userId;

    @Schema(description = "签名含义", example = "批准")
    private String signatureMeaning;

    @Schema(description = "操作类型", example = "CAPA_CLOSE")
    private String operationType;

    @Schema(description = "签名时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] signatureTime;

}
