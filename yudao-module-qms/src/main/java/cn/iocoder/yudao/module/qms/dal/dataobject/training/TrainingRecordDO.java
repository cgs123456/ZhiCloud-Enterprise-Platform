package cn.iocoder.yudao.module.qms.dal.dataobject.training;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * QMS 培训记录 DO
 *
 * <p>记录参训人员、成绩、证书信息。
 *
 * @author yudao
 */
@TableName("qms_training_record")
@KeySequence("qms_training_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingRecordDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 记录编号
     */
    private String recordNo;
    /**
     * 培训计划 ID
     */
    private Long planId;
    /**
     * 参训人员 ID
     */
    private Long traineeId;
    /**
     * 参训人员姓名
     */
    private String traineeName;
    /**
     * 成绩
     */
    private BigDecimal score;
    /**
     * 是否通过（0 否 1 是）
     */
    private Integer passed;
    /**
     * 证书编号
     */
    private String certificateNo;
    /**
     * 证书到期日
     */
    private LocalDate certificateExpireDate;
    /**
     * 状态（10 已登记 20 已完成）
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}