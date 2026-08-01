package cn.iocoder.yudao.module.qms.controller.admin.fmea.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.qms.FmeaStatusEnum;
import cn.iocoder.yudao.module.qms.enums.qms.FmeaTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - QMS FMEA 文档新增/修改 Request VO")
@Data
public class FmeaDocumentSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "FMEA 单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "FMEA20240101001")
    @NotEmpty(message = "FMEA 单号不能为空")
    private String fmeaNo;

    @Schema(description = "FMEA 类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @InEnum(FmeaTypeEnum.class)
    private Integer fmeaType;

    @Schema(description = "产品 ID", example = "1024")
    private Long productId;

    @Schema(description = "工序 ID", example = "2048")
    private Long processId;

    @Schema(description = "版本", example = "v1.0")
    private String version;

    @Schema(description = "状态", example = "10")
    @InEnum(FmeaStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
