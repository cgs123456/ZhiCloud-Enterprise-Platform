package cn.zhicloud.module.hr.controller.admin.socialinsurance.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HR 社保分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrSocialInsurancePageReqVO extends PageParam {

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "年份", example = "2024")
    private Integer year;

    @Schema(description = "状态", example = "0")
    private Integer status;

}