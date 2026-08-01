package cn.iocoder.yudao.module.hr.controller.admin.employee.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - HR 员工分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrEmployeePageReqVO extends PageParam {

    @Schema(description = "工号", example = "EMP001")
    private String empNo;

    @Schema(description = "姓名", example = "张三")
    private String name;

    @Schema(description = "性别", example = "10")
    private Integer gender;

    @Schema(description = "部门 ID", example = "2048")
    private Long deptId;

    @Schema(description = "职位 ID", example = "4096")
    private Long positionId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "用工类型", example = "10")
    private Integer employmentType;

}