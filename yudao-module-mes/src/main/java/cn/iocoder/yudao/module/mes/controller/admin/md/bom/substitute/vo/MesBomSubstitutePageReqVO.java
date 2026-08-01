package cn.iocoder.yudao.module.mes.controller.admin.md.bom.substitute.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - MES BOM 替代料分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesBomSubstitutePageReqVO extends PageParam {

    @Schema(description = "BOM 主表 ID", example = "1")
    private Long bomId;

    @Schema(description = "BOM 明细 ID", example = "10")
    private Long bomDetailId;

    @Schema(description = "替代物料 ID", example = "20")
    private Long substituteItemId;

    @Schema(description = "状态", example = "0")
    private Integer status;

}