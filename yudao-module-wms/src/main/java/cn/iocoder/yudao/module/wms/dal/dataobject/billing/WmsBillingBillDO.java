package cn.iocoder.yudao.module.wms.dal.dataobject.billing;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WMS 3PL 计费账单 DO
 *
 * @author 芋道源码
 */
@TableName("wms_billing_bill")
@KeySequence("wms_billing_bill_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsBillingBillDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 账单号
     */
    private String billNo;
    /**
     * 货主编号
     *
     * 关联 {@link WmsMerchantDO#getId()}
     */
    private Long ownerId;
    /**
     * 计费周期开始时间
     */
    private LocalDateTime billingPeriodStart;
    /**
     * 计费周期结束时间
     */
    private LocalDateTime billingPeriodEnd;
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    /**
     * 账单状态
     *
     * 10 草稿 / 20 已确认 / 30 已结算 / 40 已付款
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
