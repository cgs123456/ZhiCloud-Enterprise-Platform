package cn.iocoder.yudao.module.crm.controller.admin.clue.vo.channel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 线索渠道创建/更新 Request VO")
@Data
public class CrmClueChannelSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "渠道名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "官网渠道")
    @NotEmpty(message = "渠道名称不能为空")
    private String channelName;

    @Schema(description = "渠道类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "渠道类型不能为空")
    private Integer channelType;

    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "OFFICIAL")
    @NotEmpty(message = "渠道编码不能为空")
    private String channelCode;

    @Schema(description = "API 地址", example = "https://www.iocoder.cn/api/clue")
    private String apiUrl;

    @Schema(description = "API 密钥", example = "123456")
    private String apiKey;

    @Schema(description = "自动分配人", example = "1024")
    private Long autoAssignUserId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
