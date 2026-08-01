package cn.iocoder.yudao.module.hr.controller.admin.department.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.hr.enums.department.HrDepartmentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HR 部门新增/修改 Request VO")
@Data
public class HrDepartmentSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "父部门 ID", example = "0")
    private Long parentId;

    @Schema(description = "部门编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "D001")
    @NotEmpty(message = "部门编码不能为空")
    private String code;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "研发部")
    @NotEmpty(message = "部门名称不能为空")
    private String name;

    @Schema(description = "部门负责人（员工 ID）", example = "2048")
    private Long leaderId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "状态不能为空")
    @InEnum(HrDepartmentStatusEnum.class)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "1")
    private Integer sort;

}