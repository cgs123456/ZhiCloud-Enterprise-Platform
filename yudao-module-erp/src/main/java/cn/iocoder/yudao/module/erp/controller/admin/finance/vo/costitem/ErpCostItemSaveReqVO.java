package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costitem;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.finance.cost.ErpCostItemTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - ERP 成本项目新增/修改 Request VO")
@Data
public class ErpCostItemSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "成本项目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "CI001")
    @NotBlank(message = "成本项目编码不能为空")
    private String code;

    @Schema(description = "成本项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "直接材料")
    @NotBlank(message = "成本项目名称不能为空")
    private String name;

    @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "类型不能为空")
    @InEnum(ErpCostItemTypeEnum.class)
    private Integer type;

    @Schema(description = "计算方法", example = "标准成本法")
    private String calculationMethod;

    @Schema(description = "是否标准成本（0 否 1 是）", example = "1")
    private Integer isStandard;

    @Schema(description = "备注", example = "原材料成本")
    private String remark;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

}
