package cn.zhicloud.module.airag.service.rag;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * BM25 词法检索失败可观测性测试。
 */
class Bm25LexicalRetrieverTest {

    @Test
    void retrieve_doesNotSilentlySwallowQueryFailure() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DataAccessResourceFailureException databaseFailure =
                new DataAccessResourceFailureException("pgvector unavailable");
        doThrow(databaseFailure)
                .when(jdbcTemplate)
                .query(anyString(), any(RowMapper.class), eq("1"), eq("1"));

        Bm25LexicalRetriever retriever = new Bm25LexicalRetriever(jdbcTemplate, "airag_vector_store");

        assertThatThrownBy(() -> retriever.retrieve("query", 1L, 1L, 5))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BM25")
                .hasCause(databaseFailure);
    }
}