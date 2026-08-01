package cn.iocoder.yudao.module.hr.controller.admin.position.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.hr.enums.position.HrPositionLevelEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HR 职位新增/修改 Request VO")
@Data
public class HrPositionSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "职位编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "P001")
    @NotEmpty(message = "职位编码不能为空")
    private String code;

    @Schema(description = "职位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "Java 工程师")
    @NotEmpty(message = "职位名称不能为空")
    private String name;

    @Schema(description = "所属部门 ID", example = "2048")
    private Long deptId;

    @Schema(description = "职级", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "职级不能为空")
    @InEnum(HrPositionLevelEnum.class)
    private Integer level;

    @Schema(description = "基本工资", example = "10000.00")
    private BigDecimal baseSalary;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "1")
    private Integer sort;

}