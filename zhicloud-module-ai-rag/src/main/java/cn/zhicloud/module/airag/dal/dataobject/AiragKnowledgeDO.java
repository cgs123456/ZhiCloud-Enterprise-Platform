package cn.zhicloud.module.airag.dal.dataobject;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * AI RAG 知识库 DO
 *
 * 对应 {@code airag_knowledge} 表，存储本地化 RAG 的知识库元信息。
 *
 * @author zhicloud
 */
@TableName(value = "airag_knowledge", autoResultMap = true)
@KeySequence("airag_knowledge_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class AiragKnowledgeDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 知识库名称
     */
    private String name;
    /**
     * 知识库描述
     */
    private String description;
    /**
     * 状态
     *
     * 枚举：0-开启 1-停用（参考 {@code CommonStatusEnum}）
     */
    private Integer status;
    /**
     * Embedding 模型标识
     *
     * 例如：{@code bge-base-zh}、{@code bge-large-zh}、{@code text-embedding-3-small}
     */
    private String embeddingModel;
    /**
     * 向量维度
     *
     * 与 Embedding 模型保持对齐：
     *   - bge-small-zh：512
     *   - bge-base-zh：768（默认）
     *   - bge-large-zh：1024
     */
    private Integer vectorDimension;

}
