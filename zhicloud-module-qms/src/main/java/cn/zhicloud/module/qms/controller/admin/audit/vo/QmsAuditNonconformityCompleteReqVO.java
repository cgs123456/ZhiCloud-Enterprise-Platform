package cn.zhicloud.module.qms.controller.admin.audit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理后台 - QMS 审核不符合项「完成整改」 Request VO
 *
 * <p>台账 §12.218：整改措施提交后（RECTIFYING 20）需要有一个「完成整改」动作把
 * 不符合项推进到 RECTIFIED(30)，verify 才有机会被调用——此前五态状态机缺这一环，
 * 流程在 20 处死锁，不符合项永远无法验证与关闭。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - QMS 审核不符合项完成整改 Request VO")
@Data
public class QmsAuditNonconformityCompleteReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "不符合项 ID 不能为空")
    private Long id;

    @Schema(description = "整改完成说明（可选，留空则保留原整改措施）", example = "已完成操作人员重新培训与考核")
    private String completionNote;

}
