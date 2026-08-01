package cn.iocoder.yudao.module.crm.service.servicecloud;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.contact.CrmContactDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.customer.CrmCustomerDO;
import cn.iocoder.yudao.module.crm.dal.dataobject.product.CrmProductDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * CRM 售后工单 DO
 *
 * @author dhb52
 */
@TableName(value = "crm_work_order")
@KeySequence("crm_work_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrmWorkOrderDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 工单编号
     */
    private String no;
    /**
     * 标题
     */
    private String title;
    /**
     * 客户编号
     *
     * 关联 {@link CrmCustomerDO#getId()}
     */
    private Long customerId;
    /**
     * 联系人编号
     *
     * 关联 {@link CrmContactDO#getId()}
     */
    private Long contactId;
    /**
     * 产品编号
     *
     * 关联 {@link CrmProductDO#getId()}
     */
    private Long productId;
    /**
     * 工单类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.crm.enums.servicecloud.CrmWorkOrderTypeEnum}
     */
    private Integer workOrderType;
    /**
     * 优先级
     *
     * 枚举 {@link cn.iocoder.yudao.module.crm.enums.servicecloud.CrmWorkOrderPriorityEnum}
     */
    private Integer priority;
    /**
     * 问题描述
     */
    private String description;
    /**
     * 状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.crm.enums.servicecloud.CrmWorkOrderStatusEnum}
     */
    private Integer status;
    /**
     * 处理人
     *
     * 关联 AdminUserDO 的 id 字段
     */
    private Long assigneeUserId;
    /**
     * 解决方案
     */
    private String resolution;
    /**
     * 响应时间
     */
    private LocalDateTime respondTime;
    /**
     * 解决时间
     */
    private LocalDateTime resolveTime;
    /**
     * SLA 截止时间
     */
    private LocalDateTime slaDeadline;
    /**
     * 备注
     */
    private String remark;

}
