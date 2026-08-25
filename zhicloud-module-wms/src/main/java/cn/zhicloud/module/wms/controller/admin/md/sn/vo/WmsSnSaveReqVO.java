package cn.zhicloud.module.wms.controller.admin.md.sn.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - WMS 序列号创建/更新 Request VO")
@Data
public class WmsSnSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "序列号", example = "SN20260730001")
    @Size(max = 64, message = "序列号长度不能超过 64 个字符")
    private String sn;

    @Schema(description = "商品编号", example = "1")
    private Long productId;

    @Schema(description = "备注", example = "备注")
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;

}