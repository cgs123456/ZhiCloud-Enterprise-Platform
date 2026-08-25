package cn.zhicloud.module.qms.controller.admin.capa.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.qms.enums.qms.CAPAPriorityEnum;
import cn.zhicloud.module.qms.enums.qms.CAPASourceEnum;
import cn.zhicloud.module.qms.enums.qms.CAPAStageEnum;
import cn.zhicloud.module.qms.enums.qms.CAPAStatusEnum;
import cn.zhicloud.module.qms.enums.qms.CAPAVerificationResultEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS CAPA 文档新增/修改 Request VO")
@Data
public class CAPADocumentSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "CAPA 单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "CAPA20240101001")
    @NotEmpty(message = "CAPA 单号不能为空")
    private String capaNo;

    @Schema(description = "来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "来源不能为空")
    @InEnum(CAPASourceEnum.class)
    private Integer source;

    @Schema(description = "优先级", example = "20")
    @InEnum(CAPAPriorityEnum.class)
    private Integer priority;

    @Schema(description = "当前阶段", example = "10")
    @InEnum(CAPAStageEnum.class)
    private Integer stage;

    @Schema(description = "问题描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "产品外观不合格")
    @NotEmpty(message = "问题描述不能为空")
    private String problem;

    @Schema(description = "原因", example = "操作不当")
    private String cause;

    @Schema(description = "根本原因分析", example = "培训不足")
    private String rootCauseAnalysis;

    @Schema(description = "纠正措施", example = "重新培训")
    private String correctiveAction;

    @Schema(description = "预防措施", example = "建立培训计划")
    private String preventiveAction;

    @Schema(description = "责任人", example = "芋头")
    private String responsiblePerson;

    @Schema(description = "截止日期")
    private LocalDateTime dueDate;

    @Schema(description = "关闭日期")
    private LocalDateTime closeDate;

    @Schema(description = "状态", example = "10")
    @InEnum(CAPAStatusEnum.class)
    private Integer status;

    @Schema(description = "有效性验证结果", example = "20")
    @InEnum(CAPAVerificationResultEnum.class)
    private Integer verificationResult;

    @Schema(description = "有效性验证意见", example = "措施有效，未再发生同类问题")
    private String verificationComment;

    @Schema(description = "验证人", example = "芋头")
    private String verifiedBy;

    @Schema(description = "验证时间")
    private LocalDateTime verifiedTime;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
