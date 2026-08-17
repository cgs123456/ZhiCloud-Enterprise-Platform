package cn.iocoder.yudao.module.airag.service.rag;

import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BM25 词法检索器（混合召回的「词法路径」）。
 *
 * <p>与向量语义路径相互独立：直接查询向量库底表（{@code airag_vector_store}）中
 * 属于指定知识库 / 租户的全部 chunk 文本，在应用层用 Okapi BM25 计算词法相关度，
 * 输出 topK 候选。最终由调用方与向量召回结果做 RRF 融合，再送 Cross-Encoder 重排。
 *
 * <p>分词策略兼容中英文混合语料：英文 / 数字按词切分，CJK 字符按字切分，
 * 使中文文档也能获得可用的词法召回信号，无需额外中文分词依赖。
 *
 * @author yudao
 */
public class Bm25LexicalRetriever {

    /** Okapi BM25 参数。 */
    private static final double K1 = 1.5;
    private static final double B = 0.75;

    /** 英文 / 数字词。 */
    private static final Pattern EN_PATTERN = Pattern.compile("[a-zA-Z0-9]+");
    /** CJK 统一表意文字（基本区 + 扩展 A）。 */
    private static final Pattern CJK_PATTERN = Pattern.compile("[\\u4e00-\\u9fff\\u3400-\\u4dbf]");

    private final JdbcTemplate jdbcTemplate;
    private final String vectorTableName;

    public Bm25LexicalRetriever(JdbcTemplate jdbcTemplate, String vectorTableName) {
        this.jdbcTemplate = jdbcTemplate;
        this.vectorTableName = vectorTableName;
    }

    /**
     * 对指定知识库执行 BM25 词法召回。
     *
     * @param query      用户问题
     * @param knowledgeId 知识库 ID（用于隔离语料）
     * @param tenantId   租户 ID（多租户隔离）
     * @param topK       返回候选数
     * @return 按 BM25 分数降序的 Document 列表（含文本，供后续重排）
     */
    public List<Document> retrieve(String query, Long knowledgeId, Long tenantId, int topK) {
        String sql = "SELECT id, content FROM " + vectorTableName
                + " WHERE metadata->>'knowledge_id' = ? AND metadata->>'tenant_id' = ?";
        List<Chunk> corpus;
        try {
            corpus = jdbcTemplate.query(sql, (rs, i) -> {
                String id = rs.getString("id");
                String content = rs.getString("content");
                return new Chunk(id, content);
            }, String.valueOf(knowledgeId), String.valueOf(tenantId));
        } catch (Exception e) {
            // 向量底表尚未建好或查询异常时，词法路径降级为空（不影响向量主路径）
            return Collections.emptyList();
        }
        if (corpus == null || corpus.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建词频与文档长度
        Map<String, Integer> docFreq = new HashMap<>();
        List<Map<String, Integer>> docTermFreqs = new ArrayList<>(corpus.size());
        List<Integer> docLens = new ArrayList<>(corpus.size());
        for (Chunk chunk : corpus) {
            Map<String, Integer> tf = new HashMap<>();
            for (String tok : tokenize(chunk.content)) {
                tf.merge(tok, 1, Integer::sum);
            }
            docTermFreqs.add(tf);
            docLens.add(tf.values().stream().mapToInt(Integer::intValue).sum());
            for (String tok : tf.keySet()) {
                docFreq.merge(tok, 1, Integer::sum);
            }
        }
        double avgdl = docLens.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int n = corpus.size();

        // 计算查询词的 BM25 分数
        Map<String, Double> queryTokens = new LinkedHashMap<>();
        for (String tok : tokenize(query)) {
            queryTokens.merge(tok, 1.0, Double::sum);
        }

        List<ScoredChunk> scored = new ArrayList<>(corpus.size());
        for (int i = 0; i < corpus.size(); i++) {
            double score = 0.0;
            int dl = docLens.get(i);
            Map<String, Integer> tf = docTermFreqs.get(i);
            for (Map.Entry<String, Double> qt : queryTokens.entrySet()) {
                Integer f = tf.get(qt.getKey());
                if (f == null) {
                    continue;
                }
                int df = docFreq.getOrDefault(qt.getKey(), 0);
                double idf = Math.log((n - df + 0.5) / (df + 0.5) + 1.0);
                double numerator = f * (K1 + 1);
                double denominator = f + K1 * (1 - B + B * dl / (avgdl <= 0 ? 1 : avgdl));
                score += idf * (numerator / denominator);
            }
            if (score > 0.0) {
                scored.add(new ScoredChunk(corpus.get(i).id, corpus.get(i).content, score));
            }
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        int limit = Math.min(topK, scored.size());
        List<Document> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            ScoredChunk sc = scored.get(i);
            result.add(Document.builder().id(sc.id).text(sc.content).build());
        }
        return result;
    }

    /**
     * 中英文混合分词：英文 / 数字按词，CJK 按字。
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        String lower = text.toLowerCase();
        List<String> tokens = new ArrayList<>();
        Matcher en = EN_PATTERN.matcher(lower);
        while (en.find()) {
            tokens.add(en.group());
        }
        Matcher cjk = CJK_PATTERN.matcher(lower);
        while (cjk.find()) {
            tokens.add(cjk.group());
        }
        return tokens;
    }

    private static final class Chunk {
        final String id;
        final String content;

        Chunk(String id, String content) {
            this.id = id;
            this.content = content;
        }
    }

    private static final class ScoredChunk {
        final String id;
        final String content;
        final double score;

        ScoredChunk(String id, String content, double score) {
            this.id = id;
            this.content = content;
            this.score = score;
        }
    }
}
