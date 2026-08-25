package cn.zhicloud.module.erp.controller.admin.finance.vo.period;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.erp.enums.finance.ErpPeriodStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * ERP 会计期间创建/更新 Request VO（P0-6）
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 会计期间创建/更新 Request VO")
@Data
public class ErpPeriodSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "年度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "年度不能为空")
    private Integer year;

    @Schema(description = "月份", requiredMode = Schema.RequiredMode.REQUIRED, example = "7")
    @NotNull(message = "月份不能为空")
    private Integer month;

    @Schema(description = "期间编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "202607")
    @NotBlank(message = "期间编码不能为空")
    private String code;

    @Schema(description = "起始日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "起始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "状态", example = "10")
    @InEnum(ErpPeriodStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "2026年7月会计期间")
    private String remark;

}
