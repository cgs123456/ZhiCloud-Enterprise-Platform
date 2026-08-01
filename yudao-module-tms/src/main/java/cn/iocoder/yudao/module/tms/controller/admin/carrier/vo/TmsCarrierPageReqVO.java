package cn.iocoder.yudao.module.tms.controller.admin.carrier.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - TMS 承运商分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TmsCarrierPageReqVO extends PageParam {

    @Schema(description = "承运商名称", example = "顺丰")
    private String name;

    @Schema(description = "承运商编码", example = "SF")
    private String code;

    @Schema(description = "服务类型", example = "10")
    private Integer serviceType;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
