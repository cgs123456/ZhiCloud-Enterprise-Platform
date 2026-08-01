package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.accountbook;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpAccountBookStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpAccountingStandardEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - ERP 账簿新增/修改 Request VO")
@Data
public class ErpAccountBookSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "账簿编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOK-CAS")
    @NotBlank(message = "账簿编码不能为空")
    private String code;

    @Schema(description = "账簿名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国会计准则账簿")
    @NotBlank(message = "账簿名称不能为空")
    private String name;

    @Schema(description = "会计准则", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "会计准则不能为空")
    @InEnum(ErpAccountingStandardEnum.class)
    private Integer accountingStandard;

    @Schema(description = "本位币编号", example = "1")
    private Long currencyId;

    @Schema(description = "是否主账簿", example = "false")
    private Boolean isPrimary;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "状态不能为空")
    @InEnum(ErpAccountBookStatusEnum.class)
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "中国会计准则主账簿")
    private String remark;

}
