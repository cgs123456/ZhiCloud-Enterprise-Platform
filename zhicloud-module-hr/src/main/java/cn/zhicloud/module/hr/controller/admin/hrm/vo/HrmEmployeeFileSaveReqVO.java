package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Schema(description = "管理后台 - HRM 员工档案附件保存 Request VO")
@Data
public class HrmEmployeeFileSaveReqVO {

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "附件类型（11-25 分组）")
    @NotNull(message = "附件类型不能为空")
    private Integer type;

    @Schema(description = "附件地址数组")
    @NotEmpty(message = "附件地址不能为空")
    private List<String> fileUrls;

}
