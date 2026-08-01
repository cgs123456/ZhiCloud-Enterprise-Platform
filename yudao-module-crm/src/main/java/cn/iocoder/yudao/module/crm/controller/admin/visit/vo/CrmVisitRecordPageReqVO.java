package cn.iocoder.yudao.module.crm.controller.admin.visit.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - CRM 拜访签到记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmVisitRecordPageReqVO extends PageParam {

    @Schema(description = "客户编号", example = "2")
    private Long customerId;

    @Schema(description = "联系人编号", example = "2")
    private Long contactId;

    @Schema(description = "拜访类型", example = "1")
    private Integer visitType;

}
