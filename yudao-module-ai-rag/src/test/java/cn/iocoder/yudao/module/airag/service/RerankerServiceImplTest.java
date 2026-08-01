package cn.iocoder.yudao.module.airag.service;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RerankerServiceImpl} 的单元测试
 *
 * <p>验证 ONNX Cross-Encoder 未就绪（模型文件缺失）时，正确降级到 {@link SimpleReranker}，
 * 保证 Reranker 链路在无模型依赖下依然可用。
 *
 * @author yudao
 */
public class RerankerServiceImplTest extends BaseMockitoUnitTest {

    private Document doc(String id, String text) {
        return Document.builder().id(id).text(text).metadata(new java.util.HashMap<>()).build();
    }

    @Test
    public void testRerank_fallbackToSimpleReranker() {
        // 不触发 @PostConstruct，crossEncoderReranker 保持 null；显式开启 fallback
        RerankerServiceImpl service = new RerankerServiceImpl();
        ReflectionTestUtils.setField(service, "fallbackToSimple", true);

        List<Document> docs = List.of(
                doc("d1", "质量控制图 SPC 分析 过程能力"),
                doc("d2", "财务报表 利润 现金流"),
                doc("d3", "过程控制 方法 统计")
        );
        List<Document> result = service.rerank("质量 控制 图", docs, 3);
        assertEquals(3, result.size());
        // fallback 路径应与 SimpleReranker 行为一致：d1 最相关、d2 最不相关
        assertEquals("d1", result.get(0).getId());
        assertEquals("d2", result.get(2).getId());
        assertTrue(result.get(0).getScore() > result.get(2).getScore());
    }

    @Test
    public void testRerank_emptyDocs_noException() {
        RerankerServiceImpl service = new RerankerServiceImpl();
        ReflectionTestUtils.setField(service, "fallbackToSimple", true);
        assertTrue(service.rerank("质量", List.of(), 3).isEmpty());
    }

    @Test
    public void testRerank_noFallback_returnsOriginalOrder() {
        // fallback 关闭且模型未加载：按原顺序取 topK，不抛异常
        RerankerServiceImpl service = new RerankerServiceImpl();
        ReflectionTestUtils.setField(service, "fallbackToSimple", false);
        List<Document> docs = List.of(doc("d1", "质量"), doc("d2", "利润"));
        List<Document> result = service.rerank("质量", docs, 2);
        assertFalse(result.isEmpty());
        assertEquals("d1", result.get(0).getId());
    }
}
