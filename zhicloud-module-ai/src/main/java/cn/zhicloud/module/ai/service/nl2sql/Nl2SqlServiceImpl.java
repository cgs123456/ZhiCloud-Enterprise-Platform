package cn.zhicloud.module.ai.service.nl2sql;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.ai.controller.admin.nl2sql.vo.Nl2SqlQueryRespVO;
import cn.zhicloud.module.ai.dal.dataobject.model.AiModelDO;
import cn.zhicloud.module.ai.dal.dataobject.nl2sql.AiNl2SqlQueryHistoryDO;
import cn.zhicloud.module.ai.dal.mysql.nl2sql.AiNl2SqlQueryHistoryMapper;
import cn.zhicloud.module.ai.enums.model.AiModelTypeEnum;
import cn.zhicloud.module.ai.service.model.AiModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.NL2SQL_LLM_UNAVAILABLE;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.NL2SQL_EXECUTE_FAILED;

/**
 * NL2SQL AI 报表分析 Service 实现类
 *
 * <h3>实现说明</h3>
 * <ol>
 *   <li>translateToSql：调用 ChatClient（优先使用 Bean，兜底通过 AiModelService 构建临时 ChatClient）</li>
 *   <li>executeSql：使用 {@link JdbcTemplate} 执行只读查询，调用前进行四层安全校验</li>
 *   <li>多租户：将 LLM 生成的 SQL 中的 {@code ${tenantId}} 占位符替换为当前租户 ID</li>
 *   <li>历史记录：每次查询异步写入 {@code ai_nl2sql_query_history} 表，便于审计</li>
 * </ol>
 *
 * @author zhicloud
 */
@Service
@Slf4j
public class Nl2SqlServiceImpl implements Nl2SqlService {

    /**
     * SQL 中 tenant_id 占位符
     */
    private static final String TENANT_ID_PLACEHOLDER = "${tenantId}";

    /**
     * 最大返回行数（安全防护，避免超大结果集）
     */
    private static final int MAX_ROW_LIMIT = 1000;

    @Resource
    private SqlSafetyValidator sqlSafetyValidator;

    @Resource
    private AiNl2SqlQueryHistoryMapper queryHistoryMapper;

    /**
     * ChatClient Bean（可选；zhicloud-module-ai 未直接提供时为 null）
     */
    @Autowired(required = false)
    private ChatClient chatClient;

    /**
     * AiModelService（可选；用于在 ChatClient 不可用时按需构建 ChatModel）
     */
    @Autowired(required = false)
    private AiModelService aiModelService;

    /**
     * JdbcTemplate（可选；由 Spring Boot 从主数据源自动配置）
     */
    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * Prompt 构建器
     */
    private final Nl2SqlPromptBuilder promptBuilder = new Nl2SqlPromptBuilder();

    @Override
    public String translateToSql(String naturalLanguage, String dataSource) {
        String systemPrompt = promptBuilder.buildSystemPrompt(dataSource);
        String userPrompt = promptBuilder.buildUserPrompt(naturalLanguage);
        String raw = callLlm(systemPrompt, userPrompt);
        String sql = promptBuilder.cleanSql(raw);
        log.info("[translateToSql][自然语言={} → SQL={}]", naturalLanguage, sql);
        return sql;
    }

    @Override
    public Nl2SqlQueryRespVO executeSql(String sql) {
        Nl2SqlQueryRespVO respVO = new Nl2SqlQueryRespVO().setSql(sql).setStatus(0);
        long start = System.currentTimeMillis();
        // 1. 安全校验（预检 + JSqlParser AST 白名单校验，失败抛出带原因的 ServiceException）
        sqlSafetyValidator.validate(sql);
        // 2. 替换 tenant_id 占位符为当前租户 ID
        String finalSql = injectTenantId(sql);
        // 3. 执行查询
        if (jdbcTemplate == null) {
            throw exception(NL2SQL_EXECUTE_FAILED);
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(finalSql);
        // 4. 安全防护：截断超大结果集
        if (rows.size() > MAX_ROW_LIMIT) {
            rows = new ArrayList<>(rows.subList(0, MAX_ROW_LIMIT));
            log.warn("[executeSql][结果集超过 {} 行，已截断]", MAX_ROW_LIMIT);
        }
        // 5. 提取列名（基于第一行，若无行则空列表）
        List<String> columns = rows.isEmpty() ? new ArrayList<>() : new ArrayList<>(rows.get(0).keySet());
        respVO.setColumns(columns)
                .setRows(rows)
                .setRowCount(rows.size())
                .setCostMs(System.currentTimeMillis() - start);
        return respVO;
    }

    @Override
    public Nl2SqlQueryRespVO queryByNaturalLanguage(String naturalLanguage) {
        Nl2SqlQueryRespVO respVO = new Nl2SqlQueryRespVO().setNaturalLanguage(naturalLanguage).setStatus(0);
        long start = System.currentTimeMillis();
        AiNl2SqlQueryHistoryDO history = new AiNl2SqlQueryHistoryDO()
                .setNaturalLanguage(naturalLanguage);
        try {
            // 1. 自然语言 → SQL
            String sql = translateToSql(naturalLanguage, null);
            history.setSql(sql);
            respVO.setSql(sql);
            // 2. 执行 SQL
            Nl2SqlQueryRespVO execResult = executeSql(sql);
            respVO.setColumns(execResult.getColumns())
                    .setRows(execResult.getRows())
                    .setRowCount(execResult.getRowCount())
                    .setStatus(execResult.getStatus())
                    .setErrorMsg(execResult.getErrorMsg());
            history.setStatus(execResult.getStatus()).setRowCount(execResult.getRowCount());
        } catch (Exception e) {
            respVO.setStatus(2).setErrorMsg(StrUtil.sub(e.getMessage(), 0, 500));
            history.setStatus(2).setErrorMsg(StrUtil.sub(e.getMessage(), 0, 500));
            log.error("[queryByNaturalLanguage][查询失败，naturalLanguage={}]", naturalLanguage, e);
        } finally {
            // 3. 记录历史
            history.setCostMs(System.currentTimeMillis() - start);
            saveHistorySafely(history);
        }
        return respVO;
    }

    // ==================== 内部方法 ====================

    /**
     * 调用 LLM 生成 SQL
     *
     * <p>优先使用 ChatClient Bean；不可用时通过 AiModelService 构建临时 ChatClient。
     */
    private String callLlm(String systemPrompt, String userPrompt) {
        if (chatClient != null) {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        }
        if (aiModelService != null) {
            AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            ChatModel chatModel = aiModelService.getChatModel(model.getId());
            ChatClient client = ChatClient.builder(chatModel).build();
            return client.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        }
        throw exception(NL2SQL_LLM_UNAVAILABLE);
    }

    /**
     * 将 SQL 中的 {@code ${tenantId}} 占位符替换为当前租户 ID
     *
     * <p>租户 ID 为 Long 类型，直接替换为数字字面量，无 SQL 注入风险。
     */
    private String injectTenantId(String sql) {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            // 无租户上下文（如定时任务），替换为 0 防止占位符泄露到 SQL
            tenantId = 0L;
        }
        return sql.replace(TENANT_ID_PLACEHOLDER, String.valueOf(tenantId));
    }

    /**
     * 安全保存查询历史（异常不影响主流程）
     */
    private void saveHistorySafely(AiNl2SqlQueryHistoryDO history) {
        try {
            queryHistoryMapper.insert(history);
        } catch (Exception e) {
            log.warn("[saveHistorySafely][保存查询历史失败，naturalLanguage={}]",
                    history.getNaturalLanguage(), e);
        }
    }

}
