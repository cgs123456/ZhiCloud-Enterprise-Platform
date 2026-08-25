package cn.zhicloud.module.qms.dal.dataobject.document;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * QMS 文档分发记录 DO
 *
 * @author 智云
 */
@TableName("qms_document_distribute")
@KeySequence("qms_document_distribute_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsDocumentDistributeDO extends TenantBaseDO {

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
     * 分发对象
     */
    private String distributeTo;
    /**
     * 分发份数
     */
    private Integer distributeQty;
    /**
     * 分发日期
     */
    private LocalDate distributeDate;
    /**
     * 签收人
     */
    private String receivedBy;
    /**
     * 签收日期
     */
    private LocalDate receivedDate;
    /**
     * 回收份数
     */
    private Integer returnedQty;
    /**
     * 回收日期
     */
    private LocalDate returnedDate;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
