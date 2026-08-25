package cn.zhicloud.module.oa.dal.dataobject.document;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * OA 公文附件 DO
 *
 * @author zhicloud
 */
@TableName("oa_document_attachment")
@KeySequence("oa_document_attachment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaDocumentAttachmentDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 公文 ID
     */
    private Long documentId;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件地址
     */
    private String fileUrl;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * 文件类型
     */
    private String fileType;

}
