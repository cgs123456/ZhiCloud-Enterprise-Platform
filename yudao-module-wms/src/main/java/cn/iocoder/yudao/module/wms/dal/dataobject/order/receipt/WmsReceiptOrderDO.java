package cn.iocoder.yudao.module.wms.dal.dataobject.order.receipt;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.enums.DictTypeConstants;
import cn.iocoder.yudao.module.wms.enums.order.WmsReceiptOrderTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WMS 入库单 DO
 *
 * @author 芋道源码
 */
@TableName("wms_receipt_order")
@KeySequence("wms_receipt_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsReceiptOrderDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 入库单号
     */
    private String no;
    /**
     * 入库类型
     *
     * 枚举 {@link WmsReceiptOrderTypeEnum}
     * 字典 {@link DictTypeConstants#RECEIPT_ORDER_TYPE}
     */
    private Integer type;
    /**
     * 单据日期
     */
    private LocalDateTime orderTime;
    /**
     * 入库状态
     *
     * 字典 {@link DictTypeConstants#ORDER_STATUS}
     */
    private Integer status;
    /**
     * 业务订单号
     */
    private String bizOrderNo;
    /**
     * 质检关联业务 ID（可选）
     *
     * <p>填写后，入库完成（写库存）前会前置校验 QMS 对应检验单是否为「检验通过」，不合格拒绝入库（fail-closed）。
     * 不填则不强质检卡点，保持既有手工入库行为。
     */
    private Long qcBizId;
    /**
     * 质检业务类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.qms.enums.qms.InspectionBizTypeEnum#getBizType()}
     * 默认 {@code PURCHASE_IN}
     */
    private String qcBizType;
    /**
     * 供应商编号
     *
     * 关联 {@link WmsMerchantDO#getId()}
     */
    private Long merchantId;
    /**
     * 备注
     */
    private String remark;

    // ========= 仓库字段 =========

    /**
     * 仓库编号
     *
     * 关联 {@link WmsWarehouseDO#getId()}
     */
    private Long warehouseId;

    // ========= 汇总金额字段 =========

    /**
     * 总数量
     */
    private BigDecimal totalQuantity;
    /**
     * 总金额
     */
    private BigDecimal totalPrice;

}
