package cn.iocoder.yudao.module.erp.dal.dataobject.purchase.inquiry;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 采购询价单明细 DO
 *
 * @author 芋道源码
 */
@TableName("erp_purchase_inquiry_item")
@KeySequence("erp_purchase_inquiry_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInquiryItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 询价单编号
     *
     * 关联 {@link ErpPurchaseInquiryDO#getId()}
     */
    private Long inquiryId;
    /**
     * 产品编号
     *
     * 关联 {@link ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 产品名称
     *
     * 冗余 {@link ErpProductDO#getName()}
     */
    private String productName;
    /**
     * 数量
     */
    private BigDecimal quantity;
    /**
     * 单位
     */
    private String unit;
    /**
     * 期望价，单位：元
     */
    private BigDecimal unitPrice;
    /**
     * 期望交货日期
     */
    private LocalDate deliveryDate;
    /**
     * 备注
     */
    private String remark;

}
