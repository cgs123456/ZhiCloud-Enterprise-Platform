package cn.zhicloud.module.airag.service.rag;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.ai.dal.dataobject.model.AiModelDO;
import cn.zhicloud.module.ai.enums.model.AiModelTypeEnum;
import cn.zhicloud.module.ai.service.model.AiModelService;
import cn.zhicloud.module.airag.dal.dataobject.AiragDocumentDO;
import cn.zhicloud.module.airag.dal.dataobject.AiragKnowledgeDO;
import cn.zhicloud.module.airag.dal.mysql.AiragDocumentMapper;
import cn.zhicloud.module.airag.service.RerankerService;
import cn.zhicloud.module.airag.service.knowledge.AiragKnowledgeService;
import cn.zhicloud.module.airag.util.UrlSafetyUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.airag.enums.ErrorCodeConstants.*;

/**
 * AI RAG 核心 Service 实现类
 *
 * 基于 Spring AI 实现：Tika 文档解析 + 语义分块 + PgVectorStore 向量存储（BM25 + 向量双路召回）+ Cross-Encoder 重排 + ChatClient 回答生成。
 *
 * 向量库相关 Bean 通过 {@code zhicloud.airag.enabled=true} 控制，未启用时相关方法会抛出明确异常。
 *
 * @author zhicloud
 */
@Service
@Slf4j
public class AiragRagServiceImpl implements AiragRagService {

    /**
     * 文档处理状态
     */
    private static final int STATUS_PROCESSING = 1;
    private static final int STATUS_DONE = 2;
    private static final int STATUS_FAILED = 3;

    /**
     * 检索 topK
     */
    private static final int SEARCH_TOP_K = 4;
    /**
     * 相似度阈值
     */
    private static final double SEARCH_SIMILARITY_THRESHOLD = 0.5;

    /**
     * 向量语义路径召回 topK
     */
    private static final int VECTOR_TOP_K = 10;
    /**
     * BM25 词法路径召回 topK
     */
    private static final int BM25_TOP_K = 10;
    /**
     * RRF 融合后保留的候选数（送重排前）
     */
    private static final int FUSE_TOP_K = 8;
    /**
     * RRF 常数 k
     */
    private static final int RRF_K = 60;
    /**
     * 语义分块：单块目标字符数上限
     */
    private static final int CHUNK_MAX_CHARS = 2000;
    /**
     * 语义分块：相邻块重叠字符数（上下文衔接）
     */
    private static final int CHUNK_OVERLAP_CHARS = 200;

    /**
     * 文档分块 ID 前缀，用于向量库删除时按 documentId 定位
     */
    private static final String CHUNK_ID_PREFIX = "airag_doc_";

    @Resource
    private AiragDocumentMapper documentMapper;

    @Resource
    private AiragKnowledgeService knowledgeService;

    /**
     * 向量库 Bean，由 {@link cn.zhicloud.module.airag.config.AiragConfiguration} 条件加载。
     * 未启用时为 null，相关方法需做空判断。
     */
    @Autowired(required = false)
    private VectorStore vectorStore;

    /**
     * zhicloud-module-ai 的模型服务，用于获取默认 ChatModel。
     * zhicloud-server 同时引入 zhicloud-module-ai 与本模块时可用。
     */
    @Autowired(required = false)
    private AiModelService aiModelService;

    /**
     * 可选重排序服务（Reranker）。
     *
     * <p>由 {@link cn.zhicloud.module.airag.config.AiragConfiguration} 条件加载，
     * 仅当 {@code zhicloud.airag.reranker.enabled=true} 时存在；未启用时为 null，检索结果保持向量召回原序。
     */
    @Autowired(required = false)
    private RerankerService rerankerService;

    /**
     * 可选 BM25 词法检索器（混合召回的词法路径）。
     *
     * <p>由 {@link cn.zhicloud.module.airag.config.AiragConfiguration} 条件加载，
     * 仅当 {@code zhicloud.airag.enabled=true}（向量底表可用）时存在；未启用时为 null，
     * 检索退化为纯向量单路。
     */
    @Autowired(required = false)
    private Bm25LexicalRetriever bm25Retriever;

