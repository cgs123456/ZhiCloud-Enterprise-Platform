package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 招聘职位类型 VO")
@Data
public class HrmRecruitPostTypeVO {

    @Schema(description = "职位类型编号")
    private Long id;

    @Schema(description = "类型名称")
    private String name;

    @Schema(description = "父类型编号")
    private Long parentId;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：0开启 1关闭")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
