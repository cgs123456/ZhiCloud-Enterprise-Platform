package cn.zhicloud.module.erp.controller.admin.finance.vo.costcenter;

import cn.zhicloud.framework.common.enums.CommonStatusEnum;
import cn.zhicloud.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - ERP 成本中心新增/修改 Request VO")
@Data
public class ErpCostCenterSaveReqVO {

    @Schema(description = "编号（更新时必填）", example = "1024")
    private Long id;

    @Schema(description = "成本中心编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "CC1001")
    @NotBlank(message = "成本中心编码不能为空")
    private String code;

    @Schema(description = "成本中心名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "总部")
    @NotBlank(message = "成本中心名称不能为空")
    private String name;

    @Schema(description = "父级编号（顶级为 0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "父级编号不能为空")
    private Long parentId;

    @Schema(description = "负责人 ID", example = "1")
    private Long managerId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "一级成本中心")
    private String remark;

}
