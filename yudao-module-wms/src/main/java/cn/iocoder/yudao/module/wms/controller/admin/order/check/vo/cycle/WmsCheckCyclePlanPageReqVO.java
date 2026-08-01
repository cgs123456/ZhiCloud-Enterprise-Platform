package cn.iocoder.yudao.module.wms.controller.admin.order.check.vo.cycle;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - WMS 循环盘点计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsCheckCyclePlanPageReqVO extends PageParam {

    @Schema(description = "仓库编号", example = "1024")
    private Long warehouseId;

    @Schema(description = "ABC 分类", example = "A")
    private String abcClassification;

    @Schema(description = "是否启用", example = "1")
    private Integer enabled;

}