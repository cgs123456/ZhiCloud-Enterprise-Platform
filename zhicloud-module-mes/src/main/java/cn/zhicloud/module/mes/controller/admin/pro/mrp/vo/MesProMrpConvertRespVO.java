package cn.zhicloud.module.mes.controller.admin.pro.mrp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - MES MRP 转采购/生产建议结果 Response VO")
@Data
public class MesProMrpConvertRespVO {

    @Schema(description = "转出工单总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer totalCount;

    @Schema(description = "采购型工单数（无 BOM 物料）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer purchaseCount;

    @Schema(description = "生产型工单数（有 BOM 物料）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer produceCount;

}
