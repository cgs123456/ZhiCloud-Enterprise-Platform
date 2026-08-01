package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fixedasset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 固定资产 Response VO")
@Data
public class ErpFixedAssetRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;
    @Schema(description = "资产编码", example = "FA-001")
    private String code;
    @Schema(description = "资产名称", example = "服务器")
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
    @Schema(description = "资产原值", example = "100000.00")
    private BigDecimal originalValue;
    @Schema(description = "预计残值", example = "5000.00")
    private BigDecimal salvageValue;
    @Schema(description = "预计使用年限（月数）", example = "60")
    private Integer usefulLifeMonths;
    @Schema(description = "折旧方法（10=直线法）", example = "10")
    private Integer depreciationMethod;
    @Schema(description = "折旧方法名称", example = "直线法")
    private String depreciationMethodName;
    @Schema(description = "入账日期")
    private LocalDate capitalizationDate;
    @Schema(description = "对应资产科目编号", example = "1")
    private Long assetAccountId;
    @Schema(description = "对应累计折旧科目编号", example = "2")
    private Long accumulatedDepreciationAccountId;
    @Schema(description = "对应折旧费用科目编号", example = "3")
    private Long depreciationExpenseAccountId;
    @Schema(description = "已折旧月数", example = "12")
    private Integer depreciatedMonths;
    @Schema(description = "累计折旧金额", example = "19000.00")
    private BigDecimal accumulatedDepreciation;
    @Schema(description = "账面净值", example = "81000.00")
    private BigDecimal netBookValue;
    @Schema(description = "状态（10=在用）", example = "10")
    private Integer status;
    @Schema(description = "状态名称", example = "在用")
    private String statusName;
    @Schema(description = "备注", example = "备注")
    private String remark;

}
