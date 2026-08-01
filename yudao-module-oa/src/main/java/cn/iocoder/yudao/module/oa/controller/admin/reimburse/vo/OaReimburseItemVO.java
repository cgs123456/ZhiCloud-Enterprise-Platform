package cn.iocoder.yudao.module.oa.controller.admin.reimburse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - OA 报销明细 VO")
@Data
public class OaReimburseItemVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "报销单 ID", example = "2048")
    private Long reimburseId;

    @Schema(description = "科目（如机票/住宿/餐饮）", requiredMode = Schema.RequiredMode.REQUIRED, example = "机票")
    @NotEmpty(message = "科目不能为空")
    private String subject;

    @Schema(description = "发生日期", example = "2024-01-01")
    private LocalDate occurrenceDate;

    @Schema(description = "金额", requiredMode = Schema.RequiredMode.REQUIRED, example = "500.00")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "发票张数", example = "1")
    private Integer invoiceCount;

    @Schema(description = "说明", example = "北京到上海")
    private String description;

}
