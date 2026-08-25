package cn.zhicloud.module.airag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 简单重排序器（fallback）
 *
 * <p>当 ONNX BGE-reranker 模型不可用时使用。基于 TF-IDF + query 覆盖率打分：
 * <ul>
 *   <li>计算 query 中每个词的 IDF 权重（基于当前召回的文档集合）</li>
 *   <li>文档与 query 的词重叠率 × IDF 加权，得到最终相关度分数</li>
 * </ul>
 *
 * <p>虽然精度不及 Cross-Encoder，但可在无模型依赖下保证 Reranker 链路可用。
 *
 * @author zhicloud
 */
@Slf4j
public class SimpleReranker {

    /**
     * 默认的中文停用词（避免无意义的高频词影响打分）
     */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "和", "是", "在", "我", "有", "也", "就", "不", "都", "与", "及", "或",
            "the", "a", "an", "is", "are", "of", "and", "or", "in", "on", "to", "for"
    );

    /**
     * 对文档列表重排序
     *
     * @param query     用户查询
     * @param documents 待排序文档
     * @param topK      返回数量
     * @return 按 score 降序的 topK 文档
     */
    public List<Document> rerank(String query, List<Document> documents, int topK) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        if (query == null || query.isBlank()) {
            return documents.stream().limit(topK).collect(Collectors.toList());
        }

        // 1. 分词
        Set<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) {
            return documents.stream().limit(topK).collect(Collectors.toList());
        }

        // 2. 统计每个 query 词在多少篇文档中出现过（用于 IDF）
        Map<String, Integer> docFreq = new HashMap<>();
        List<Set<String>> docTokenSets = new ArrayList<>(documents.size());
        for (Document doc : documents) {
            Set<String> tokens = tokenize(doc.getText());
            docTokenSets.add(tokens);
            for (String token : queryTokens) {
                if (tokens.contains(token)) {
                    docFreq.merge(token, 1, Integer::sum);
                }
            }
        }

        // 3. 计算 IDF：idf = log(1 + N / (1 + df))
        int totalDocs = documents.size();
        Map<String, Double> idf = new HashMap<>();
        for (String token : queryTokens) {
            int df = docFreq.getOrDefault(token, 0);
            idf.put(token, Math.log(1.0 + (double) totalDocs / (1.0 + df)));
        }

        // 4. 对每篇文档计算分数：query 覆盖率 × IDF 加权
        List<Document> scoredDocs = new ArrayList<>(documents.size());
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Set<String> docTokens = docTokenSets.get(i);

            double score = 0.0;
            int matched = 0;
            for (String token : queryTokens) {
                if (docTokens.contains(token)) {
                    score += idf.getOrDefault(token, 0.0);
                    matched++;
                }
            }
            // 覆盖率：命中的 query 词占比，归一化到 [0,1]
            double coverage = (double) matched / queryTokens.size();
            // 最终分数 = 覆盖率 × 0.5 + IDF 累积分 × 0.5（简单加权，保证分数在合理区间）
            double finalScore = coverage * 0.5 + normalize(score) * 0.5;

            // 复制文档并更新 score（避免污染原对象）
            Document scored = Document.builder()
                    .id(doc.getId())
                    .text(doc.getText())
                    .metadata(doc.getMetadata())
                    .score(finalScore)
                    .build();
            scoredDocs.add(scored);
        }

        // 5. 按分数降序，取 topK
        scoredDocs.sort((a, b) -> Double.compare(
                b.getScore() != null ? b.getScore() : 0.0,
                a.getScore() != null ? a.getScore() : 0.0));
        return scoredDocs.stream().limit(topK).collect(Collectors.toList());
    }

    /**
     * 简单的中英文分词：
     * <ul>
     *   <li>英文：按非字母数字字符切分，转小写</li>
     *   <li>中文：按单字切分（bigram 提升语义匹配）</li>
     *   <li>过滤停用词与过短 token</li>
     * </ul>
     */
    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> tokens = new HashSet<>();
        // 英文：按非字母数字切分
        String[] words = text.toLowerCase().split("[^a-z0-9]+");
        for (String word : words) {
            if (word.length() >= 2 && !STOP_WORDS.contains(word)) {
                tokens.add(word);
            }
        }
        // 中文：单字 + bigram（相邻两字组合）
        StringBuilder chinese = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 0x4E00 && c <= 0x9FFF) {
                chinese.append(c);
            }
        }
        String zh = chinese.toString();
        for (int i = 0; i < zh.length(); i++) {
            String single = String.valueOf(zh.charAt(i));
            if (!STOP_WORDS.contains(single)) {
                tokens.add(single);
            }
        }
        for (int i = 0; i < zh.length() - 1; i++) {
            tokens.add(zh.substring(i, i + 2));
        }
        return tokens;
    }

    /**
     * 将 IDF 累积分归一化到 [0,1]（避免极端值）
     */
    private double normalize(double score) {
        if (score <= 0) {
            return 0.0;
        }
        // 使用 sigmoid 把分数压缩到 (0,1)
        return 1.0 / (1.0 + Math.exp(-score));
    }

}
