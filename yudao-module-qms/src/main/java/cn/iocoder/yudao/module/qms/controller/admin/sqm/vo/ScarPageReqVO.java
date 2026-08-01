package cn.iocoder.yudao.module.qms.controller.admin.sqm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS SCAR 分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ScarPageReqVO extends PageParam {

    @Schema(description = "SCAR 单号", example = "SCAR202401001")
    private String scarNo;

    @Schema(description = "供应商 ID", example = "2048")
    private Long supplierId;

    @Schema(description = "状态", example = "10")
    private Integer status;

}