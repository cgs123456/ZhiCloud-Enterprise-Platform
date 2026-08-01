package cn.iocoder.yudao.module.qms.dal.dataobject.sqm;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.ScarStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QMS SCAR（Supplier Corrective Action Request 供应商纠正措施请求）DO
 *
 * @author yudao
 */
@TableName("qms_scar")
@KeySequence("qms_scar_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScarDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * SCAR 单号
     */
    private String scarNo;
    /**
     * 供应商 ID
     */
    private Long supplierId;
    /**
     * 供应商名称
     */
    private String supplierName;
    /**
     * 产品 ID
     */
    private Long productId;
    /**
     * 产品名称
     */
    private String productName;
    /**
     * 缺陷描述
     */
    private String defectDescription;
    /**
     * 根本原因
     */
    private String rootCause;
    /**
     * 纠正措施
     */
    private String correctiveAction;
    /**
     * 状态
     *
     * 枚举 {@link ScarStatusEnum}
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