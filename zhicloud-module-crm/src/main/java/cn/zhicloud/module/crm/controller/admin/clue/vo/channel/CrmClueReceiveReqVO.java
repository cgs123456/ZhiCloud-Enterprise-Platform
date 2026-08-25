package cn.zhicloud.module.crm.controller.admin.clue.vo.channel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Schema(description = "外部系统 - CRM 线索接收 Request VO")
@Data
public class CrmClueReceiveReqVO {

    @Schema(description = "渠道编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "OFFICIAL")
    @NotEmpty(message = "渠道编码不能为空")
    private String channelCode;

    @Schema(description = "来源名称", example = "百度推广")
    private String sourceName;

    @Schema(description = "联系人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "联系人姓名不能为空")
    private String contactName;

    @Schema(description = "联系人电话", requiredMode = Schema.RequiredMode.REQUIRED, example = "13800138000")
    @NotEmpty(message = "联系人电话不能为空")
    private String contactPhone;

    @Schema(description = "公司名称", example = "腾讯科技")
    private String companyName;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