    @Override
    public void importDocument(Long knowledgeId, Long documentId) {
        // 1. 校验向量库可用
        validateVectorStoreAvailable();
        // 2. 获取文档与知识库信息
        AiragDocumentDO document = documentMapper.selectById(documentId);
        if (document == null) {
            throw exception(DOCUMENT_NOT_EXISTS);
        }
        AiragKnowledgeDO knowledge = knowledgeService.validateKnowledgeExists(knowledgeId);

        // 3. 标记为处理中
        documentMapper.updateById(new AiragDocumentDO()
                .setId(documentId)
                .setStatus(STATUS_PROCESSING));

        try {
            // 4. 下载文件（下载前做 SSRF 校验：仅允许公网 http/https 地址，防内网探测与云元数据泄露）
            UrlSafetyUtils.validateSafeUrl(document.getUrl());
            byte[] bytes = HttpUtil.downloadBytes(document.getUrl());
            if (bytes == null || bytes.length == 0) {
                throw exception(DOCUMENT_FILE_EMPTY);
            }

            // 5. Tika 解析
            TikaDocumentReader reader = new TikaDocumentReader(new ByteArrayResource(bytes));
            List<Document> rawDocuments = reader.get();
            if (CollUtil.isEmpty(rawDocuments)) {
                throw exception(DOCUMENT_FILE_READ_FAIL);
            }

            // 6. 语义分块（按段落 / 标题边界切分，按字符预算合并并保留重叠）
            List<Document> chunks = semanticChunk(rawDocuments);
            if (CollUtil.isEmpty(chunks)) {
                throw exception(DOCUMENT_FILE_READ_FAIL);
            }

            // 7. 为每个分块注入元数据 + 确定性 ID（便于按 documentId 删除）
            // P0-2 引用溯源：额外注入 chunk_id、chunk_index，供 RagAdvisor 构造引用列表
            // P0-2 安全修复：tenant_id 必须写入向量库 metadata，供 chat 检索时复合过滤
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            List<Document> enrichedChunks = new ArrayList<>(chunks.size());
            for (int i = 0; i < chunks.size(); i++) {
                Document chunk = chunks.get(i);
                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                String chunkId = CHUNK_ID_PREFIX + documentId + "_" + i;
                metadata.put("knowledge_id", knowledgeId);
                metadata.put("document_id", documentId);
                metadata.put("document_name", document.getName());
                metadata.put("knowledge_name", knowledge.getName());
                metadata.put("chunk_id", chunkId);
                metadata.put("chunk_index", i);
                metadata.put("tenant_id", tenantId);
                // page：Tika 解析得到的 page（如有），否则使用 chunk_index 作为伪页码
                metadata.putIfAbsent("page", i);
                enrichedChunks.add(Document.builder()
                        .id(chunkId)
                        .text(chunk.getText())
                        .metadata(metadata)
                        .build());
            }

            // 8. 写入向量库（PgVectorStore 内部会调用 EmbeddingModel 生成向量）
            vectorStore.add(enrichedChunks);

            // 9. 更新文档状态为已完成
            documentMapper.updateById(new AiragDocumentDO()
                    .setId(documentId)
                    .setStatus(STATUS_DONE)
                    .setChunkCount(enrichedChunks.size())
                    .setErrorMsg(null));
            log.info("[importDocument][文档向量化完成，documentId={}, chunkCount={}]",
                    documentId, enrichedChunks.size());
        } catch (Exception e) {
            log.error("[importDocument][文档向量化失败，documentId={}]", documentId, e);
            documentMapper.updateById(new AiragDocumentDO()
                    .setId(documentId)
                    .setStatus(STATUS_FAILED)
                    .setErrorMsg(StrUtil.sub(e.getMessage(), 0, 500)));
            throw e;
        }
    }

