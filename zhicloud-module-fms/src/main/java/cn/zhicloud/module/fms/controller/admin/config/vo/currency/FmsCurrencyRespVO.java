package cn.zhicloud.module.fms.controller.admin.config.vo.currency;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Schema(description = "管理后台 - FMS 币别 Response VO")
@Data
public class FmsCurrencyRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号")
    private Long accountSetId;

    @Schema(description = "币别编码")
    private String code;

    @Schema(description = "币别名称")
    private String name;

    @Schema(description = "汇率")
    private BigDecimal exchangeRate;

    @Schema(description = "是否本位币")
    private Boolean standard;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime createTime;

}
