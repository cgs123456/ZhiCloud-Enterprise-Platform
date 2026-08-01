package cn.iocoder.yudao.module.qms.controller.admin.ncr.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.qms.NcrDefectLevelEnum;
import cn.iocoder.yudao.module.qms.enums.qms.NcrDispositionEnum;
import cn.iocoder.yudao.module.qms.enums.qms.NcrSourceEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - QMS 不合格品报告新增/修改 Request VO")
@Data
public class NcrDocumentSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "NCR 单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "NCR20240101001")
    @NotEmpty(message = "NCR 单号不能为空")
    private String ncrNo;

    @Schema(description = "来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "来源不能为空")
    @InEnum(NcrSourceEnum.class)
    private Integer source;

    @Schema(description = "检验单 ID", example = "2048")
    private Long inspectionOrderId;

    @Schema(description = "产品 ID", example = "1024")
    private Long productId;

    @Schema(description = "供应商 ID", example = "2048")
    private Long supplierId;

    @Schema(description = "工单 ID", example = "3072")
    private Long workOrderId;

    @Schema(description = "缺陷描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "外观划痕")
    @NotEmpty(message = "缺陷描述不能为空")
    private String defectDescription;

    @Schema(description = "缺陷等级", example = "20")
    @InEnum(NcrDefectLevelEnum.class)
    private Integer defectLevel;

    @Schema(description = "不合格数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "不合格数量不能为空")
    private BigDecimal quantity;

    @Schema(description = "处置方式", example = "10")
    @InEnum(NcrDispositionEnum.class)
    private Integer disposition;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
