package cn.iocoder.yudao.module.oa.dal.dataobject.knowledge;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * OA 知识库评论 DO
 *
 * @author yudao
 */
@TableName("oa_knowledge_comment")
@KeySequence("oa_knowledge_comment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaKnowledgeCommentDO extends TenantBaseDO {

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
     * 父评论 ID（0 为根评论）
     */
    private Long parentId;
    /**
     * 评论内容
     */
    private String content;
    /**
     * 评论人 ID
     */
    private Long commentatorUserId;
    /**
     * 评论人姓名
     */
    private String commentatorName;
    /**
     * 点赞数
     */
    private Integer likeCount;
    /**
     * 状态
     * <p>
     * 0 正常 1 已删除
     */
    private Integer status;

}
