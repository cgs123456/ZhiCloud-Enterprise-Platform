package cn.iocoder.yudao.module.qms.controller.admin.inspectionitem.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - QMS 检验项目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InspectionItemPageReqVO extends PageParam {

    @Schema(description = "检验项目编码", example = "IQC-001")
    private String code;

    @Schema(description = "检验项目名称", example = "外观")
    private String name;

    @Schema(description = "检验类型", example = "10")
    private Integer type;

    @Schema(description = "检验方法", example = "10")
    private Integer method;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
