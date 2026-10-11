package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 招聘渠道状态更新 Request VO")
@Data
public class HrmRecruitChannelUpdateStatusReqVO {

    @Schema(description = "招聘渠道编号")
    @NotNull(message = "渠道编号不能为空")
    private Long id;

    @Schema(description = "状态：0开启 1关闭")
    @NotNull(message = "状态不能为空")
    private Integer status;

}
