package cn.iocoder.yudao.module.hr.controller.admin.position.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.hr.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 职位 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HrPositionRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "职位编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "P001")
    @ExcelProperty("职位编码")
    private String code;

    @Schema(description = "职位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "Java 工程师")
    @ExcelProperty("职位名称")
    private String name;

    @Schema(description = "所属部门 ID", example = "2048")
    @ExcelProperty("所属部门 ID")
    private Long deptId;

    @Schema(description = "职级", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "职级", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.HR_POSITION_LEVEL)
    private Integer level;

    @Schema(description = "基本工资", example = "10000.00")
    @ExcelProperty("基本工资")
    private BigDecimal baseSalary;

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