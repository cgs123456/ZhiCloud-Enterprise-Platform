package cn.zhicloud.module.qms.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 文档分发记录新增/修改 Request VO")
@Data
public class QmsDocumentDistributeSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "受控文档 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "受控文档 ID 不能为空")
    private Long documentId;

    @Schema(description = "分发对象", requiredMode = Schema.RequiredMode.REQUIRED, example = "质量部")
    @NotEmpty(message = "分发对象不能为空")
    private String distributeTo;

    @Schema(description = "分发份数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @NotNull(message = "分发份数不能为空")
    private Integer distributeQty;

    @Schema(description = "分发日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @NotNull(message = "分发日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate distributeDate;

    @Schema(description = "签收人", example = "张三")
    private String receivedBy;

    @Schema(description = "签收日期", example = "2024-01-02")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate receivedDate;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
