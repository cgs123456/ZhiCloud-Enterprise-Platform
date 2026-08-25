package cn.zhicloud.module.qms.controller.admin.audit.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.qms.enums.audit.QmsAuditorRoleEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 审核组成员新增/修改 Request VO")
@Data
public class QmsAuditPlanAuditorSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "审核计划 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "审核计划 ID 不能为空")
    private Long planId;

    @Schema(description = "审核员 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "审核员 ID 不能为空")
    private Long auditorId;

    @Schema(description = "角色", example = "10")
    @InEnum(QmsAuditorRoleEnum.class)
    private Integer role;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
