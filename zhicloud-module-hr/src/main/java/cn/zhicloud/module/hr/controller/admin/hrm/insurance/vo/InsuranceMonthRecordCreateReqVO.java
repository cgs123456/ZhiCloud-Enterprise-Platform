package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 月度社保表创建 Request VO")
@Data
public class InsuranceMonthRecordCreateReqVO {

    @Schema(description = "年份")
    @NotNull(message = "年份不能为空")
    private Integer year;

    @Schema(description = "月份")
    @NotNull(message = "月份不能为空")
    private Integer month;

}
