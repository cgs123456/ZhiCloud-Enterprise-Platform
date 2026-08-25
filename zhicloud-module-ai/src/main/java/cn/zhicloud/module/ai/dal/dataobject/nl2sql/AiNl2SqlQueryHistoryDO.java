package cn.zhicloud.module.ai.dal.dataobject.nl2sql;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * AI NL2SQL 查询历史 DO
 *
 * <p>记录每次自然语言查询的输入、生成的 SQL、执行状态与结果摘要，用于审计与优化。
 *
 * @author zhicloud
 */
@TableName(value = "ai_nl2sql_query_history", autoResultMap = true)
@KeySequence("ai_nl2sql_query_history_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class AiNl2SqlQueryHistoryDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 自然语言问题
     */
    private String naturalLanguage;
    /**
     * 生成的 SQL
     */
    private String sql;
    /**
     * 数据源标识（预留，用于多 schema 切换）
     */
    private String dataSource;
    /**
     * 执行状态
     *
     * 枚举：0-成功 1-校验失败 2-执行失败
     */
    private Integer status;
    /**
     * 结果行数
     */
    private Integer rowCount;
    /**
     * 错误信息
     */
    private String errorMsg;
    /**
     * 耗时（毫秒）
     */
    private Long costMs;

}
