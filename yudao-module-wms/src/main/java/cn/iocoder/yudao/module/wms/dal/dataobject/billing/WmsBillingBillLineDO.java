package cn.iocoder.yudao.module.wms.dal.dataobject.billing;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * WMS 3PL 计费账单明细 DO
 *
 * @author 芋道源码
 */
@TableName("wms_billing_bill_line")
@KeySequence("wms_billing_bill_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsBillingBillLineDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 账单编号
     *
     * 关联 {@link WmsBillingBillDO#getId()}
     */
    private Long billId;
    /**
     * 计费合同条款编号
     *
     * 关联 {@link WmsBillingContractItemDO#getId()}
     */
    private Long contractItemId;
    /**
     * 费用类型
     *
     * 10 仓储费 / 20 操作费 / 30 装卸费 / 40 越库费 / 50 其他
     */
    private Integer feeType;
    /**
     * 计费方式
     *
     * 10 按天 / 20 按次 / 30 按件
     */
    private Integer feeMode;
    /**
     * 数量（天数/次数/件数）
     */
    private BigDecimal quantity;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 金额
     */
    private BigDecimal amount;
    /**
     * 关联单据号
     */
    private String referenceOrderNo;
    /**
     * 备注
     */
    private String remark;

}
