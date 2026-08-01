package cn.iocoder.yudao.module.ai.service.nl2sql;

import cn.iocoder.yudao.module.ai.controller.admin.nl2sql.vo.Nl2SqlQueryRespVO;

/**
 * NL2SQL AI 报表分析 Service 接口
 *
 * <p>核心能力：自然语言 → SQL → 只读查询 → 报表数据
 *
 * <h3>典型流程</h3>
 * <pre>
 * 1. translateToSql：调用 ChatClient 将自然语言转换为 SQL
 * 2. executeSql：执行只读查询（四层安全校验）
 * 3. queryByNaturalLanguage：组合上述两步，返回结构化报表
 * </pre>
 *
 * @author yudao
 */
public interface Nl2SqlService {

    /**
     * 将自然语言转换为 SQL
     *
     * @param naturalLanguage 自然语言问题
     * @param dataSource 数据源标识（预留，用于多 schema 切换），为空时使用默认 schema
     * @return 生成的 SQL 语句
     */
    String translateToSql(String naturalLanguage, String dataSource);

    /**
     * 执行只读 SQL 查询
     *
     * <p>安全约束：调用 {@link SqlSafetyValidator} 进行四层校验，仅允许 SELECT/WITH 语句。
     *
     * @param sql SQL 语句
     * @return 查询结果（已脱敏 tenant_id 占位符）
     */
    Nl2SqlQueryRespVO executeSql(String sql);

    /**
     * 自然语言查询（组合 translateToSql + executeSql）
     *
     * @param naturalLanguage 自然语言问题
     * @return 查询结果（含生成的 SQL + 结果集）
     */
    Nl2SqlQueryRespVO queryByNaturalLanguage(String naturalLanguage);

}
