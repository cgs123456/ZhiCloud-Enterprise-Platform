package cn.zhicloud.module.qms.controller.admin.sqm.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 供应商审核分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SupplierAuditPageReqVO extends PageParam {

    @Schema(description = "审核编号", example = "SA202401001")
    private String auditNo;

    @Schema(description = "审核名称", example = "XX 供应商审核")
    private String auditName;

    @Schema(description = "供应商 ID", example = "2048")
    private Long supplierId;

    @Schema(description = "状态", example = "10")
    private Integer status;

}