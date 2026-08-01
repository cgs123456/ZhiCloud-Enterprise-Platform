package cn.iocoder.yudao.module.oa.dal.dataobject.knowledge;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * OA 知识库文章 DO
 *
 * @author yudao
 */
@TableName("oa_knowledge_article")
@KeySequence("oa_knowledge_article_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OaKnowledgeArticleDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 分类 ID
     */
    private Long categoryId;
    /**
     * 标题
     */
    private String title;
    /**
     * 摘要
     */
    private String summary;
    /**
     * 正文（Markdown/HTML）
     */
    private String content;
    /**
     * 标签（逗号分隔）
     */
    private String tags;
    /**
     * 作者 ID
     */
    private Long authorUserId;
    /**
     * 作者姓名
     */
    private String authorName;
    /**
     * 阅读量
     */
    private Integer viewCount;
    /**
     * 点赞数
     */
    private Integer likeCount;
    /**
     * 评论数
     */
    private Integer commentCount;
    /**
     * 当前版本号
     */
    private Integer currentVersion;
    /**
     * 状态
     * <p>
     * 10 草稿 20 已发布 30 已下架
     */
    private Integer status;
    /**
     * 是否置顶
     */
    private Boolean topFlag;
    /**
     * 备注
     */
    private String remark;

}
