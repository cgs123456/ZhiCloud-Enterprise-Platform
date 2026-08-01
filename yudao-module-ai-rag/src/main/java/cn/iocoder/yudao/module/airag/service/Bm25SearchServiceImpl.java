package cn.iocoder.yudao.module.airag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * BM25 检索 Service 实现类
 *
 * <p>基于内存 BM25 索引实现，算法参数：
 * <ul>
 *   <li>k1 = 1.2：词频饱和参数，控制词频对分数的影响</li>
 *   <li>b = 0.75：文档长度归一化参数</li>
 * </ul>
 *
 * <p>BM25 打分公式：
 * <pre>
 *   score(D, Q) = Σ IDF(t) * (f(t,D) * (k1+1)) / (f(t,D) + k1*(1-b+b*|D|/avgdl))
 *   IDF(t) = log((N - n(t) + 0.5) / (n(t) + 0.5) + 1)
 * </pre>
 *
 * <p>通过 {@code yudao.airag.hybrid-search.enabled=true} 控制是否启用。
 *
 * @author yudao
 */
@Service
@ConditionalOnProperty(prefix = "yudao.airag.hybrid-search", name = "enabled", havingValue = "true")
@Slf4j
public class Bm25SearchServiceImpl implements Bm25SearchService {

    /**
     * BM25 词频饱和参数 k1
     */
    private static final double K1 = 1.2;
    /**
     * BM25 文档长度归一化参数 b
     */
    private static final double B = 0.75;

    /**
     * 内存索引：文档 ID → 分词后的词频表
     */
    private final Map<String, IndexedDoc> index = new ConcurrentHashMap<>();
    /**
     * 全局统计：词 → 包含该词的文档数（用于 IDF）
     */
    private final Map<String, Integer> docFrequency = new ConcurrentHashMap<>();
    /**
     * 所有文档的平均长度（token 数）
     */
    private volatile double avgDocLength = 0.0;

    @Override
    public List<Document> search(String query, int topK) {
        return search(query, new ArrayList<>(index.values()
                .stream().map(IndexedDoc::getDocument).toList()), topK);
    }

    @Override
    public List<Document> search(String query, List<Document> documents, int topK) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        if (query == null || query.isBlank()) {
            return documents.stream().limit(topK).collect(Collectors.toList());
        }

        // 1. query 分词
        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return documents.stream().limit(topK).collect(Collectors.toList());
        }

        // 2. 对给定文档集合构建临时索引统计
        int n = documents.size();
        // 词 → 包含该词的文档数
        Map<String, Integer> df = new HashMap<>();
        // 文档 → 词频表
        List<Map<String, Integer>> docTermFreqs = new ArrayList<>(n);
        // 文档 → 长度
        int[] docLengths = new int[n];
        double totalLength = 0;
        for (int i = 0; i < n; i++) {
            List<String> tokens = tokenize(documents.get(i).getText());
            docLengths[i] = tokens.size();
            totalLength += tokens.size();
            Map<String, Integer> tf = new HashMap<>();
            for (String token : tokens) {
                tf.merge(token, 1, Integer::sum);
            }
            docTermFreqs.add(tf);
            for (String token : queryTokens) {
                if (tf.containsKey(token)) {
                    df.merge(token, 1, Integer::sum);
                }
            }
        }
        double avgdl = totalLength / n;

        // 3. 计算 IDF 并打分
        Map<String, Double> idf = new HashMap<>();
        for (String token : queryTokens) {
            int docsWithTerm = df.getOrDefault(token, 0);
            double idfValue = Math.log((double) (n - docsWithTerm + 0.5) / (docsWithTerm + 0.5) + 1.0);
            idf.put(token, idfValue);
        }

        List<ScoredDoc> scoredDocs = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Map<String, Integer> tf = docTermFreqs.get(i);
            double score = 0.0;
            for (String token : queryTokens) {
                Integer freq = tf.get(token);
                if (freq == null || freq == 0) {
                    continue;
                }
                double idfValue = idf.getOrDefault(token, 0.0);
                double denom = freq + K1 * (1 - B + B * (docLengths[i] / avgdl));
                score += idfValue * (freq * (K1 + 1)) / denom;
            }

            Document scored = Document.builder()
                    .id(documents.get(i).getId())
                    .text(documents.get(i).getText())
                    .metadata(documents.get(i).getMetadata())
                    .score(score)
                    .build();
            scoredDocs.add(new ScoredDoc(scored, score));
        }

        // 4. 按分数降序，取 topK
        scoredDocs.sort((a, b) -> Double.compare(b.score, a.score));
        return scoredDocs.stream().limit(topK)
                .map(sd -> sd.document)
                .collect(Collectors.toList());
    }

    @Override
    public void index(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return;
        }
        for (Document doc : documents) {
            List<String> tokens = tokenize(doc.getText());
            Map<String, Integer> tf = new HashMap<>();
            for (String token : tokens) {
                tf.merge(token, 1, Integer::sum);
            }
            IndexedDoc indexed = new IndexedDoc(doc, tf, tokens.size());
            index.put(doc.getId(), indexed);
            // 更新全局 docFrequency
            for (String token : tf.keySet()) {
                docFrequency.merge(token, 1, Integer::sum);
            }
        }
        recalcAvgDocLength();
        log.debug("[index][已索引 {} 篇文档，总计 {} 篇]", documents.size(), index.size());
    }

    @Override
    public void clear() {
        index.clear();
        docFrequency.clear();
        avgDocLength = 0.0;
        log.info("[clear][已清空 BM25 索引]");
    }

    /**
     * 重新计算平均文档长度
     */
    private void recalcAvgDocLength() {
        if (index.isEmpty()) {
            avgDocLength = 0.0;
            return;
        }
        long total = index.values().stream().mapToLong(IndexedDoc::getLength).sum();
        avgDocLength = (double) total / index.size();
    }

    /**
     * 中英文分词
     * <ul>
     *   <li>英文：按非字母数字切分，转小写</li>
     *   <li>中文：按单字切分（unigram，BM25 中单字匹配更稳定）</li>
     * </ul>
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        // 英文：按非字母数字切分
        String[] words = text.toLowerCase().split("[^a-z0-9]+");
        for (String word : words) {
            if (word.length() >= 1 && !word.isBlank()) {
                tokens.add(word);
            }
        }
        // 中文：单字切分
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                tokens.add(String.valueOf(c));
            }
        }
        return tokens;
    }

    /**
     * 索引文档
     */
    private static class IndexedDoc {
        final Document document;
        final Map<String, Integer> termFreq;
        final int length;

        IndexedDoc(Document document, Map<String, Integer> termFreq, int length) {
            this.document = document;
            this.termFreq = termFreq;
            this.length = length;
        }

        Document getDocument() {
            return document;
        }

        int getLength() {
            return length;
        }
    }

    /**
     * 带分数的文档包装类
     */
    private static class ScoredDoc {
        final Document document;
        final double score;

        ScoredDoc(Document document, double score) {
            this.document = document;
            this.score = score;
        }
    }

}
