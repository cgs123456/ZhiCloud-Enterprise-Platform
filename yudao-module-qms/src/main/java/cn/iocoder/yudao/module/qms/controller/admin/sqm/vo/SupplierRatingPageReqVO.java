package cn.iocoder.yudao.module.qms.controller.admin.sqm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 供应商评级分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SupplierRatingPageReqVO extends PageParam {

    @Schema(description = "评级编号", example = "SR2024Q1001")
    private String ratingNo;

    @Schema(description = "供应商 ID", example = "2048")
    private Long supplierId;

    @Schema(description = "评级周期", example = "2024-Q1")
    private String ratingPeriod;

    @Schema(description = "供应商等级", example = "A")
    private String grade;

}