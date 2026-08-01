package cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES OEE 趋势 Response VO")
@Data
public class MesDvOeeTrendRespVO {

    @Schema(description = "记录日期")
    private LocalDateTime recordDate;

    @Schema(description = "可用率", example = "0.8750")
    private BigDecimal availability;

    @Schema(description = "表现率", example = "0.9524")
    private BigDecimal performance;

    @Schema(description = "质量率", example = "0.9500")
    private BigDecimal quality;

    @Schema(description = "OEE 值", example = "0.7917")
    private BigDecimal oee;

}
