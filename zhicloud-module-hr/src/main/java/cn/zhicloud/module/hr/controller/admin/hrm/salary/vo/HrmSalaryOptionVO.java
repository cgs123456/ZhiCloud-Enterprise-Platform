package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 薪资项 VO（新增/修改/响应共用，树形）")
@Data
public class HrmSalaryOptionVO {

    @Schema(description = "薪资项编号")
    private Long id;

    @Schema(description = "薪资项编码")
    private Integer code;

    @Schema(description = "父薪资项编码（0=根）")
    private Integer parentCode;

    @Schema(description = "薪资项名称")
    @NotEmpty(message = "薪资项名称不能为空")
    private String name;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否系统默认项（不可删）")
    private Boolean systemFlag;

    @Schema(description = "薪资项类型")
    private Integer type;

    @Schema(description = "是否计税")
    private Boolean taxEnabled;

    @Schema(description = "是否显示")
    private Boolean visible;

    @Schema(description = "是否参与计算")
    private Boolean calculateEnabled;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "标准薪资项目录编号")
    private Long templateId;

    @Schema(description = "子薪资项")
    private List<HrmSalaryOptionVO> children;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
