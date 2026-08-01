package cn.iocoder.yudao.module.ai.controller.admin.nl2sql.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - AI NL2SQL 自然语言查询 Response VO")
@Data
@Accessors(chain = true)
public class Nl2SqlQueryRespVO {

    @Schema(description = "自然语言问题", example = "查一下本月销售额前10的产品")
    private String naturalLanguage;

    @Schema(description = "生成的 SQL", example = "SELECT product_id, SUM(total_price) FROM erp_sale_order_item ... LIMIT 10")
    private String sql;

    @Schema(description = "执行状态（0-成功 1-校验失败 2-执行失败）", example = "0")
    private Integer status;

    @Schema(description = "结果列名列表", example = "[\"product_id\",\"total_amount\"]")
    private List<String> columns;

    @Schema(description = "结果行列表，每行为列名 → 列值", example = "[{\"product_id\":1,\"total_amount\":1000}]")
    private List<Map<String, Object>> rows;

    @Schema(description = "结果行数", example = "10")
    private Integer rowCount;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "耗时（毫秒）", example = "256")
    private Long costMs;

}
