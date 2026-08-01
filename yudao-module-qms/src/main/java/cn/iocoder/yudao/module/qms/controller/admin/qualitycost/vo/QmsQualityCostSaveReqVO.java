package cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.qms.QmsQualityCostTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - QMS 质量成本新增/修改 Request VO")
@Data
public class QmsQualityCostSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "成本类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PREVENTION")
    @NotEmpty(message = "成本类型不能为空")
    @InEnum(QmsQualityCostTypeEnum.class)
    private String costType;

    @Schema(description = "成本类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "培训费")
    @NotEmpty(message = "成本类别不能为空")
    private String costCategory;

    @Schema(description = "成本项目", requiredMode = Schema.RequiredMode.REQUIRED, example = "六西格玛绿带培训")
    @NotEmpty(message = "成本项目不能为空")
    private String costItem;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "50000.0000")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "年度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024")
    @NotNull(message = "年度不能为空")
    private Integer periodYear;

    @Schema(description = "月份（1-12）", requiredMode = Schema.RequiredMode.REQUIRED, example = "6")
    @NotNull(message = "月份不能为空")
    private Integer periodMonth;

    @Schema(description = "关联业务 ID（如 8D 报告 ID/NCR ID/CAPA ID）", example = "1024")
    private Long relatedId;

    @Schema(description = "关联业务类型（EIGHT_D/NCR/CAPA）", example = "NCR")
    private String relatedType;

    @Schema(description = "备注", example = "随便")
    private String remark;

}