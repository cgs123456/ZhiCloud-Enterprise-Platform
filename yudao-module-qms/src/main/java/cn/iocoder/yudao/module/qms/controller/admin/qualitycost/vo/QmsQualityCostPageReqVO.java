package cn.iocoder.yudao.module.qms.controller.admin.qualitycost.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 质量成本分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsQualityCostPageReqVO extends PageParam {

    @Schema(description = "成本类型", example = "PREVENTION")
    private String costType;

    @Schema(description = "成本类别", example = "培训费")
    private String costCategory;

    @Schema(description = "成本项目", example = "六西格玛绿带培训")
    private String costItem;

    @Schema(description = "年度", example = "2024")
    private Integer periodYear;

    @Schema(description = "月份（1-12）", example = "6")
    private Integer periodMonth;

    @Schema(description = "关联业务类型（EIGHT_D/NCR/CAPA）", example = "NCR")
    private String relatedType;

    @Schema(description = "关联业务 ID", example = "1024")
    private Long relatedId;

}