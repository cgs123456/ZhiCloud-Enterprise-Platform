package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - HRM 招聘候选人分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmRecruitCandidatePageReqVO extends PageParam {

    @Schema(description = "候选人姓名，模糊匹配")
    private String name;

    @Schema(description = "手机号码，模糊匹配")
    private String mobile;

    @Schema(description = "应聘职位编号数组")
    private Long[] postIds;

    @Schema(description = "用人部门编号数组")
    private Long[] deptIds;

    @Schema(description = "招聘渠道编号")
    private Long channelId;

    @Schema(description = "招聘负责人员工编号")
    private Long ownerEmployeeId;

    @Schema(description = "学历")
    private Integer education;

    @Schema(description = "候选人状态数组")
    private Integer[] statuses;

}
