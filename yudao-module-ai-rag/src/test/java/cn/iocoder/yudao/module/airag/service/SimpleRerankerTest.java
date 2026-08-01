package cn.iocoder.yudao.module.airag.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SimpleReranker} 的单元测试（TF-IDF + query 覆盖率打分）
 *
 * @author yudao
 */
public class SimpleRerankerTest extends BaseMockitoUnitTest {

    private SimpleReranker reranker = new SimpleReranker();

    private Document doc(String id, String text) {
        return Document.builder().id(id).text(text).metadata(new java.util.HashMap<>()).build();
    }

    @Test
    public void testRerank_ordersByRelevance() {
        List<Document> docs = List.of(
                doc("d1", "质量控制图 SPC 分析 过程能力"),
                doc("d2", "财务报表 利润 现金流"),
                doc("d3", "过程控制 方法 统计")
        );
        List<Document> result = reranker.rerank("质量 控制 图", docs, 3);
        assertEquals(3, result.size());
        // 完全命中的 d1 应排第一，零命中的 d2 应排最后
        assertEquals("d1", result.get(0).getId());
        assertEquals("d2", result.get(2).getId());
        // d1 分数应严格高于 d2
        assertTrue(result.get(0).getScore() > result.get(2).getScore());
    }

    @Test
    public void testRerank_emptyDocs() {
        assertTrue(reranker.rerank("质量", List.of(), 3).isEmpty());
    }

    @Test
    public void testRerank_blankQuery_keepsOrder() {
        List<Document> docs = List.of(doc("d1", "a"), doc("d2", "b"));
        List<Document> result = reranker.rerank("   ", docs, 2);
        assertEquals(2, result.size());
        assertEquals("d1", result.get(0).getId());
    }

    @Test
    public void testRerank_topKLimitsSize() {
        List<Document> docs = List.of(
                doc("d1", "质量控制 过程能力 分析"),
                doc("d2", "财务报表 利润"),
                doc("d3", "过程控制 方法")
        );
        List<Document> result = reranker.rerank("质量 控制", docs, 1);
        assertEquals(1, result.size());
        assertEquals("d1", result.get(0).getId());
    }

    @Test
    public void testRerank_stopwordOnlyQuery_noException() {
        // 仅含停用词的查询：分词后为空，应安全返回（不抛异常）
        List<Document> docs = List.of(doc("d1", "质量 控制"), doc("d2", "利润 报表"));
        List<Document> result = reranker.rerank("的 和 是", docs, 2);
        assertFalse(result.isEmpty());
    }
}
