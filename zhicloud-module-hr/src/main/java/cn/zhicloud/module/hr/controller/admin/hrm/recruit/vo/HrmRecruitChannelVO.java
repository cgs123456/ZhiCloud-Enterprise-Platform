package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 招聘渠道 VO")
@Data
public class HrmRecruitChannelVO {

    @Schema(description = "招聘渠道编号")
    private Long id;

    @Schema(description = "渠道名称")
    private String name;

    @Schema(description = "是否系统内置")
    private Boolean systemFlag;

    @Schema(description = "状态：0开启 1关闭")
    private Integer status;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
