package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * HRM 标准参保类型 VO（前端 getInsuranceStandardTypeList 返回）
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - HRM 标准参保类型 VO")
@Data
public class InsuranceStandardTypeVO {

    @Schema(description = "参保方案编码")
    private String code;

    @Schema(description = "参保方案名称")
    private String name;

}
