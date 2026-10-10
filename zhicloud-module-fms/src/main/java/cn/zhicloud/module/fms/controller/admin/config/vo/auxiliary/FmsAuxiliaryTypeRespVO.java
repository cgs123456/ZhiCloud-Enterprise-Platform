package cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - FMS 辅助核算类别 Response VO")
@Data
public class FmsAuxiliaryTypeRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号")
    private Long accountSetId;

    @Schema(description = "类别名称")
    private String name;

    @Schema(description = "辅助核算类型")
    private Integer type;

    @Schema(description = "是否系统预置")
    private Boolean systemPreset;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime createTime;

}
