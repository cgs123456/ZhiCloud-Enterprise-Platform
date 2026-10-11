package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 调薪模板 VO（新增/修改/响应共用）")
@Data
public class HrmSalaryChangeTemplateVO {

    @Schema(description = "调薪模板编号")
    private Long id;

    @Schema(description = "模板名称")
    @NotEmpty(message = "模板名称不能为空")
    private String name;

    @Schema(description = "是否默认模板")
    private Boolean defaultStatus;

    @Schema(description = "调薪项配置")
    private List<HrmSalaryChangeOptionVO> options;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
