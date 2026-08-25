package cn.zhicloud.module.qms.controller.admin.ncr.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 不合格品报告分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class NcrDocumentPageReqVO extends PageParam {

    @Schema(description = "NCR 单号", example = "NCR20240101001")
    private String ncrNo;

    @Schema(description = "来源", example = "10")
    private Integer source;

    @Schema(description = "缺陷等级", example = "20")
    private Integer defectLevel;

    @Schema(description = "处置方式", example = "10")
    private Integer disposition;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "产品 ID", example = "1024")
    private Long productId;

    @Schema(description = "供应商 ID", example = "2048")
    private Long supplierId;

}
