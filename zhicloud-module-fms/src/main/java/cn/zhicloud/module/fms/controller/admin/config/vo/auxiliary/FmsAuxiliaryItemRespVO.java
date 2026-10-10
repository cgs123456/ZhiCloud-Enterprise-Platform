package cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - FMS 辅助核算项目 Response VO")
@Data
public class FmsAuxiliaryItemRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号")
    private Long accountSetId;

    @Schema(description = "辅助核算类别编号")
    private Long auxiliaryTypeId;

    @Schema(description = "项目编码")
    private String code;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "规格")
    private String specification;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime createTime;

}
