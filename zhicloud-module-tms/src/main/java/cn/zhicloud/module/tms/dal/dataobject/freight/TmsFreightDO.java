package cn.zhicloud.module.tms.dal.dataobject.freight;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TMS 运费结算单 DO
 *
 * @author zhicloud
 */
@TableName("tms_freight")
@KeySequence("tms_freight_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsFreightDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 结算单号
     */
    private String no;
    /**
     * 运单编号
     */
    private Long shipmentId;
    /**
     * 承运商编号
     */
    private Long carrierId;
    /**
     * 计费方式
     *
     * 10 按重量 / 20 按体积 / 30 按件数 / 40 整车一口价 / 50 里程计费
     */
    private Integer billingMethod;
    /**
     * 计费数量
     */
    private BigDecimal billingQuantity;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 附加费用
     */
    private BigDecimal surcharge;
    /**
     * 折扣金额
     */
    private BigDecimal discountAmount;
    /**
     * 运费总额
     */
    private BigDecimal totalAmount;
    /**
     * 结算状态
     *
     * 10 待审核 / 20 已审核 / 30 已结算 / 40 已驳回
     */
    private Integer status;
    /**
     * 审核人
     */
    private String auditor;
    /**
     * 审核时间
     */
    private LocalDateTime auditTime;
    /**
     * 结算时间
     */
    private LocalDateTime settleTime;
    /**
     * 驳回原因
     */
    private String rejectReason;
    /**
     * 备注
     */
    private String remark;

}
