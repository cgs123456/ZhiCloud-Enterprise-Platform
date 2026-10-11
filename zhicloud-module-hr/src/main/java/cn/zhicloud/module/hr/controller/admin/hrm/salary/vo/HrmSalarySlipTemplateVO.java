package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 工资条模板 VO（新增/修改/响应共用）")
@Data
public class HrmSalarySlipTemplateVO {

    @Schema(description = "工资条模板编号")
    private Long id;

    @Schema(description = "模板名称")
    @NotEmpty(message = "模板名称不能为空")
    private String name;

    @Schema(description = "是否隐藏空工资项")
    private Boolean hideEmpty;

    @Schema(description = "是否默认模板")
    private Boolean defaultStatus;

    @Schema(description = "模板工资项")
    private List<HrmSalarySlipTemplateOptionVO> options;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
