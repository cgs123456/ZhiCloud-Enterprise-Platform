package cn.zhicloud.module.airag.dal.dataobject;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * AI RAG 文档 DO
 *
 * 对应 {@code airag_document} 表，记录知识库下导入的原始文档及其向量化处理状态。
 *
 * @author zhicloud
 */
@TableName(value = "airag_document", autoResultMap = true)
@KeySequence("airag_document_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class AiragDocumentDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 知识库编号
     *
     * 关联 {@link AiragKnowledgeDO#getId()}
     */
    private Long knowledgeId;
    /**
     * 文档名称
     */
    private String name;
    /**
     * 文档类型
     *
     * 例如：pdf / docx / txt / md
     */
    private String type;
    /**
     * 文件 URL
     */
    private String url;
    /**
     * 处理状态
     *
     * 枚举：
     *   0 - 待处理
     *   1 - 处理中
     *   2 - 已完成
     *   3 - 失败
     */
    private Integer status;
    /**
     * 分块数量
     */
    private Integer chunkCount;
    /**
     * 错误信息
     */
    private String errorMsg;

}
