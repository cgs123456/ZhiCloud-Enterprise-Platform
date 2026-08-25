package cn.zhicloud.module.qms.controller.admin.eightd.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 8D 报告分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EightDReportPageReqVO extends PageParam {

    @Schema(description = "8D 报告编号", example = "8D20240101001")
    private String reportNo;

    @Schema(description = "标题", example = "XX 产品 8D 报告")
    private String title;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "关联 NCR 编号 ID", example = "2048")
    private Long ncrId;

    @Schema(description = "关联 CAPA 编号 ID", example = "3072")
    private Long capaId;

}