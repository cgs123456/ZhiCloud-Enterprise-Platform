package cn.zhicloud.module.erp.controller.admin.finance.vo.currency;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - ERP 币种新增/修改 Request VO")
@Data
public class ErpCurrencySaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "币种编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNY")
    @NotBlank(message = "币种编码不能为空")
    private String code;

    @Schema(description = "币种名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "人民币")
    @NotBlank(message = "币种名称不能为空")
    private String name;

    @Schema(description = "币种符号", example = "¥")
    private String symbol;

    @Schema(description = "是否本位币", example = "false")
    private Boolean isBase;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @InEnum(CommonStatusEnum.class)
    private Integer enabled;

    @Schema(description = "备注", example = "本位币")
    private String remark;

}
