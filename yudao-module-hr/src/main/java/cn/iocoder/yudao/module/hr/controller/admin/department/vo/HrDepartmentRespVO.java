package cn.iocoder.yudao.module.hr.controller.admin.department.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 部门 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HrDepartmentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "父部门 ID", example = "0")
    @ExcelProperty("父部门 ID")
    private Long parentId;

    @Schema(description = "部门编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "D001")
    @ExcelProperty("部门编码")
    private String code;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "研发部")
    @ExcelProperty("部门名称")
    private String name;

    @Schema(description = "部门负责人（员工 ID）", example = "2048")
    @ExcelProperty("部门负责人")
    private Long leaderId;

    @Schema(description = "状态（10 启用 20 禁用）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}