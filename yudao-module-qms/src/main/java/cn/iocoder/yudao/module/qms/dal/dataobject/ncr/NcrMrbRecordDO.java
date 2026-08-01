package cn.iocoder.yudao.module.qms.dal.dataobject.ncr;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.NcrMrbDecisionEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QMS MRB 物料评审委员会记录 DO
 *
 * @author 芋道源码
 */
@TableName("qms_ncr_mrb_record")
@KeySequence("qms_ncr_mrb_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NcrMrbRecordDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * NCR 报告 ID
     *
     * 关联 {@link NcrDocumentDO#getId()}
     */
    private Long ncrId;
    /**
     * 评审日期
     */
    private LocalDateTime mrbDate;
    /**
     * 评审成员
     */
    private String mrbMembers;
    /**
     * 决议
     *
     * 枚举 {@link NcrMrbDecisionEnum}
     */
    private Integer decision;
    /**
     * 附加条件
     */
    private String conditionTerms;
    /**
     * 备注
     */
    private String remark;

}
