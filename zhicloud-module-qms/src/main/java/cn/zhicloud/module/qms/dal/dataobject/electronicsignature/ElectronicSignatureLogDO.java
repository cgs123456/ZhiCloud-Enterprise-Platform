package cn.zhicloud.module.qms.dal.dataobject.electronicsignature;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * QMS 电子签名记录 DO（21 CFR Part 11）
 *
 * @author 智云
 */
@TableName("qms_electronic_signature_log")
@KeySequence("qms_electronic_signature_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicSignatureLogDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 用户 ID
     */
    private Long userId;
    /**
     * 签名含义
     */
    private String signatureMeaning;
    /**
     * 操作类型
     */
    private String operationType;
    /**
     * 操作内容
     */
    private String operationContent;
    /**
     * 签名时间
     */
    private LocalDateTime signatureTime;
    /**
     * IP 地址
     */
    private String ipAddress;
    /**
     * 备注
     */
    private String remark;

}
