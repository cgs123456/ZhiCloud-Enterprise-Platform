# yudao-module-ai-rag Embedding 模型选型决策

> 本文档记录 yudao-module-ai-rag 模块在 Embedding 模型选型上的对比与结论。

## 1. 背景

本地化 RAG 模块需要一个中文向量化模型，用于：
1. 文档导入时，将文本分块向量化并写入 PostgreSQL + pgvector；
2. 用户提问时，将问题向量化后在向量库中做相似度检索。

约束：
- **完全本地化部署**，不依赖外部 Embedding API（OpenAI / 通义千问等），避免网络延迟与 API 费用；
- 中文场景为主；
- 通过 ONNX Runtime 嵌入应用，与 Spring AI 集成；
- 向量维度需与 PostgreSQL + pgvector 表结构对齐。

## 2. 候选模型对比

| 维度 | BGE-small-zh-v1.5 | BGE-base-zh-v1.5 | BGE-large-zh-v1.5 |
|---|---|---|---|
| 维度 | 512 | **768** | 1024 |
| 模型大小 | ~95 MB | ~390 MB | ~1.2 GB |
| 中文效果（C-MTEC） | 良好 | **优秀** | 顶级 |
| 推理速度（CPU） | 最快 | 适中 | 较慢 |
| 内存占用 | 低 | 中 | 高 |
| 是否支持 ONNX | 是 | **是** | 是 |
| 适用场景 | 资源受限 / 实时性高 | **通用推荐** | 精度优先 / 有 GPU |

> 数据来源：BAAI 官方发布的 C-MTEC 中文向量评测榜单（https://github.com/embeddings-benchmark/mteb）。

## 3. 选型结论

**采用 BGE-base-zh-v1.5（768 维）。**

理由：
1. **效果与成本的最佳平衡**：BGE-base-zh 在中文向量检索榜单上效果优秀，模型体积（~390MB）与推理速度可接受，是业界本地化部署的主流选择；
2. **维度 768 适中**：相比 BGE-large-zh 的 1024 维，768 维在 pgvector 中的存储与索引开销更小，HNSW 索引构建更快；相比 BGE-small-zh 的 512 维，召回质量更稳定；
3. **官方提供 ONNX 导出**：BAAI 官方与 HuggingFace 社区均提供 BGE-base-zh 的 ONNX 版本，可直接通过 ONNX Runtime 加载，无需自行转换；
4. **与 Spring AI 兼容**：Spring AI 支持通过 ONNX Runtime 加载本地 Embedding 模型（`TransformersEmbeddingModel` / `OnnxEmbeddingModel`），可无缝接入。

## 4. ONNX Runtime 部署方式

采用 **嵌入应用** 的方式（而非独立微服务），原因：
- yudao 为单体/单体模块化架构，减少独立服务运维成本；
- 文档向量化是低频异步操作，对主链路性能影响可控；
- ONNX Runtime 通过 JNI 调用，进程内加载即可。

### 4.1 模型文件准备

从 HuggingFace 下载 BGE-base-zh-v1.5 的 ONNX 版本：
- 模型仓库：`BAAI/bge-base-zh-v1.5`（原生 PyTorch）或社区导出的 ONNX 版本
- 推荐使用 `onnx` 子目录下的 `model.onnx` + `tokenizer.json`

放置路径（约定）：
```
yudao-server/
└── resources/
    └── airag/
        └── bge-base-zh/
            ├── model.onnx        # ONNX 模型
            ├── tokenizer.json    # HuggingFace tokenizer
            └── tokenizer_config.json
```

### 4.2 Spring AI 集成方式

Spring AI 提供两种加载本地 ONNX Embedding 模型的方式：

#### 方式 A：TransformersEmbeddingModel（基于 ONNX Runtime + HuggingFace Tokenizers）

```java
// 需引入 spring-ai-transformers 依赖（如未在 yudao-module-ai 中引入，需在本模块 pom.xml 补充）
@Bean
@ConditionalOnProperty(prefix = "yudao.airag", name = "enabled", havingValue = "true")
public EmbeddingModel airagEmbeddingModel() {
    // 指定 ONNX 模型与 tokenizer 的资源路径
    return new TransformersEmbeddingModel(new ClassPathResource("airag/bge-base-zh/model.onnx"),
            new ClassPathResource("airag/bge-base-zh/tokenizer.json"));
}
```

#### 方式 B：复用 yudao-module-ai 的 AiModelService

在 yudao 后台「AI 模型管理」中配置一个 EMBEDDING 类型的模型（平台选 OpenAI 兼容 / Ollama 等），由 `AiModelService.getOrCreateVectorStore(modelId, ...)` 创建 VectorStore。此方式依赖外部 Embedding 服务，**不符合完全本地化**诉求，仅作为 fallback。

### 4.3 当前实现状态

由于 PostgreSQL + pgvector 当前未部署，本模块的 `AiragConfiguration` 通过 `@ConditionalOnProperty(prefix = "yudao.airag", name = "enabled", havingValue = "true")` 控制 `PgVectorStore` Bean 的加载，且 `PgVectorStore` 依赖容器中存在 `EmbeddingModel` Bean 才创建。

**部署完成后，需要：**
1. 引入 `spring-ai-transformers` 依赖（或自行实现 BGE-base-zh 的 ONNX EmbeddingModel Bean）；
2. 在 `AiragConfiguration` 中新增 `airagEmbeddingModel` Bean（参考 4.1 方式 A）；
3. 配置 `yudao.airag.enabled=true`；
4. 执行 `sql/postgresql/airag_pgvector.sql` 建表脚本。

## 5. 维度对齐说明

BGE-base-zh 输出维度为 **768**，需在以下位置保持一致：

| 位置 | 配置 | 说明 |
|---|---|---|
| `AiragKnowledgeDO.vectorDimension` | 768 | 知识库元信息记录 |
| `AiragConfiguration.airagVectorStore` | `.dimensions(768)` | PgVectorStore 建表维度 |
| `airag_vector_store.embedding` | `vector(768)` | pgvector 列类型（由 Spring AI 自动创建） |
| `AiragKnowledgeSaveReqVO.vectorDimension` | 768 | 创建知识库时校验 |

**切换模型时需同步调整：**
- 若改用 BGE-large-zh（1024 维），需重建 `airag_vector_store` 表（DROP + 重建），并修改 `AiragConfiguration.DEFAULT_VECTOR_DIMENSION`；
- 已导入的文档需重新向量化（`AiragDocumentService` 触发 `importDocument`）。

## 6. 参考链接

- BGE 模型：https://huggingface.co/BAAI/bge-base-zh-v1.5
- pgvector：https://github.com/pgvector/pgvector
- Spring AI PgVectorStore：https://docs.spring.io/spring-ai/reference/api/vectorstores/pgvector.html
- Spring AI Transformers Embedding：https://docs.spring.io/spring-ai/reference/api/embeddings/transformers-embeddings.html
- ONNX Runtime：https://onnxruntime.ai/
