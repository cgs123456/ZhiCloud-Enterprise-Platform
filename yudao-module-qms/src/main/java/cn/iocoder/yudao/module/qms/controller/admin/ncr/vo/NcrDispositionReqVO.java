package cn.iocoder.yudao.module.qms.controller.admin.ncr.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.qms.NcrMrbDecisionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * NCR 处置决议请求 VO
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - NCR 处置决议 Request VO")
@Data
public class NcrDispositionReqVO {

    @Schema(description = "NCR 编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "NCR 编号不能为空")
    private Long ncrId;

    @Schema(description = "MRB 评审日期", example = "2024-01-01T10:00:00")
    private LocalDateTime mrbDate;

    @Schema(description = "评审成员", example = "张三,李四,王五")
    private String mrbMembers;

    @Schema(description = "决议", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "决议不能为空")
    @InEnum(NcrMrbDecisionEnum.class)
    private Integer decision;

    @Schema(description = "附加条件", example = "返工后需全检")
    private String conditionTerms;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
