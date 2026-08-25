package cn.zhicloud.module.erp.dal.dataobject.purchase.inquiry;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 采购比价单 DO
 *
 * @author 智云
 */
@TableName(value = "erp_purchase_compare")
@KeySequence("erp_purchase_compare_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseCompareDO extends BaseDO {

    /**
     * 状态 - 草稿
     */
    public static final Integer STATUS_DRAFT = 10;
    /**
     * 状态 - 已完成
     */
    public static final Integer STATUS_FINISHED = 20;

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 比价单号
     */
    private String no;
    /**
     * 询价单编号
     *
     * 关联 {@link ErpPurchaseInquiryDO#getId()}
     */
    private Long inquiryId;
    /**
     * 推荐供应商编号
     *
     * 关联 {@link ErpSupplierDO#getId()}
     */
    private Long recommendSupplierId;
    /**
     * 推荐理由
     */
    private String recommendReason;
    /**
     * 报价总数（参与比价的供应商数量）
     */
    private Integer totalQuoteCount;
    /**
     * 状态
     *
     * 枚举：10 草稿 / 20 已完成
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
