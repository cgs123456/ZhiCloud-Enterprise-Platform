package cn.zhicloud.module.oa.dal.dataobject.knowledge;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * OA 知识库版本 DO
 *
 * @author zhicloud
 */
@TableName("oa_knowledge_version")
@KeySequence("oa_knowledge_version_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaKnowledgeVersionDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 文章 ID
     */
    private Long articleId;
    /**
     * 版本号
     */
    private Integer versionNo;
    /**
     * 该版本标题
     */
    private String title;
    /**
     * 该版本正文
     */
    private String content;
    /**
     * 该版本摘要
     */
    private String summary;
    /**
     * 变更说明
     */
    private String changeLog;
    /**
     * 编辑人 ID
     */
    private Long editorUserId;
    /**
     * 编辑人姓名
     */
    private String editorName;

}
