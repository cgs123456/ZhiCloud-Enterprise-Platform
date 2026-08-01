package cn.iocoder.yudao.module.qms.controller.admin.inspectionitem.vo;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionMethodEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - QMS 检验项目新增/修改 Request VO")
@Data
public class InspectionItemSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "检验项目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "IQC-001")
    @NotEmpty(message = "检验项目编码不能为空")
    private String code;

    @Schema(description = "检验项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "外观检查")
    @NotEmpty(message = "检验项目名称不能为空")
    private String name;

    @Schema(description = "检验类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "检验类型不能为空")
    @InEnum(InspectionTypeEnum.class)
    private Integer type;

    @Schema(description = "检验方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "检验方法不能为空")
    @InEnum(InspectionMethodEnum.class)
    private Integer method;

    @Schema(description = "检验标准", example = "GB/T 2828.1")
    private String standard;

    @Schema(description = "目标值", example = "10.0")
    private String target;

    @Schema(description = "上限", example = "10.5")
    private BigDecimal upperLimit;

    @Schema(description = "下限", example = "9.5")
    private BigDecimal lowerLimit;

    @Schema(description = "单位", example = "mm")
    private String unit;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

}
