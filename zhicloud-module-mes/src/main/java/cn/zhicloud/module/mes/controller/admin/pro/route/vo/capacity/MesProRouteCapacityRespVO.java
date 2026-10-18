package cn.zhicloud.module.mes.controller.admin.pro.route.vo.capacity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 工艺路线产能 Response VO")
@Data
public class MesProRouteCapacityRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long routeId;

    @Schema(description = "产能（件/小时；0 = 未配置）", example = "100.00")
    private BigDecimal capacity;

    @Schema(description = "效率系数（>0）", example = "1.000")
    private BigDecimal efficiency;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
