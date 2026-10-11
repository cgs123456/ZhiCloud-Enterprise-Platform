package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - HRM 员工字段配置 Response VO")
@Data
public class HrmEmployeeFieldConfigRespVO {

    @Schema(description = "字段名称")
    private String name;

    @Schema(description = "字段标题")
    private String title;

    @Schema(description = "字段分组名称")
    private String groupName;

    @Schema(description = "是否显示")
    private Boolean visible;

    @Schema(description = "是否允许员工编辑")
    private Boolean editable;

    @Schema(description = "是否锁定显示")
    private Boolean visibleLocked;

    @Schema(description = "是否锁定编辑")
    private Boolean editableLocked;

}
