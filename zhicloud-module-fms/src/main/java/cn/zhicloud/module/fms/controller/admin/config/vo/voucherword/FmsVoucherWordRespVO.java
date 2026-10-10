package cn.zhicloud.module.fms.controller.admin.config.vo.voucherword;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - FMS 凭证字 Response VO")
@Data
public class FmsVoucherWordRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号")
    private Long accountSetId;

    @Schema(description = "凭证字")
    private String name;

    @Schema(description = "打印标题")
    private String printTitle;

    @Schema(description = "是否默认凭证字")
    private Boolean defaultStatus;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime createTime;

}
