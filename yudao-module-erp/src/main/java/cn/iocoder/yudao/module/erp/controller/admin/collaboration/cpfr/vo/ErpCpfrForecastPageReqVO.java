package cn.iocoder.yudao.module.erp.controller.admin.collaboration.cpfr.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP CPFR 预测分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpCpfrForecastPageReqVO extends PageParam {

    @Schema(description = "预测单号", example = "CPFR202401001")
    private String no;

    @Schema(description = "合作伙伴类型（10 供应商 / 20 客户）", example = "10")
    private Integer partnerType;

    @Schema(description = "合作伙伴编号", example = "2048")
    private Long partnerId;

    @Schema(description = "产品编号", example = "4096")
    private Long productId;

    @Schema(description = "产品名称", example = "键盘")
    private String productName;

    @Schema(description = "预测周期（yyyyMM）", example = "202401")
    private String forecastPeriod;

}
