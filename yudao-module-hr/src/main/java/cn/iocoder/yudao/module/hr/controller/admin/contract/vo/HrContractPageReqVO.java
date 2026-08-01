package cn.iocoder.yudao.module.hr.controller.admin.contract.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HR 合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrContractPageReqVO extends PageParam {

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "合同编号", example = "HT202401001")
    private String contractNo;

    @Schema(description = "合同类型", example = "1")
    private Integer contractType;

    @Schema(description = "状态", example = "0")
    private Integer status;

}