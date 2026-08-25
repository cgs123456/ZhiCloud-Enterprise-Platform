package cn.zhicloud.module.mes.controller.admin.md.bom.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES BOM Response VO")
@Data
public class MesBomRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "BOM 编号", example = "BOM001")
    private String bomNo;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "产品名称", example = "成品A")
    private String productName;

    @Schema(description = "BOM 类型", example = "MANUFACTURING")
    private String bomType;

    @Schema(description = "版本号", example = "V1.0")
    private String version;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime createTime;

    @Schema(description = "BOM 单层成本（叶子项 unitCost 汇总）")
    private BigDecimal singleLayerCost;

    private static final String FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND = "yyyy-MM-dd HH:mm:ss";

}