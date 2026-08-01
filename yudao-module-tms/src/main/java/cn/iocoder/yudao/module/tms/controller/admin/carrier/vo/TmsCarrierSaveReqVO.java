package cn.iocoder.yudao.module.tms.controller.admin.carrier.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "管理后台 - TMS 承运商新增/修改 Request VO")
@Data
public class TmsCarrierSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "承运商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "顺丰")
    @NotEmpty(message = "承运商名称不能为空")
    private String name;

    @Schema(description = "承运商编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "SF")
    @NotEmpty(message = "承运商编码不能为空")
    private String code;

    @Schema(description = "服务类型（10 快递 / 20 零担 / 30 整车 / 40 空运 / 50 海运）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer serviceType;

    @Schema(description = "联系人", example = "张三")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "资质编号", example = "QT-001")
    private String qualificationNo;

    @Schema(description = "评分（1-5）", example = "5")
    private Integer rating;

    @Schema(description = "状态（10 启用 / 20 停用）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
