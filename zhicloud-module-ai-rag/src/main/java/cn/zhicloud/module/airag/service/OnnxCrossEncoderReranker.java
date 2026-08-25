package cn.zhicloud.module.airag.service;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OnnxValue;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

import java.util.*;

/**
 * ONNX Cross-Encoder 重排序器（BGE-reranker-base）
 *
 * <p>基于 BGE-reranker-base 模型（Cross-Encoder 架构），对 (query, document) pair 进行打分：
 * <ol>
 *   <li>使用 DJL HuggingFaceTokenizer 对 query+document 编码（含 input_ids、attention_mask、token_type_ids）</li>
 *   <li>通过 ONNX Runtime 执行 Cross-Encoder 推理，输出单个相关性 logit</li>
 *   <li>按 logit 降序排序，取 topK</li>
 * </ol>
 *
 * <p>模型文件需预先下载到 {@code classpath:airag/{model-path}/} 目录下，包含：
 * {@code model.onnx} 与 {@code tokenizer.json}。
 *
 * <p>本类为可选组件，通过 {@link #tryLoad(String)} 进行懒加载，任一依赖缺失或模型文件不存在时返回 null，
 * 由上层 fallback 到 {@link SimpleReranker}。
 *
 * @author zhicloud
 */
@Slf4j
public class OnnxCrossEncoderReranker {

    /**
     * BGE-reranker-base 最大 token 长度（query+document 总和）
     */
    private static final int MAX_SEQ_LENGTH = 512;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final HuggingFaceTokenizer tokenizer;

    private OnnxCrossEncoderReranker(OrtEnvironment environment, OrtSession session, HuggingFaceTokenizer tokenizer) {
        this.environment = environment;
        this.session = session;
        this.tokenizer = tokenizer;
    }

    /**
     * 尝试加载 ONNX Cross-Encoder 模型
     *
     * @param modelPath 模型目录名（classpath:airag/{model-path}/）
     * @return 加载成功返回实例，失败返回 null
     */
    public static OnnxCrossEncoderReranker tryLoad(String modelPath) {
        try {
            String baseDir = "airag/" + modelPath;
            ClassPathResource modelRes = new ClassPathResource(baseDir + "/model.onnx");
            ClassPathResource tokenizerRes = new ClassPathResource(baseDir + "/tokenizer.json");
            if (!modelRes.exists() || !tokenizerRes.exists()) {
                log.info("[OnnxCrossEncoderReranker][模型文件不存在 model-path={}，跳过 ONNX 加载]", modelPath);
                return null;
            }

            OrtEnvironment env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            // 读取模型字节，避免 Windows 路径问题
            byte[] modelBytes = modelRes.getInputStream().readAllBytes();
            OrtSession session = env.createSession(modelBytes, opts);

            Map<String, String> tokenizerOpts = new HashMap<>();
            tokenizerOpts.put("maxLength", String.valueOf(MAX_SEQ_LENGTH));
            tokenizerOpts.put("padding", "true");
            tokenizerOpts.put("truncation", "true");
            HuggingFaceTokenizer tokenizer = HuggingFaceTokenizer.newInstance(
                    tokenizerRes.getInputStream(), tokenizerOpts);

            log.info("[OnnxCrossEncoderReranker][加载成功 model-path={}]", modelPath);
            return new OnnxCrossEncoderReranker(env, session, tokenizer);
        } catch (Throwable t) {
            log.info("[OnnxCrossEncoderReranker][加载失败 model-path={}，原因：{}]", modelPath, t.toString());
            return null;
        }
    }

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

        // 1. 对每个 (query, document) pair 编码并打分
        List<ScoredDoc> scoredDocs = new ArrayList<>(documents.size());
        for (Document doc : documents) {
            float score = scorePair(query, doc.getText());
            scoredDocs.add(new ScoredDoc(doc, score));
        }

        // 2. 按分数降序，取 topK
        scoredDocs.sort((a, b) -> Float.compare(b.score, a.score));

        List<Document> result = new ArrayList<>(Math.min(topK, scoredDocs.size()));
        for (ScoredDoc sd : scoredDocs) {
            if (result.size() >= topK) {
                break;
            }
            Document scored = Document.builder()
                    .id(sd.doc.getId())
                    .text(sd.doc.getText())
                    .metadata(sd.doc.getMetadata())
                    .score((double) sd.score)
                    .build();
            result.add(scored);
        }
        return result;
    }

    /**
     * 对单个 (query, document) pair 计算 Cross-Encoder 相关性分数
     */
    private float scorePair(String query, String document) {
        // 截断超长文档，避免推理过慢
        String docText = truncateText(document, 1000);
        try {
            // 1. 编码 query+document pair
            ai.djl.huggingface.tokenizers.Encoding encoding = tokenizer.encode(query, docText);
            long[] inputIds = encoding.getIds();
            long[] attentionMask = encoding.getAttentionMask();
            long[] tokenTypeIds = encoding.getTypeIds();

            // 2. 构造 ONNX 输入张量
            long[] shape = {1, inputIds.length};
            try (OnnxTensor inputIdsTensor = OnnxTensor.createTensor(environment, java.nio.LongBuffer.wrap(inputIds), shape);
                 OnnxTensor attentionTensor = OnnxTensor.createTensor(environment, java.nio.LongBuffer.wrap(attentionMask), shape);
                 OnnxTensor tokenTypeTensor = OnnxTensor.createTensor(environment, java.nio.LongBuffer.wrap(tokenTypeIds), shape)) {

                Map<String, OnnxTensor> inputs = new HashMap<>();
                inputs.put("input_ids", inputIdsTensor);
                inputs.put("attention_mask", attentionTensor);
                inputs.put("token_type_ids", tokenTypeTensor);

                // 3. 执行推理
                try (OrtSession.Result output = session.run(inputs)) {
                    // BGE-reranker 输出 logits [1,1]，取第一个值作为分数
                    for (Map.Entry<String, OnnxValue> entry : output) {
                        Object value = entry.getValue().getValue();
                        if (value instanceof float[] floats && floats.length > 0) {
                            return floats[0];
                        }
                        if (value instanceof float[][] mat && mat.length > 0 && mat[0].length > 0) {
                            return mat[0][0];
                        }
                    }
                }
            }
        } catch (Throwable t) {
            log.warn("[scorePair][ONNX 推理异常 err={}]", t.toString());
        }
        return 0f;
    }

    /**
     * 截断文本到指定字符数
     */
    private String truncateText(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        return text.length() > maxChars ? text.substring(0, maxChars) : text;
    }

    /**
     * 带分数的文档包装类
     */
    private static class ScoredDoc {
        final Document doc;
        final float score;

        ScoredDoc(Document doc, float score) {
            this.doc = doc;
            this.score = score;
        }
    }

}
