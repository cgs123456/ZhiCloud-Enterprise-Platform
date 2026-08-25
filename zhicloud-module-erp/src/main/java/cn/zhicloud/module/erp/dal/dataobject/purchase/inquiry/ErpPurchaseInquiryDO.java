package cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 采购询价单 DO
 *
 * @author 智云
 */
@TableName(value = "erp_purchase_inquiry")
@KeySequence("erp_purchase_inquiry_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInquiryDO extends BaseDO {

    /**
     * 状态 - 草稿
     */
    public static final Integer STATUS_DRAFT = 10;
    /**
     * 状态 - 已发布
     */
    public static final Integer STATUS_PUBLISHED = 20;
    /**
     * 状态 - 已比价
     */
    public static final Integer STATUS_COMPARED = 30;
    /**
     * 状态 - 已关闭
     */
    public static final Integer STATUS_CLOSED = 40;
    /**
     * 状态 - 已转采购订单
     */
    public static final Integer STATUS_CONVERTED = 50;

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 询价单号
     */
    private String no;
    /**
     * 询价主题
     */
    private String inquiryName;
    /**
     * 供应商编号列表，多供应商逗号分隔
     *
     * 关联 {@link cn.zhicloud.module.erp.dal.dataobject.purchase.ErpSupplierDO#getId()}
     */
    private String supplierIds;
    /**
     * 状态
     *
     * 枚举：10 草稿 / 20 已发布 / 30 已比价 / 40 已关闭 / 50 已转采购订单
     */
    private Integer status;
    /**
     * 合计金额，单位：元
     */
    private BigDecimal totalAmount;
    /**
     * 期望交货日期
     */
    private LocalDate expectedDeliveryDate;
    /**
     * 备注
     */
    private String remark;

}
