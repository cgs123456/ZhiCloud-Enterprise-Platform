package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工档案附件 Response VO")
@Data
public class HrmEmployeeFileRespVO {

    @Schema(description = "附件编号")
    private Long id;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "附件类型")
    private Integer type;

    @Schema(description = "附件地址")
    private String url;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
