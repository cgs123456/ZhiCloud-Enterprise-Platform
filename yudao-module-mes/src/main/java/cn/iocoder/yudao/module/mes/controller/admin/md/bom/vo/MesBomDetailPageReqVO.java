package cn.iocoder.yudao.module.mes.controller.admin.md.bom.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - MES BOM 明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesBomDetailPageReqVO extends PageParam {

    @Schema(description = "BOM 主数据编号", example = "1")
    private Long bomId;

    @Schema(description = "子件产品编号", example = "2")
    private Long productId;

}