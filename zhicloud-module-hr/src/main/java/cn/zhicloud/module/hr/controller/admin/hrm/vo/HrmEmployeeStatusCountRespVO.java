package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工状态数量 Response VO")
@Data
public class HrmEmployeeStatusCountRespVO {

    @Schema(description = "状态页签：11在职 12全职 13待入职 14待离职 15已离职")
    private Integer status;

    @Schema(description = "数量")
    private Long count;

}
