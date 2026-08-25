package cn.zhicloud.module.qms.dal.dataobject.msa;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.qms.enums.qms.MsaStatusEnum;
import cn.zhicloud.module.qms.enums.qms.MsaStudyTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * QMS MSA 研究记录 DO
 *
 * @author 智云
 */
@TableName("qms_msa_study")
@KeySequence("qms_msa_study_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MsaStudyDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 研究编号
     */
    private String studyNo;
    /**
     * 研究类型
     *
     * 枚举 {@link MsaStudyTypeEnum}
     */
    private Integer studyType;
    /**
     * 特性名称
     */
    private String characteristicName;
    /**
     * 测量设备 ID
     */
    private Long equipmentId;
    /**
     * 评价人数量
     */
    private Integer appraiserCount;
    /**
     * 试验次数
     */
    private Integer trialCount;
    /**
     * 零件数量
     */
    private Integer partCount;
    /**
     * 状态
     *
     * 枚举 {@link MsaStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
