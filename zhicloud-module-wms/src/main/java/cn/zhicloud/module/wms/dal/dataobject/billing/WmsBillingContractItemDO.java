package cn.zhicloud.module.wms.dal.dataobject.billing;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * WMS 3PL 计费合同条款 DO
 *
 * @author 智云
 */
@TableName("wms_billing_contract_item")
@KeySequence("wms_billing_contract_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsBillingContractItemDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 计费合同编号
     *
     * 关联 {@link WmsBillingContractDO#getId()}
     */
    private Long contractId;
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
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 最低收费
     */
    private BigDecimal minCharge;
    /**
     * 备注
     */
    private String remark;

}