    @Override
    public String chat(Long knowledgeId, String question) {
        // 1. 双路召回 + 构造带检索上下文的系统提示
        String systemPrompt = buildRagSystemPrompt(knowledgeId, question);
        // 2. 调用 ChatClient 同步生成回答
        ChatModel chatModel = getChatModel();
        return ChatClient.builder(chatModel)
                .build()
                .prompt()
                .system(systemPrompt)
                .user(question)
                .call()
                .content();
    }

    @Override
    public Flux<String> chatStream(Long knowledgeId, String question) {
        // 1. 与同步 chat 走同一检索与提示构造链路
        String systemPrompt = buildRagSystemPrompt(knowledgeId, question);
        // 2. 调用 ChatClient 流式生成回答（同一 advisor 链，仅 call → stream）
        ChatModel chatModel = getChatModel();
        return ChatClient.builder(chatModel)
                .build()
                .prompt()
                .system(systemPrompt)
                .user(question)
                .stream()
                .content();
    }

    /**
     * 执行「向量 + BM25 双路召回 + 可选重排」检索，并构造带引用上下文的系统提示。
     *
     * <p>供 {@link #chat(Long, String)} 与 {@link #chatStream(Long, String)} 复用，
     * 保证同步与流式接口的检索行为一致。
     */
    private String buildRagSystemPrompt(Long knowledgeId, String question) {
        // 1. 校验向量库可用
        validateVectorStoreAvailable();
        // 2. 校验知识库存在（DB 层多租户隔离已拦截跨租户访问）
        knowledgeService.validateKnowledgeExists(knowledgeId);

        // 3. 向量语义路径（P0-2 修复：必须同时按 knowledge_id + tenant_id 复合过滤，防止跨租户数据泄漏）
        //    向量库（PgVectorStore）不受 MyBatis Plus 多租户拦截器保护，必须在应用层显式过滤
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        Filter.Expression filter = builder.and(
                builder.eq("knowledge_id", knowledgeId),
                builder.eq("tenant_id", tenantId)
        ).build();
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(VECTOR_TOP_K)
                .similarityThreshold(SEARCH_SIMILARITY_THRESHOLD)
                .filterExpression(filter)
                .build();
        List<Document> vectorDocs = vectorStore.similaritySearch(request);
        log.info("[chat][向量检索完成，knowledgeId={}, tenantId={}, 命中 {} 条]", knowledgeId, tenantId, CollUtil.size(vectorDocs));

        // 4. BM25 词法路径（与向量路径独立）+ RRF 融合，构成「BM25 + 向量双路召回」
        List<Document> documents = fuseHybrid(question, knowledgeId, tenantId, vectorDocs);
        log.info("[chat][双路召回融合后 {} 条]", CollUtil.size(documents));

        // 5. 可选重排序：若启用了 Reranker，对融合结果二次排序以提升注入上下文精度
        if (rerankerService != null && CollUtil.isNotEmpty(documents)) {
            documents = rerankerService.rerank(question, documents, SEARCH_TOP_K);
            log.info("[chat][已执行 Reranker 重排序，重排后 {} 条]", CollUtil.size(documents));
        }

        // 6. 拼接上下文（P0-2 引用溯源：每个 chunk 前加 [N] 编号 + 文档名+页码+chunk_id）
        String context = CollUtil.isEmpty(documents) ? "" : buildCitationContext(documents);

        // 7. 构造系统提示 + 用户问题
        return buildSystemPrompt(context);
    }

