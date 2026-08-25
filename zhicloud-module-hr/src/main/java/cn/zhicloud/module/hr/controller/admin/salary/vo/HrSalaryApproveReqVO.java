package cn.zhicloud.module.hr.controller.admin.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HR 薪资审核 Request VO")
@Data
public class HrSalaryApproveReqVO {

    @Schema(description = "薪资记录编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "薪资记录编号不能为空")
    private Long id;

}