package cn.iocoder.yudao.module.qms.dal.dataobject.eightd;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.EightDStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QMS 8D 报告 DO
 *
 * <p>8D（Eight Disciplines）问题解决法，覆盖 D1-D8 八个阶段，
 * 可关联 NCR（不合格品报告）或 CAPA（纠正预防措施）。
 *
 * @author yudao
 */
@TableName("qms_eight_d_report")
@KeySequence("qms_eight_d_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EightDReportDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 8D 报告编号
     */
    private String reportNo;
    /**
     * 标题
     */
    private String title;
    /**
     * 关联 NCR 编号 ID
     */
    private Long ncrId;
    /**
     * 关联 CAPA 编号 ID
     */
    private Long capaId;
    /**
     * 状态
     *
     * 枚举 {@link EightDStatusEnum}
     */
    private Integer status;
    /**
     * D1 团队成员
     */
    private String d1TeamMembers;
    /**
     * D2 问题描述
     */
    private String d2ProblemDescription;
    /**
     * D3 临时遏制措施
     */
    private String d3InterimAction;
    /**
     * D4 根本原因分析
     */
    private String d4RootCause;
    /**
     * D5 永久纠正措施
     */
    private String d5PermanentAction;
    /**
     * D6 实施并验证结果
     */
    private String d6ImplementationResult;
    /**
     * D7 预防再发生措施
     */
    private String d7PreventionAction;
    /**
     * D8 团队表彰
     */
    private String d8TeamRecognition;
    /**
     * 关闭时间
     */
    private LocalDateTime closeTime;
    /**
     * 备注
     */
    private String remark;

}