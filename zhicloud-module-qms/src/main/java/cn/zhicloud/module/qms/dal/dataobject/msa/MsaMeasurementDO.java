package cn.zhicloud.module.qms.dal.dataobject.msa;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * QMS MSA 测量数据 DO
 *
 * @author 智云
 */
@TableName("qms_msa_measurement")
@KeySequence("qms_msa_measurement_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MsaMeasurementDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 研究 ID
     *
     * 关联 {@link MsaStudyDO#getId()}
     */
    private Long studyId;
    /**
     * 零件 ID
     */
    private Long partId;
    /**
     * 评价人 ID
     */
    private Long appraiserId;
    /**
     * 试验序号
     */
    private Integer trialNo;
    /**
     * 测量值
     */
    private BigDecimal measurementValue;
    /**
     * 备注
     */
    private String remark;

}
