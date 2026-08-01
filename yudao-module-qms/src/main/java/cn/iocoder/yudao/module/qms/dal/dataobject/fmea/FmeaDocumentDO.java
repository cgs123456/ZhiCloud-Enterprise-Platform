package cn.iocoder.yudao.module.qms.dal.dataobject.fmea;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.FmeaStatusEnum;
import cn.iocoder.yudao.module.qms.enums.qms.FmeaTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * QMS FMEA 文档 DO
 *
 * @author 芋道源码
 */
@TableName("qms_fmea_document")
@KeySequence("qms_fmea_document_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FmeaDocumentDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * FMEA 单号
     */
    private String fmeaNo;
    /**
     * FMEA 类型
     *
     * 枚举 {@link FmeaTypeEnum}
     */
    private Integer fmeaType;
    /**
     * 产品 ID
     */
    private Long productId;
    /**
     * 工序 ID
     */
    private Long processId;
    /**
     * 版本
     */
    private String version;
    /**
     * 状态
     *
     * 枚举 {@link FmeaStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
