package cn.zhicloud.module.qms.dal.dataobject.qualitycost;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.qms.enums.qms.QmsQualityCostTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * QMS 质量成本 DO
 *
 * <p>基于 PAIF 模型划分四类质量成本：PREVENTION 预防 / APPRAISAL 鉴定 /
 * INTERNAL_FAILURE 内部故障 / EXTERNAL_FAILURE 外部故障。
 *
 * @author zhicloud
 */
@TableName("qms_quality_cost")
@KeySequence("qms_quality_cost_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsQualityCostDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 成本类型
     *
     * 枚举 {@link QmsQualityCostTypeEnum}：PREVENTION / APPRAISAL / INTERNAL_FAILURE / EXTERNAL_FAILURE
     */
    private String costType;
    /**
     * 成本类别（如：培训费/检测设备费/返工费/退货处理费）
     */
    private String costCategory;
    /**
     * 成本项目
     */
    private String costItem;
    /**
     * 金额
     */
    private BigDecimal amount;
    /**
     * 年度
     */
    private Integer periodYear;
    /**
     * 月份（1-12）
     */
    private Integer periodMonth;
    /**
     * 关联业务 ID（如 8D 报告 ID / NCR ID / CAPA ID）
     */
    private Long relatedId;
    /**
     * 关联业务类型（EIGHT_D / NCR / CAPA）
     */
    private String relatedType;
    /**
     * 备注
     */
    private String remark;

}