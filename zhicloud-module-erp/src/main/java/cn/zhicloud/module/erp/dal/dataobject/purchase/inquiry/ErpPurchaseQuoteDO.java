package cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 采购报价单 DO
 *
 * @author 智云
 */
@TableName(value = "erp_purchase_quote")
@KeySequence("erp_purchase_quote_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseQuoteDO extends BaseDO {

    /**
     * 状态 - 草稿
     */
    public static final Integer STATUS_DRAFT = 10;
    /**
     * 状态 - 已报价
     */
    public static final Integer STATUS_QUOTED = 20;
    /**
     * 状态 - 已采纳
     */
    public static final Integer STATUS_ADOPTED = 30;
    /**
     * 状态 - 已拒绝
     */
    public static final Integer STATUS_REJECTED = 40;

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 报价单号
     */
    private String no;
    /**
     * 询价单编号
     *
     * 关联 {@link ErpPurchaseInquiryDO#getId()}
     */
    private Long inquiryId;
    /**
     * 供应商编号
     *
     * 关联 {@link ErpSupplierDO#getId()}
     */
    private Long supplierId;
    /**
     * 报价时间
     */
    private LocalDateTime quoteDate;
    /**
     * 合计金额，单位：元
     */
    private BigDecimal totalAmount;
    /**
     * 状态
     *
     * 枚举：10 草稿 / 20 已报价 / 30 已采纳 / 40 已拒绝
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
