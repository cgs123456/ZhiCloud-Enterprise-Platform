package cn.zhicloud.module.erp.controller.admin.finance.vo.fixedasset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 固定资产新增/修改 Request VO")
@Data
public class ErpFixedAssetSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "资产编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "FA-001")
    @NotEmpty(message = "资产编码不能为空")
    private String code;

    @Schema(description = "资产名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "服务器")
    @NotEmpty(message = "资产名称不能为空")
    private String name;

    @Schema(description = "资产类别", example = "办公设备")
    private String category;

    @Schema(description = "规格型号", example = "Dell R750")
    private String specification;

    @Schema(description = "部门编号", example = "1")
    private Long departmentId;

    @Schema(description = "存放地点", example = "机房 A")
    private String location;

    @Schema(description = "责任人", example = "张三")
    private String responsiblePerson;

    @Schema(description = "资产原值", requiredMode = Schema.RequiredMode.REQUIRED, example = "100000.00")
    @NotNull(message = "资产原值不能为空")
    private BigDecimal originalValue;

    @Schema(description = "预计残值", example = "5000.00")
    private BigDecimal salvageValue;

    @Schema(description = "预计使用年限（月数）", requiredMode = Schema.RequiredMode.REQUIRED, example = "60")
    @NotNull(message = "使用年限不能为空")
    private Integer usefulLifeMonths;

    @Schema(description = "折旧方法（10=直线法）", example = "10")
    private Integer depreciationMethod;

    @Schema(description = "入账日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "入账日期不能为空")
    private LocalDate capitalizationDate;

    @Schema(description = "对应资产科目编号", example = "1")
    private Long assetAccountId;

    @Schema(description = "对应累计折旧科目编号", example = "2")
    private Long accumulatedDepreciationAccountId;

    @Schema(description = "对应折旧费用科目编号", example = "3")
    private Long depreciationExpenseAccountId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
