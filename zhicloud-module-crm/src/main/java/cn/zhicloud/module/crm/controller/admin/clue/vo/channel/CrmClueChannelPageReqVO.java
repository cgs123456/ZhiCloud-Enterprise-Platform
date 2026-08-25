package cn.zhicloud.module.crm.controller.admin.clue.vo.channel;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CRM 线索渠道分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmClueChannelPageReqVO extends PageParam {

    @Schema(description = "渠道名称", example = "官网渠道")
    private String channelName;

    @Schema(description = "渠道类型", example = "10")
    private Integer channelType;

    @Schema(description = "渠道编码", example = "OFFICIAL")
    private String channelCode;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
