package cn.zhicloud.module.erp.controller.admin.finance.vo.glaccount;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 会计科目分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpGlAccountPageReqVO extends PageParam {

    @Schema(description = "科目编码", example = "1001")
    private String code;

    @Schema(description = "科目名称", example = "现金")
    private String name;

    @Schema(description = "科目类型", example = "10")
    private Integer type;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "是否末级科目", example = "true")
    private Boolean isLeaf;

}
