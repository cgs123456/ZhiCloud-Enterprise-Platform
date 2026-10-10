package cn.zhicloud.module.fms.controller.admin.config.vo.auxiliary;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - FMS 辅助核算项目 导入 Response VO")
@Data
@Builder
public class FmsAuxiliaryItemImportRespVO {

    @Schema(description = "总数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalCount;

    @Schema(description = "成功项目编码数组", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> successItemCodes;

    @Schema(description = "失败原因集合，key 为项目编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, String> failureReasons;

}
