package cn.zhicloud.module.erp.dal.dataobject.sale.credit;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.sale.ErpCustomerDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 客户信用额度 DO
 *
 * @author 智云
 */
@TableName("erp_credit_limit")
@KeySequence("erp_credit_limit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCreditLimitDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 客户编号
     *
     * 关联 {@link ErpCustomerDO#getId()}
     */
    private Long customerId;
    /**
     * 信用额度
     */
    private BigDecimal creditLimit;
    /**
     * 已用额度
     */
    private BigDecimal usedAmount;
    /**
     * 可用额度
     */
    private BigDecimal availableAmount;
    /**
     * 逾期金额
     */
    private BigDecimal overdueAmount;
    /**
     * 预警比例（默认 80，即 80%）
     */
    private BigDecimal warningRatio;
    /**
     * 状态
     *
     * 10 正常 / 20 预警 / 30 冻结
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
