package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 招聘渠道删除 Request VO（内置渠道需承接）")
@Data
public class HrmRecruitChannelDeleteReqVO {

    @Schema(description = "待删除招聘渠道编号")
    @NotNull(message = "渠道编号不能为空")
    private Long id;

    @Schema(description = "承接招聘渠道编号")
    private Long transferChannelId;

}
