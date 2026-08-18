package cn.iocoder.yudao.module.qms.dal.dataobject.inspectionorder;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionOrderStatusEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QMS 检验单 DO
 *
 * @author 芋道源码
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
     * 枚举 {@link cn.iocoder.yudao.module.qms.enums.qms.InspectionBizTypeEnum}
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
