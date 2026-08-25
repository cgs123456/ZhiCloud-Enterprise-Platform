package cn.zhicloud.module.system.controller.admin.logger.vo.operatelog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - 操作日志 Hash 链验证结果 Response VO")
@Data
public class OperateLogHashVerifyRespVO {

    @Schema(description = "是否通过验证（true 未被篡改；false 检测到篡改）", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private boolean valid;

    @Schema(description = "验证起始日志 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long startId;

    @Schema(description = "本次验证的日志总数", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Integer totalCount;

    @Schema(description = "检测到篡改的日志 ID 列表（链断点：从这些 ID 开始 hash 与重新计算结果不一致）",
            example = "[2048, 2049]")
    private List<Long> tamperedIds = new ArrayList<>();

    @Schema(description = "验证说明", example = "Hash 链完整性验证通过")
    private String message;

}
