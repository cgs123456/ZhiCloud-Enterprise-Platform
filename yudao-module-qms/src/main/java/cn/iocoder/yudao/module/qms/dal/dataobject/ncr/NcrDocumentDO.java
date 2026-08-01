package cn.iocoder.yudao.module.qms.dal.dataobject.ncr;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.NcrDefectLevelEnum;
import cn.iocoder.yudao.module.qms.enums.qms.NcrDispositionEnum;
import cn.iocoder.yudao.module.qms.enums.qms.NcrSourceEnum;
import cn.iocoder.yudao.module.qms.enums.qms.NcrStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * QMS 不合格品报告 DO
 *
 * @author 芋道源码
 */
@TableName("qms_ncr_document")
@KeySequence("qms_ncr_document_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NcrDocumentDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * NCR 单号
     */
    private String ncrNo;
    /**
     * 来源
     *
     * 枚举 {@link NcrSourceEnum}
     */
    private Integer source;
    /**
     * 检验单 ID
     */
    private Long inspectionOrderId;
    /**
     * 产品 ID
     */
    private Long productId;
    /**
     * 供应商 ID
     */
    private Long supplierId;
    /**
     * 工单 ID
     */
    private Long workOrderId;
    /**
     * 缺陷描述
     */
    private String defectDescription;
    /**
     * 缺陷等级
     *
     * 枚举 {@link NcrDefectLevelEnum}
     */
    private Integer defectLevel;
    /**
     * 不合格数量
     */
    private BigDecimal quantity;
    /**
     * 处置方式
     *
     * 枚举 {@link NcrDispositionEnum}
     */
    private Integer disposition;
    /**
     * 状态
     *
     * 枚举 {@link NcrStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
