package cn.zhicloud.module.mes.controller.admin.pro.route.vo.capacity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - MES 工艺路线产能新增/修改 Request VO")
@Data
public class MesProRouteCapacitySaveReqVO {

    @Schema(description = "工艺路线编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "工艺路线编号不能为空")
    private Long routeId;

    @Schema(description = "产能（件/小时；0 = 未配置）", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    @NotNull(message = "产能不能为空")
    @DecimalMin(value = "0", message = "产能不能小于 0")
    private BigDecimal capacity;

    @Schema(description = "效率系数（>0）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.000")
    @NotNull(message = "效率系数不能为空")
    @DecimalMin(value = "0.001", message = "效率系数必须大于 0")
    private BigDecimal efficiency;

    @Schema(description = "备注")
    private String remark;

}
