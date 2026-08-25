package cn.zhicloud.module.hr.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HR 假期类型新增/修改 Request VO")
@Data
public class HrLeaveTypeSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "假期类型名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "年假")
    @NotEmpty(message = "假期类型名称不能为空")
    private String name;

    @Schema(description = "编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "ANNUAL")
    @NotEmpty(message = "编码不能为空")
    private String code;

    @Schema(description = "是否带薪 1是 0否", example = "1")
    private Integer isPaid;

    @Schema(description = "是否扣薪 1是 0否", example = "0")
    private Integer deductSalary;

    @Schema(description = "备注", example = "随便")
    private String remark;

}