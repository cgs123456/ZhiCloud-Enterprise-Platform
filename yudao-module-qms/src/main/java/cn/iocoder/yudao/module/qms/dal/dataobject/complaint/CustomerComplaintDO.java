package cn.iocoder.yudao.module.qms.dal.dataobject.complaint;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.ComplaintHandleTypeEnum;
import cn.iocoder.yudao.module.qms.enums.qms.ComplaintStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * QMS 客户投诉 DO
 *
 * <p>覆盖投诉登记、调查、处理措施，并支持关联 8D 报告。
 *
 * @author yudao
 */
@TableName("qms_customer_complaint")
@KeySequence("qms_customer_complaint_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerComplaintDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 投诉编号
     */
    private String complaintNo;
    /**
     * 客户 ID
     */
    private Long customerId;
    /**
     * 客户名称
     */
    private String customerName;
    /**
     * 产品 ID
     */
    private Long productId;
    /**
     * 产品名称
     */
    private String productName;
    /**
     * 投诉内容
     */
    private String complaintContent;
    /**
     * 投诉日期
     */
    private LocalDate complaintDate;
    /**
     * 调查根因
     */
    private String rootCause;
    /**
     * 影响范围
     */
    private String impactScope;
    /**
     * 处理方式
     *
     * 枚举 {@link ComplaintHandleTypeEnum}
     */
    private Integer handleType;
    /**
     * 处理措施描述
     */
    private String handleAction;
    /**
     * 关联 8D 报告 ID
     */
    private Long eightDId;
    /**
     * 状态
     *
     * 枚举 {@link ComplaintStatusEnum}
     */
    private Integer status;
    /**
     * 关闭时间
     */
    private LocalDateTime closeTime;
    /**
     * 备注
     */
    private String remark;

}