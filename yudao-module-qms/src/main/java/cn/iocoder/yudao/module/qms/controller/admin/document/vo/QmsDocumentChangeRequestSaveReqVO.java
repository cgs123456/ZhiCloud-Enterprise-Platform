package cn.iocoder.yudao.module.qms.controller.admin.document.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.document.QmsDocChangeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 文件变更申请新增/修改 Request VO")
@Data
public class QmsDocumentChangeRequestSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "受控文档 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "受控文档 ID 不能为空")
    private Long documentId;

    @Schema(description = "变更类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "变更类型不能为空")
    @InEnum(QmsDocChangeTypeEnum.class)
    private Integer changeType;

    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "更新流程以适配新规")
    @NotEmpty(message = "变更原因不能为空")
    private String changeReason;

    @Schema(description = "变更内容", example = "第3.2章节流程调整")
    private String changeContent;

    @Schema(description = "申请人 ID", example = "1024")
    private Long applicantId;

    @Schema(description = "申请日期", example = "2024-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate applyDate;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
