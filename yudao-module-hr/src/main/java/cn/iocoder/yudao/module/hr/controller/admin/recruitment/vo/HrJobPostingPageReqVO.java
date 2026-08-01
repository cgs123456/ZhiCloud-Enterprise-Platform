package cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HR 招聘职位分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrJobPostingPageReqVO extends PageParam {

    @Schema(description = "岗位 ID", example = "4096")
    private Long positionId;

    @Schema(description = "招聘标题", example = "Java")
    private String title;

    @Schema(description = "状态", example = "0")
    private Integer status;

}