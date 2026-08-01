package cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 质量成本 Response VO")
@Data
public class QmsQualityCostRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "成本类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PREVENTION")
    private String costType;

    @Schema(description = "成本类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "培训费")
    private String costCategory;

    @Schema(description = "成本项目", requiredMode = Schema.RequiredMode.REQUIRED, example = "六西格玛绿带培训")
    private String costItem;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "50000.0000")
    private BigDecimal amount;

    @Schema(description = "年度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024")
    private Integer periodYear;

    @Schema(description = "月份（1-12）", requiredMode = Schema.RequiredMode.REQUIRED, example = "6")
    private Integer periodMonth;

    @Schema(description = "关联业务 ID", example = "1024")
    private Long relatedId;

    @Schema(description = "关联业务类型（EIGHT_D/NCR/CAPA）", example = "NCR")
    private String relatedType;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}