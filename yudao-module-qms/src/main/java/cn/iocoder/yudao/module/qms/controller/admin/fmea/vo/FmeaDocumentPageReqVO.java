package cn.iocoder.yudao.module.qms.controller.admin.fmea.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS FMEA 文档分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FmeaDocumentPageReqVO extends PageParam {

    @Schema(description = "FMEA 单号", example = "FMEA20240101001")
    private String fmeaNo;

    @Schema(description = "FMEA 类型", example = "10")
    private Integer fmeaType;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "产品 ID", example = "1024")
    private Long productId;

}
