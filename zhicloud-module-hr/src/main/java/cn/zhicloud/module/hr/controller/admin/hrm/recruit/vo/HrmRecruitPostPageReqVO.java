package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - HRM 招聘职位分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmRecruitPostPageReqVO extends PageParam {

    @Schema(description = "职位名称，模糊匹配")
    private String postName;

    @Schema(description = "用人部门编号数组")
    private Long[] deptIds;

    @Schema(description = "职位类型编号")
    private Long postTypeId;

    @Schema(description = "职位状态")
    private Integer status;

    @Schema(description = "工作性质")
    private Integer jobNature;

    @Schema(description = "紧急程度")
    private Integer emergencyLevel;

    @Schema(description = "招聘负责人员工编号")
    private Long ownerEmployeeId;

}
