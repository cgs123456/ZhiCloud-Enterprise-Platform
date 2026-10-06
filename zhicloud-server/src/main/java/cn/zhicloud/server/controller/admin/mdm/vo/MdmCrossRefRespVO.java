package cn.zhicloud.server.controller.admin.mdm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台 - 主数据跨域对照 Response VO（R-014）
 *
 * @author 智云集成治理
 */
@Schema(description = "管理后台 - 主数据跨域对照 Response VO")
@Data
public class MdmCrossRefRespVO {

    @Schema(description = "对照编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "源系统", requiredMode = Schema.RequiredMode.REQUIRED, example = "ERP")
    private String sourceSystem;

    @Schema(description = "源实体类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "PRODUCT")
    private String sourceEntity;

    @Schema(description = "源编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private String sourceId;

    @Schema(description = "目标系统", requiredMode = Schema.RequiredMode.REQUIRED, example = "WMS")
    private String targetSystem;

    @Schema(description = "目标实体类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "SKU")
    private String targetEntity;

    @Schema(description = "目标编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "9001")
    private String targetId;

    @Schema(description = "状态：VALID 生效 / INVALID 已失效", requiredMode = Schema.RequiredMode.REQUIRED, example = "VALID")
    private String status;

    @Schema(description = "备注", example = "迁移批次 2026-10")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
