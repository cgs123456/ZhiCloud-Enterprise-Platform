package cn.iocoder.yudao.module.qms.dal.dataobject.document;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.document.QmsChangeRequestStatusEnum;
import cn.iocoder.yudao.module.qms.enums.document.QmsDocChangeTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * QMS 文件变更申请 DO
 *
 * @author 芋道源码
 */
@TableName("qms_document_change_request")
@KeySequence("qms_document_change_request_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsDocumentChangeRequestDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 受控文档 ID
     *
     * 关联 {@link QmsDocumentDO#getId()}
     */
    private Long documentId;
    /**
     * 变更类型
     *
     * 枚举 {@link QmsDocChangeTypeEnum}
     */
    private Integer changeType;
    /**
     * 变更原因
     */
    private String changeReason;
    /**
     * 变更内容
     */
    private String changeContent;
    /**
     * 申请人 ID
     */
    private Long applicantId;
    /**
     * 申请日期
     */
    private LocalDate applyDate;
    /**
     * 审批人 ID
     */
    private Long approverId;
    /**
     * 审批日期
     */
    private LocalDateTime approveDate;
    /**
     * 状态
     *
     * 枚举 {@link QmsChangeRequestStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
