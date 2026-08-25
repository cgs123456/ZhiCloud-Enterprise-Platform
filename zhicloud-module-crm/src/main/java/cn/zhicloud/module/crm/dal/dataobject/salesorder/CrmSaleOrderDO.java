package cn.zhicloud.module.crm.dal.dataobject.salesorder;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.crm.dal.dataobject.business.CrmBusinessDO;
import cn.zhicloud.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CRM 销售订单 DO
 *
 * @author dhb52
 */
@TableName(value = "crm_sale_order")
@KeySequence("crm_sale_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmSaleOrderDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 订单编号
     */
    private String no;
    /**
     * 合同编号
     *
     * 关联 {@link CrmContractDO#getId()}
     */
    private Long contractId;
    /**
     * 客户编号
     *
     * 关联 {@link CrmCustomerDO#getId()}
     */
    private Long customerId;
    /**
     * 商机编号
     *
     * 关联 {@link CrmBusinessDO#getId()}
     */
    private Long businessId;
    /**
     * 联系人编号
     *
     * 关联 {@link CrmContactDO#getId()}
     */
    private Long contactId;
    /**
     * 下单日期
     */
    private LocalDateTime orderDate;
    /**
     * 交货日期
     */
    private LocalDateTime deliveryDate;
    /**
     * 总金额，单位：元
     */
    private BigDecimal totalAmount;
    /**
     * 折扣金额，单位：元
     */
    private BigDecimal discountAmount;
    /**
     * 最终金额，单位：元
     */
    private BigDecimal finalAmount;
    /**
     * 付款状态
     *
     * 枚举 {@link cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderPaymentStatusEnum}
     */
    private Integer paymentStatus;
    /**
     * 发货状态
     *
     * 枚举 {@link cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderDeliveryStatusEnum}
     */
    private Integer deliveryStatus;
    /**
     * 订单状态
     *
     * 枚举 {@link cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderStatusEnum}
     */
    private Integer status;
    /**
     * 负责人的用户编号
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long ownerUserId;
    /**
     * 工作流编号
     *
     * 关联 ProcessInstance 的 id 属性
     */
    private String processInstanceId;
    /**
     * 备注
     */
    private String remark;

}
