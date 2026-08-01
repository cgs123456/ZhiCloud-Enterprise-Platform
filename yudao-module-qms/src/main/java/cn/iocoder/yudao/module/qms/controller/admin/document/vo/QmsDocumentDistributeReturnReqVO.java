package cn.iocoder.yudao.module.qms.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - QMS 文档回收登记 Request VO")
@Data
public class QmsDocumentDistributeReturnReqVO {

    @Schema(description = "分发记录 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "分发记录 ID 不能为空")
    private Long id;

    @Schema(description = "回收份数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "回收份数不能为空")
    private Integer returnedQty;

    @Schema(description = "回收日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-01-01")
    @NotNull(message = "回收日期不能为空")
    private LocalDate returnedDate;

}
