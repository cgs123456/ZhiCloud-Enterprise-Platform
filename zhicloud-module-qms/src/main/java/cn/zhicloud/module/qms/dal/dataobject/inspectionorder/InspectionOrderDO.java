package cn.zhicloud.module.qms.dal.dataobject.inspectionorder;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.qms.enums.qms.InspectionOrderStatusEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QMS 检验单 DO
 *
 * @author 智云
 */
@TableName("qms_inspection_order")
@KeySequence("qms_inspection_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionOrderDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 乐观锁版本号（P2：@Version 并发保护）
     */
    @Version
    private Long version;
    /**
     * 检验单号
     */
    private String orderNo;
    /**
     * 检验类型
     *
     * 枚举 {@link InspectionTypeEnum}
     */
    private Integer type;
    /**
     * 供应商 ID
     *
     * <p>关联 ERP 供应商主数据 {@code erp_supplier.id}（ErpSupplierDO#getId()），与 ERP/MES 侧 supplierId 同一口径。
     * 注意不要拿 {@code wms_receipt_order.merchant_id}（那是 WmsMerchantDO 商户）回填这里，两者不是同一实体。
     */
    private Long supplierId;
    /**
     * 批次号
     */
    private String batchNo;
    /**
     * 工单 ID
     */
    private Long workOrderId;
    /**
     * 产品 ID
     *
     * <p>关联 MES 物料主数据 {@code mes_md_item.id}（MesMdItemDO#getId()）——与 MES 侧
     * {@code MesProWorkOrderDO.productId} 同口径，追溯链路里也是把它当物料（materialId）使用的。
     * 它不是商城的 product_spu / product_sku，也不是 WMS 的 wms_item，跨系统取数时不要混用。
     */
    private Long productId;
    /**
     * 检验员
     */
    private String inspector;
    /**
     * 检验时间
     */
    private LocalDateTime inspectTime;
    /**
     * 状态
     *
     * 枚举 {@link InspectionOrderStatusEnum}
     */
    private Integer status;
    /**
     * AQL 接收数 Ac（缺陷数 <= Ac 判合格）
     */
    private Integer acceptanceQuantity;
    /**
     * AQL 拒收数 Re（缺陷数 >= Re 判不合格）
     */
    private Integer rejectQuantity;
    /**
     * 业务类型
     *
     * 枚举 {@link cn.zhicloud.module.qms.enums.qms.InspectionBizTypeEnum}
     */
    private String bizType;
    /**
     * 业务单据 ID（与 {@link #bizType} 共同定位关联业务）
     */
    private Long bizId;
    /**
     * 备注
     */
    private String remark;

}
