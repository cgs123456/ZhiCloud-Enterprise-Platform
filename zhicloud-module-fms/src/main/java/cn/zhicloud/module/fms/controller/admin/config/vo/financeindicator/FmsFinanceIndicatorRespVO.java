package cn.zhicloud.module.fms.controller.admin.config.vo.financeindicator;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - FMS 财务指标 Response VO")
@Data
public class FmsFinanceIndicatorRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号")
    private Long accountSetId;

    @Schema(description = "指标名称")
    private String name;

    @Schema(description = "指标编码")
    private String code;

    @Schema(description = "取数报表类型")
    private Integer type;

    @Schema(description = "指标公式")
    private String formula;

    @Schema(description = "展示顺序")
    private Integer sort;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime createTime;

}