    @Override
    public void deleteDocument(Long documentId) {
        if (vectorStore == null) {
            log.info("[deleteDocument][向量库未启用，跳过删除，documentId={}]", documentId);
            return;
        }
        // 1. 查询文档，获取分块数量（DB 层多租户隔离已拦截跨租户访问）
        AiragDocumentDO document = documentMapper.selectById(documentId);
        if (document == null) {
            return;
        }
        Integer chunkCount = document.getChunkCount();
        if (chunkCount == null || chunkCount <= 0) {
            log.info("[deleteDocument][文档无分块记录，跳过向量库删除，documentId={}]", documentId);
            return;
        }
        // 2. 构造 chunk ID 列表（与 importDocument 中的确定性 ID 规则保持一致）
        List<String> chunkIds = new ArrayList<>(chunkCount);
        for (int i = 0; i < chunkCount; i++) {
            chunkIds.add(CHUNK_ID_PREFIX + documentId + "_" + i);
        }
        // 3. 删除（P0-2 修复：记录 tenantId 用于审计日志）
        Long tenantId = TenantContextHolder.getTenantId();
        try {
            vectorStore.delete(chunkIds);
            log.info("[deleteDocument][向量库删除完成，documentId={}, tenantId={}, chunkCount={}]", documentId, tenantId, chunkCount);
        } catch (Exception e) {
            // 一致性修复：此处原先仅 log.warn 吞掉异常，导致「向量没删掉但调用方以为删成功」，
            // 上层随即删除文档 DB 记录 => 文档在后台消失、向量却永久残留在检索库里，
            // 表现为「已删除的文档仍被 RAG 召回」，既是数据残留也是合规风险，且此后再无入口可清理
            //（因为清理逻辑依赖 DB 里的 chunkCount，记录一删就彻底失去线索）。
            // 改为向上抛出：调用方据此保留 DB 记录，用户可重试；chunkId 由 documentId 确定性生成，
            // 重复删除幂等，因此重试路径最终一定收敛。
            log.error("[deleteDocument][向量库删除失败，documentId={}, tenantId={}，原因={}]",
                    documentId, tenantId, e.getMessage(), e);
            throw exception(RAG_VECTOR_DELETE_FAIL);
        }
    }

    /**
     * 语义 / 结构感知分块：先按段落 / 标题等自然边界切分，再按字符预算合并并保留重叠。
     *
     * <p>相比纯 TokenTextSplitter 的等距硬切，本方法尊重文档语义边界（段落、空行），
     * 避免将一个完整语义单元从中间截断，提升后续 Embedding 与召回质量。
     */
    private List<Document> semanticChunk(List<Document> rawDocuments) {
        StringBuilder sb = new StringBuilder();
        for (Document d : rawDocuments) {
            if (d.getText() != null) {
                sb.append(d.getText()).append("\n\n");
            }
        }
        String text = sb.toString();
        // 按空行 / 标题边界切分为段落块
        String[] blocks = text.split("\\n\\s*\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String b : blocks) {
            String trimmed = b.trim();
            if (!trimmed.isEmpty()) {
                paragraphs.add(trimmed);
            }
        }
        List<Document> chunks = new ArrayList<>();
        if (paragraphs.isEmpty()) {
            if (!text.trim().isEmpty()) {
                chunks.add(new Document(text.trim()));
            }
            return chunks;
        }
        StringBuilder cur = new StringBuilder();
        for (String p : paragraphs) {
            if (cur.length() > 0 && cur.length() + p.length() + 2 > CHUNK_MAX_CHARS) {
                chunks.add(new Document(cur.toString().trim()));
                // 重叠：保留尾部若干字符作为上下文衔接
                String curStr = cur.toString();
                int start = Math.max(0, curStr.length() - CHUNK_OVERLAP_CHARS);
                cur = new StringBuilder(curStr.substring(start).trim());
            }
            if (cur.length() > 0) {
                cur.append("\n\n");
            }
            cur.append(p);
        }
        if (cur.length() > 0) {
            chunks.add(new Document(cur.toString().trim()));
        }
        return chunks;
    }

    /**
     * 双路召回融合：向量语义路径 + BM25 词法路径，使用 Reciprocal Rank Fusion（RRF）合并。
     *
     * <p>RRF 仅依赖各路径的排名、无需对向量相似度与 BM25 分数做归一化，
     * 对异构分数天然鲁棒；融合后再统一送 Cross-Encoder 重排。
     */
    private List<Document> fuseHybrid(String question, Long knowledgeId, Long tenantId, List<Document> vectorDocs) {
        Map<String, Double> rrf = new HashMap<>();
        Map<String, Document> byId = new HashMap<>();

        int rank = 1;
        for (Document d : vectorDocs) {
            accumulateRrf(rrf, byId, d, rank++);
        }

        if (bm25Retriever != null) {
            try {
                List<Document> bm25Docs = bm25Retriever.retrieve(question, knowledgeId, tenantId, BM25_TOP_K);
                rank = 1;
                for (Document d : bm25Docs) {
                    accumulateRrf(rrf, byId, d, rank++);
                }
                log.info("[chat][BM25 词法路径命中 {} 条]", CollUtil.size(bm25Docs));
            } catch (Exception e) {
                log.warn("[chat][BM25 词法路径异常，已忽略，knowledgeId={}]", knowledgeId, e);
            }
        }

        return rrf.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(FUSE_TOP_K)
                .map(e -> byId.get(e.getKey()))
                .toList();
    }

