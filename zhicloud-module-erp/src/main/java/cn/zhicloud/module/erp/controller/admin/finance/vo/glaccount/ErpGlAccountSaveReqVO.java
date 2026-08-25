package cn.zhicloud.module.erp.controller.admin.finance.vo.glaccount;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.erp.enums.finance.ErpGlAccountBalanceDirectionEnum;
import cn.zhicloud.module.erp.enums.finance.ErpGlAccountTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 会计科目创建/更新 Request VO（P0-7）
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 会计科目创建/更新 Request VO")
@Data
public class ErpGlAccountSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "父级编号（顶级为 0）", example = "0")
    @NotNull(message = "父级编号不能为空")
    private Long parentId;

    @Schema(description = "科目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotBlank(message = "科目编码不能为空")
    private String code;

    @Schema(description = "科目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "库存现金")
    @NotBlank(message = "科目名称不能为空")
    private String name;

    @Schema(description = "科目类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "科目类型不能为空")
    @InEnum(ErpGlAccountTypeEnum.class)
    private Integer type;

    @Schema(description = "余额方向", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "余额方向不能为空")
    @InEnum(ErpGlAccountBalanceDirectionEnum.class)
    private Integer balanceDirection;

    @Schema(description = "是否末级科目", example = "true")
    private Boolean isLeaf;

    @Schema(description = "期初借方余额", example = "0")
    private BigDecimal openingDebit;

    @Schema(description = "期初贷方余额", example = "0")
    private BigDecimal openingCredit;

    @Schema(description = "状态（0 启用 / 1 禁用）", example = "0")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "现金科目")
    private String remark;

}
