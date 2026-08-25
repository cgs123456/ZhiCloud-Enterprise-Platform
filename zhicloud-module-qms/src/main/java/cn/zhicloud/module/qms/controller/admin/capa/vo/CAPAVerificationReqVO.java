package cn.zhicloud.module.qms.controller.admin.capa.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.qms.enums.qms.CAPAVerificationResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CAPA 有效性验证结果提交请求 VO（P0-4）
 *
 * <p>仅在 stage=VERIFICATION 时可调用。结果为 PASSED 后可关闭；
 * 结果为 FAILED 时自动回退至 CORRECTIVE_ACTION 阶段重新走流程。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - CAPA 有效性验证提交 Request VO")
@Data
public class CAPAVerificationReqVO {

    @Schema(description = "CAPA 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "CAPA 编号不能为空")
    private Long id;

    @Schema(description = "验证结果", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "验证结果不能为空")
    @InEnum(CAPAVerificationResultEnum.class)
    private Integer verificationResult;

    @Schema(description = "验证意见", requiredMode = Schema.RequiredMode.REQUIRED, example = "措施有效，未再发生同类问题")
    @NotEmpty(message = "验证意见不能为空")
    private String verificationComment;

    @Schema(description = "验证人", example = "芋头")
    private String verifiedBy;

}
