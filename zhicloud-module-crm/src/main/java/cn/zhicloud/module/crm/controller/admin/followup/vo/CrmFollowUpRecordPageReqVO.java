package cn.zhicloud.module.crm.controller.admin.followup.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 跟进记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmFollowUpRecordPageReqVO extends PageParam {

    @Schema(description = "数据类型", example = "2")
    @NotNull(message = "数据类型不能为空")
    private Integer bizType;

    @Schema(description = "数据编号", example = "5564")
    @NotNull(message = "数据编号不能为空")
    private Long bizId;

}