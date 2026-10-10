package cn.zhicloud.module.fms.controller.admin.config.vo.vouchertemplate;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - FMS 凭证模板分类 Response VO")
@Data
public class FmsVoucherTemplateCategoryRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "账套编号")
    private Long accountSetId;

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private java.time.LocalDateTime createTime;

}
