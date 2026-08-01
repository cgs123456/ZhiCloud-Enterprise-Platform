package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.fixedasset;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 固定资产分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpFixedAssetPageReqVO extends PageParam {

    @Schema(description = "资产编码", example = "FA-001")
    private String code;

    @Schema(description = "资产名称", example = "服务器")
    private String name;

    @Schema(description = "资产类别", example = "办公设备")
    private String category;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "部门编号", example = "1")
    private Long departmentId;

}
