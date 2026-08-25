# zhicloud-module-ai-rag

> 本地化 RAG（Retrieval-Augmented Generation）模块：基于 PostgreSQL + pgvector + BGE-base-zh-v1.5，提供完全本地化部署的检索增强生成能力。

## 1. 模块简介

`zhicloud-module-ai-rag` 是 zhicloud 项目的可选模块（Spring Modulith 声明 A3），提供一套**完全本地化**的 RAG 能力：

- 文档解析（Tika）→ 智能分块（TokenTextSplitter）→ 向量化（BGE-base-zh ONNX）→ 相似度检索（pgvector）→ LLM 流式回答
- 不依赖外部 Embedding API（OpenAI / 通义千问等），避免网络延迟与 API 费用
- 通过 `zhicloud.airag.enabled=true` 开关控制启用，默认不启用（PostgreSQL + pgvector 未部署时不会影响主应用启动）

模块依赖 `zhicloud-module-ai`（复用 Spring AI 配置、`AiModelService`、ChatClient 等），并新增 `spring-ai-starter-vector-store-pgvector` 与 `spring-ai-tika-document-reader` 两个 Spring AI 依赖。

## 2. 核心能力

| 能力 | 说明 |
|---|---|
| 知识库管理 | `airag_knowledge` 表，维护知识库元信息（名称、状态、Embedding 模型、向量维度） |
| 文档管理 | `airag_document` 表，记录文档处理状态（待处理/处理中/已完成/失败）、分块数量、错误信息 |
| 文档解析 | 基于 Apache Tika 解析 PDF / DOCX / TXT / MD 等格式 |
| 智能分块 | 基于 Spring AI `TokenTextSplitter` 按Token数切分 |
| 向量化 | 通过 ONNX Runtime 嵌入应用加载 BGE-base-zh-v1.5 模型（768 维） |
| 向量存储 | PostgreSQL + pgvector，HNSW 索引，余弦距离 |
| 相似度检索 | 基于用户问题向量做 Top-K 检索，召回相关文档块 |
| RAG 对话 | 检索结果作为上下文，调用 LLM 流式生成回答 |

## 3. 技术栈

| 组件 | 版本 / 说明 |
|---|---|
| Spring AI | 1.1.8（BOM 统一管理） |
| Spring AI PgVectorStore | `spring-ai-starter-vector-store-pgvector` |
| Spring AI Tika Document Reader | `spring-ai-tika-document-reader`（已排除 spring-cloud 传递依赖） |
| PostgreSQL | 16+（docker-compose 中使用 `pgvector/pgvector:pg16` 镜像） |
| pgvector | 随 `pgvector/pgvector:pg16` 镜像内置，需 `CREATE EXTENSION vector` |
| Embedding 模型 | BGE-base-zh-v1.5（768 维，~390MB） |
| ONNX Runtime | 通过 `TransformersEmbeddingModel` / `OnnxEmbeddingModel` 嵌入应用 |

> 关于 Embedding 模型的选型对比（BGE-small / base / large）详见模块根目录 [EMBEDDING_DECISION.md](./EMBEDDING_DECISION.md)。

## 4. 模块结构

```
zhicloud-module-ai-rag/
├── EMBEDDING_DECISION.md          # Embedding 模型选型决策文档
├── pom.xml                        # 模块依赖声明（spring-ai-bom 1.1.8 + pgvector + tika）
└── src/main/java/cn/zhicloud/zhicloud/module/airag/
    ├── AiragApplication.java      # 模块入口标记类（无独立 main，由 zhicloud-server 扫描加载）
    ├── package-info.java          # Spring Modulith 模块声明（A3，依赖 ai/system/infra）
    ├── config/
    │   └── AiragConfiguration.java     # 自动配置：@ConditionalOnProperty(zhicloud.airag.enabled=true)
    ├── enums/
    │   └── ErrorCodeConstants.java     # 错误码（1-041-xxx-xxx 段，与 ai 模块 1-040 区分）
    ├── controller/admin/
    │   ├── knowledge/                  # 知识库 CRUD
    │   ├── document/                   # 文档上传/管理
    │   └── chat/                       # RAG 对话（流式）
    ├── service/
    │   ├── knowledge/                  # AiragKnowledgeService
    │   ├── document/                   # AiragDocumentService（Tika 解析 + 分块）
    │   └── rag/                         # AiragRagService（检索 + LLM 调用）
    └── dal/
        ├── mysql/                      # AiragKnowledgeMapper、AiragDocumentMapper
        └── dataobject/                 # AiragKnowledgeDO、AiragDocumentDO
```

> 控制器位于 `controller.admin` 包下，自动继承 zhicloud 框架的 `/admin-api` URL 前缀。

## 5. 启用步骤

### 5.1 部署 PostgreSQL 16 + pgvector 扩展

最快的方式是使用项目根目录的 `docker-compose.yml`：

```bash
cd d:\Desktop\zhicloud\script\docker
docker compose up -d postgres
```

`docker-compose.yml` 中已使用 `pgvector/pgvector:pg16` 镜像（已内置 pgvector 扩展），并将 `sql/postgresql/airag_pgvector.sql` 挂载到 `docker-entrypoint-initdb.d/`，容器首次启动时自动执行建表脚本。

默认连接参数：

| 参数 | 默认值 |
|---|---|
| Host | localhost |
| Port | 5432 |
| Database | zhicloud_rag |
| Username | zhicloud |
| Password | zhicloud@2026 |

### 5.2 执行建表脚本

如未使用 docker-compose 自动初始化，需手动执行：

```bash
psql -h localhost -U zhicloud -d zhicloud_rag -f d:\Desktop\zhicloud\sql\postgresql\airag_pgvector.sql
```