    private void accumulateRrf(Map<String, Double> rrf, Map<String, Document> byId, Document doc, int rank) {
        String id = doc.getId();
        if (id == null) {
            id = "doc_" + byId.size();
        }
        byId.putIfAbsent(id, doc);
        rrf.merge(id, 1.0 / (RRF_K + rank), Double::sum);
    }

    /**
     * 校验向量库是否可用
     */
    private void validateVectorStoreAvailable() {
        if (vectorStore == null) {
            throw exception(RAG_VECTOR_STORE_NOT_READY);
        }
    }

    /**
     * 构造 RAG 系统提示（P0-2 引用溯源）
     *
     * <p>当存在检索上下文时，要求 LLM 用 [N] 标注引用并在末尾输出"信息来源"列表。
     */
    private String buildSystemPrompt(String context) {
        if (StrUtil.isBlank(context)) {
            return "你是一个专业的知识库问答助手。请基于用户问题，使用你所知道的知识进行回答。" +
                    "如果无法回答，请明确告知用户。回答请使用中文。";
        }
        return StrUtil.format(
                "你是一个专业的知识库问答助手。请优先基于以下检索到的上下文回答用户问题。\n" +
                        "如果上下文中没有相关信息，请如实告知，并尝试基于通用知识回答。\n" +
                        "回答请使用中文。\n\n" +
                        "【引用规则】\n" +
                        "1. 在回答中引用检索上下文时，请用 [1][2]... 形式标注来源编号，编号对应下方【检索上下文】中的序号。\n" +
                        "2. 回答末尾必须追加一个【信息来源】列表，列出本次回答实际引用到的所有来源，格式如下：\n" +
                        "   【信息来源】\n" +
                        "   [1] 文档名 | 页码: N | chunk_id: xxx\n" +
                        "   [2] 文档名 | 页码: N | chunk_id: xxx\n" +
                        "3. 若未引用任何检索上下文（仅使用通用知识回答），可省略【信息来源】列表。\n\n" +
                        "【检索上下文】\n{}\n\n【结束】", context);
    }

    /**
     * 构造带 [N] 编号的引用上下文（P0-2 引用溯源）
     *
     * <p>每个 chunk 前加 {@code [N] 文档名 | 页码: N | chunk_id: xxx} 标识，便于 LLM 输出引用列表。
     */
    private String buildCitationContext(List<Document> documents) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Map<String, Object> meta = doc.getMetadata();
            String docName = meta != null ? String.valueOf(meta.getOrDefault("document_name", "-")) : "-";
            Object pageObj = meta != null ? meta.get("page") : null;
            String page = pageObj != null ? String.valueOf(pageObj) : "-";
            String chunkId = meta != null ? String.valueOf(meta.getOrDefault("chunk_id", doc.getId())) : String.valueOf(doc.getId());
            sb.append("[").append(i + 1).append("] ")
                    .append(docName).append(" | 页码: ").append(page).append(" | chunk_id: ").append(chunkId)
                    .append("\n")
                    .append(doc.getText()).append("\n\n---\n\n");
        }
        return sb.toString();
    }

    /**
     * 获取 ChatModel
     *
     * 优先使用 zhicloud-module-ai 的默认对话模型；若 AiModelService 不可用，则尝试从 Spring 容器直接注入。
     */
    private ChatModel getChatModel() {
        if (aiModelService != null) {
            AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            return aiModelService.getChatModel(model.getId());
        }
        throw exception(RAG_CHAT_ERROR);
    }

}
