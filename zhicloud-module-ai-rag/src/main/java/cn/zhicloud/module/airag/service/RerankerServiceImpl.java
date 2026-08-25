package cn.zhicloud.module.airag.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

/**
 * Reranker Service 实现类
 *
 * <p>实现策略：
 * <ol>
 *   <li>启动时尝试加载 BGE-reranker-base（ONNX Cross-Encoder）</li>
 *   <li>模型可用 → 调用 ONNX 对 query+document 打分，按分数排序取 topK</li>
 *   <li>模型不可用且 {@code fallback-to-simple=true} → 使用 {@link SimpleReranker}（TF-IDF + query 覆盖率）</li>
 * </ol>
 *
 * <p>通过 {@code zhicloud.airag.reranker.enabled=true} 控制是否启用（Bean 在 {@code AiragConfiguration} 中注册）。
 *
 * @author zhicloud
 */
@Slf4j
public class RerankerServiceImpl implements RerankerService {

    /**
     * ONNX Cross-Encoder 重排序器（懒加载，加载失败则为 null）
     */
    private volatile OnnxCrossEncoderReranker crossEncoderReranker;

    /**
     * fallback 简单重排序器
     */
    private final SimpleReranker simpleReranker = new SimpleReranker();

    @Value("${zhicloud.airag.reranker.model-path:bge-reranker-base}")
    private String modelPath;

    @Value("${zhicloud.airag.reranker.fallback-to-simple:true}")
    private boolean fallbackToSimple;

    @PostConstruct
    public void init() {
        try {
            this.crossEncoderReranker = OnnxCrossEncoderReranker.tryLoad(modelPath);
            if (this.crossEncoderReranker != null) {
                log.info("[RerankerServiceImpl][已加载 ONNX Cross-Encoder 重排序模型 model-path={}]", modelPath);
            } else {
                log.warn("[RerankerServiceImpl][ONNX 重排序模型未就绪 model-path={}，fallback-to-simple={}]",
                        modelPath, fallbackToSimple);
            }
        } catch (Throwable t) {
            log.warn("[RerankerServiceImpl][加载 ONNX 重排序模型失败，将使用 fallback err={}]", t.toString());
            this.crossEncoderReranker = null;
        }
    }

    @Override
    public List<Document> rerank(String query, List<Document> documents, int topK) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }
        if (topK <= 0) {
            topK = documents.size();
        }

        // 1. 优先使用 ONNX Cross-Encoder
        if (crossEncoderReranker != null) {
            try {
                List<Document> result = crossEncoderReranker.rerank(query, documents, topK);
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            } catch (Throwable t) {
                log.warn("[rerank][ONNX 重排序异常，降级到 SimpleReranker err={}]", t.toString());
            }
        }

        // 2. fallback 到 SimpleReranker
        if (fallbackToSimple) {
            return simpleReranker.rerank(query, documents, topK);
        }

        // 3. 不允许 fallback，按原顺序取 topK
        return documents.stream().limit(topK).toList();
    }

}
