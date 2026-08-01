package cn.iocoder.yudao.module.qms.dal.dataobject.document;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.document.QmsDocStatusEnum;
import cn.iocoder.yudao.module.qms.enums.document.QmsDocTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * QMS 受控文档 DO
 *
 * @author 芋道源码
 */
@TableName("qms_document")
@KeySequence("qms_document_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsDocumentDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 文件编号
     */
    private String docNo;
    /**
     * 标题
     */
    private String title;
    /**
     * 文件类型
     *
     * 枚举 {@link QmsDocTypeEnum}
     */
    private Integer docType;
    /**
     * 版本号
     */
    private String version;
    /**
     * 状态
     *
     * 枚举 {@link QmsDocStatusEnum}
     */
    private Integer status;
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;
    /**
     * 失效日期
     */
    private LocalDate expiryDate;
    /**
     * 审批人 ID
     */
    private Long approverId;
    /**
     * 审批日期
     */
    private LocalDateTime approveDate;
    /**
     * 归属部门 ID
     */
    private Long ownerDeptId;
    /**
     * 文件 URL
     */
    private String fileUrl;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
