package cn.iocoder.yudao.module.qms.dal.dataobject.capa;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.CAPAPriorityEnum;
import cn.iocoder.yudao.module.qms.enums.qms.CAPASourceEnum;
import cn.iocoder.yudao.module.qms.enums.qms.CAPAStageEnum;
import cn.iocoder.yudao.module.qms.enums.qms.CAPAStatusEnum;
import cn.iocoder.yudao.module.qms.enums.qms.CAPAVerificationResultEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QMS CAPA 文档 DO
 *
 * @author 芋道源码
 */
@TableName("qms_capa_document")
@KeySequence("qms_capa_document_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CAPADocumentDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * CAPA 单号
     */
    private String capaNo;
    /**
     * 来源
     *
     * 枚举 {@link CAPASourceEnum}
     */
    private Integer source;
    /**
     * 优先级（P0-4）
     *
     * 枚举 {@link CAPAPriorityEnum}
     */
    private Integer priority;
    /**
     * 当前阶段（P0-4）
     *
     * 枚举 {@link CAPAStageEnum}，与 status 字段保持一致：
     * <ul>
     *   <li>CREATED / ROOT_CAUSE_ANALYSIS / CORRECTIVE_ACTION / PREVENTIVE_ACTION 对应 status=OPEN</li>
     *   <li>VERIFICATION 对应 status=IN_PROGRESS</li>
     *   <li>CLOSED 对应 status=CLOSED</li>
     * </ul>
     */
    private Integer stage;
    /**
     * 问题描述
     */
    private String problem;
    /**
     * 原因
     */
    private String cause;
    /**
     * 根本原因分析
     */
    private String rootCauseAnalysis;
    /**
     * 纠正措施
     */
    private String correctiveAction;
    /**
     * 预防措施
     */
    private String preventiveAction;
    /**
     * 责任人
     */
    private String responsiblePerson;
    /**
     * due_date 截止日期
     */
    private LocalDateTime dueDate;
    /**
     * 关闭日期
     */
    private LocalDateTime closeDate;
    /**
     * 状态
     *
     * 枚举 {@link CAPAStatusEnum}
     */
    private Integer status;
    /**
     * 有效性验证结果（P0-4）
     *
     * 枚举 {@link CAPAVerificationResultEnum}，仅在 stage=VERIFICATION 后填写
     */
    private Integer verificationResult;
    /**
     * 有效性验证意见（P0-4）
     *
     * 由验证人填写，记录验证过程中的观察、结论依据等
     */
    private String verificationComment;
    /**
     * 验证人（P0-4）
     */
    private String verifiedBy;
    /**
     * 验证时间（P0-4）
     */
    private LocalDateTime verifiedTime;
    /**
     * 备注
     */
    private String remark;

}