该脚本会创建：

- `airag_knowledge`（知识库表）
- `airag_document`（文档表）
- `airag_vector_store`（向量存储表，**由 Spring AI `PgVectorStore` 通过 `initializeSchema=true` 自动创建**，脚本中仅作声明，避免与 Spring AI 版本不兼容）

### 5.3 下载 BGE-base-zh ONNX 模型

执行项目根目录的下载脚本：

```bash
# Linux / macOS / Git Bash / WSL
bash scripts/download-bge-model.sh

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File scripts\download-bge-model.ps1
```

下载完成后，模型文件应位于：

```
zhicloud-server/src/main/resources/airag/bge-base-zh/
├── model.onnx              # ONNX 模型（~390MB）
├── tokenizer.json          # HuggingFace Tokenizer
├── tokenizer_config.json   # Tokenizer 配置
├── config.json             # 模型配置
└── vocab.txt               # 词表
```

### 5.4 配置启用开关

在 `zhicloud-server` 的 `application-local.yaml`（或对应 profile）中：

```yaml
zhicloud:
  airag:
    enabled: true
    vector-table-name: airag_vector_store
```

如使用独立数据源（与 MySQL 业务库隔离），可参考 `docker-compose.yml` 中的配置：

```yaml
zhicloud:
  airag:
    enabled: true
    datasource:
      url: jdbc:postgresql://localhost:5432/zhicloud_rag
      username: zhicloud
      password: zhicloud@2026
```

### 5.5 补充 EmbeddingModel Bean（部署完成后）

当前 `AiragConfiguration` 仅在容器中存在 `EmbeddingModel` Bean 时才创建 `VectorStore`。部署完成后需在 `AiragConfiguration` 中新增 `airagEmbeddingModel` Bean（参考 EMBEDDING_DECISION.md §4.2 方式 A），并补充 `spring-ai-transformers` 依赖（如未在 zhicloud-module-ai 中引入）。

## 6. 维度对齐说明

BGE-base-zh 输出维度为 **768**，需在以下位置保持一致：

| 位置 | 配置 | 说明 |
|---|---|---|
| `AiragKnowledgeDO.vectorDimension` | 768 | 知识库元信息记录的向量维度 |
| `AiragConfiguration.DEFAULT_VECTOR_DIMENSION` | 768 | `PgVectorStore.builder().dimensions(768)` |
| `airag_vector_store.embedding` | `vector(768)` | pgvector 列类型（由 Spring AI 自动创建） |
| `AiragKnowledgeSaveReqVO.vectorDimension` | 768 | 创建知识库时校验 |

**切换模型时需同步调整**：

- 若改用 BGE-large-zh（1024 维），需重建 `airag_vector_store` 表（DROP + 重建），并修改 `AiragConfiguration.DEFAULT_VECTOR_DIMENSION`；
- 已导入的文档需重新向量化（触发 `AiragDocumentService.importDocument`）。

## 7. 设计要点

### 7.1 与 zhicloud-module-ai 的关系

`zhicloud-module-ai` 已有基于 Redis / Qdrant / Milvus 的向量存储能力（通过 `AiKnowledgeService` + `AiModelService.getOrCreateVectorStore(modelId, ...)` 创建），其 Embedding 通常依赖外部 Embedding API（OpenAI 兼容 / Ollama 等）。

本模块定位为**完全本地化方案**，差异如下：

| 维度 | zhicloud-module-ai（已有） | zhicloud-module-ai-rag（本模块） |
|---|---|---|
| 向量库 | Redis / Qdrant / Milvus（可选） | PostgreSQL + pgvector |
| Embedding 来源 | 外部 API（OpenAI / 通义 / Ollama） | 本地 ONNX Runtime（BGE-base-zh） |
| 维度 | 由 Embedding 模型决定 | 固定 768 维 |
| 适用场景 | 已有外部 LLM 服务，多模型切换 | 完全离线 / 数据不出域 / 成本敏感 |
| 启用方式 | 默认可用 | `zhicloud.airag.enabled=true` 显式启用 |

### 7.2 启动安全

`AiragConfiguration` 同时使用两个条件注解，确保 PostgreSQL + pgvector 未部署时主应用可正常启动：

```java
@Configuration
@ConditionalOnProperty(prefix = "zhicloud.airag", name = "enabled", havingValue = "true")
public class AiragConfiguration {
    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    public VectorStore airagVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) { ... }
}
```

- `@ConditionalOnProperty`：未配置 `zhicloud.airag.enabled=true` 时整个配置类不加载
- `@ConditionalOnBean(EmbeddingModel.class)`：即使开关打开，未提供 `EmbeddingModel` Bean 也不创建 `VectorStore`

### 7.3 错误码段

使用 `1-041-xxx-xxx` 段，与 `zhicloud-module-ai`（`1-040`）和 `zhicloud-module-ai-multiagent`（`1-042`）区分：

- `1_041_000_000` ~ `1_041_000_001`：知识库错误
- `1_041_001_000` ~ `1_041_001_004`：文档错误
- `1_041_002_000` ~ `1_041_002_003`：RAG 检索错误

## 8. 参考链接

- BGE 模型：https://huggingface.co/BAAI/bge-base-zh-v1.5
- pgvector 扩展：https://github.com/pgvector/pgvector
- Spring AI PgVectorStore：https://docs.spring.io/spring-ai/reference/api/vectorstores/pgvector.html
- Spring AI Transformers Embedding：https://docs.spring.io/spring-ai/reference/api/embeddings/transformers-embeddings.html
- ONNX Runtime：https://onnxruntime.ai/
- C-MTEC 中文向量评测榜单：https://github.com/embeddings-benchmark/mteb
