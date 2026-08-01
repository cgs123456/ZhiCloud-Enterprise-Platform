package cn.iocoder.yudao.module.qms.controller.admin.capa.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.qms.CAPAStageEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * CAPA 阶段流转请求 VO（P0-4）
 *
 * <p>仅用于显式触发状态机前进/后退 1 步。若需要在流转时同步更新阶段字段，
 * 直接通过 {@code update} 接口修改 stage 字段也可，但会绕过状态机校验。
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - CAPA 阶段流转 Request VO")
@Data
public class CAPAStageTransitionReqVO {

    @Schema(description = "CAPA 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "CAPA 编号不能为空")
    private Long id;

    @Schema(description = "目标阶段", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "目标阶段不能为空")
    @InEnum(CAPAStageEnum.class)
    private Integer targetStage;

    @Schema(description = "流转备注（可选）", example = "已完成根本原因分析")
    private String remark;

}
