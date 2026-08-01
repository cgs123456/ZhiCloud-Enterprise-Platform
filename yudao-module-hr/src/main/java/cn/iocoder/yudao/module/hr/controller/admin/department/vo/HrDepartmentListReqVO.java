package cn.iocoder.yudao.module.hr.controller.admin.department.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HR 部门列表 Request VO")
@Data
public class HrDepartmentListReqVO {

    @Schema(description = "部门编码", example = "D001")
    private String code;

    @Schema(description = "部门名称", example = "研发")
    private String name;

    @Schema(description = "状态", example = "10")
    private Integer status;

}