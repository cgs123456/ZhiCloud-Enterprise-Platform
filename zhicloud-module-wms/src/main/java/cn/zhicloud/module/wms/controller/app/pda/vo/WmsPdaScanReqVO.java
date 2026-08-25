package cn.zhicloud.module.wms.controller.app.pda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * PDA 扫码请求 VO
 *
 * @author 智云
 */
@Schema(description = "用户 App - PDA 扫码 Request VO")
@Data
public class WmsPdaScanReqVO {

    /**
     * 扫码类型：库位 / 物料
     */
    public static final String SCAN_TYPE_LOCATION = "LOCATION";
    public static final String SCAN_TYPE_ITEM = "ITEM";

    @Schema(description = "扫码类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOCATION")
    @NotEmpty(message = "扫码类型不能为空")
    private String scanType;

    @Schema(description = "扫码内容（库位编码 / 物料条码）", requiredMode = Schema.RequiredMode.REQUIRED, example = "LOC001")
    @NotEmpty(message = "扫码内容不能为空")
    private String scanCode;

    @Schema(description = "仓库编号（扫码库位时可指定）", example = "1024")
    private Long warehouseId;

    @Schema(description = "当前页码", example = "1")
    @NotNull(message = "页码不能为空")
    private Integer pageNo = 1;

    @Schema(description = "每页条数", example = "20")
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize = 20;

}
