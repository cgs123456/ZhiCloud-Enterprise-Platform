package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Schema(description = "管理后台 - HRM 招聘渠道新增/修改 Request VO")
@Data
public class HrmRecruitChannelSaveReqVO {

    @Schema(description = "招聘渠道编号")
    private Long id;

    @Schema(description = "渠道名称")
    @NotEmpty(message = "渠道名称不能为空")
    private String name;

    @Schema(description = "状态：0开启 1关闭")
    private Integer status;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

}
