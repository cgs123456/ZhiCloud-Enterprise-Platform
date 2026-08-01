package cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HR 简历分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrResumePageReqVO extends PageParam {

    @Schema(description = "招聘职位 ID", example = "2048")
    private Long jobPostingId;

    @Schema(description = "候选人姓名", example = "张三")
    private String candidateName;

    @Schema(description = "状态", example = "0")
    private Integer status;

}