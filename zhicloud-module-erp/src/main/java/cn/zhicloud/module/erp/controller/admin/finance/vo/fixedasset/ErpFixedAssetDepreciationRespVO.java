package cn.zhicloud.module.erp.controller.admin.finance.vo.fixedasset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 固定资产折旧记录 Response VO")
@Data
public class ErpFixedAssetDepreciationRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;
    @Schema(description = "固定资产编号", example = "1")
    private Long fixedAssetId;
    @Schema(description = "资产编码", example = "FA-001")
    private String assetCode;
    @Schema(description = "资产名称", example = "服务器")
    private String assetName;
    @Schema(description = "会计期间编号", example = "1")
    private Long periodId;
    @Schema(description = "期间编码", example = "202607")
    private String periodCode;
    @Schema(description = "折旧日期")
    private LocalDate depreciationDate;
    @Schema(description = "本月折旧额", example = "1583.33")
    private BigDecimal depreciationAmount;
    @Schema(description = "累计折旧额", example = "19000.00")
    private BigDecimal accumulatedDepreciation;
    @Schema(description = "账面净值", example = "81000.00")
    private BigDecimal netBookValue;
    @Schema(description = "已折旧月数", example = "12")
    private Integer depreciatedMonths;
    @Schema(description = "折旧方法（10=直线法）", example = "10")
    private Integer depreciationMethod;
    @Schema(description = "状态（10=待审核；20=已审核）", example = "10")
    private Integer status;
    @Schema(description = "状态名称", example = "待审核")
    private String statusName;
    @Schema(description = "生成的凭证编号", example = "1")
    private Long voucherId;
    @Schema(description = "凭证编号字符串", example = "FA-DEP-202607-1024")
    private String voucherNo;
    @Schema(description = "备注", example = "备注")
    private String remark;

}
