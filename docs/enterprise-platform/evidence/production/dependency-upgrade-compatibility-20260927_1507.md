# Maven HIGH/CRITICAL 依赖升级兼容性分析（方案，未执行）

> 证据版本：v1  
> 生成时间：2026-09-27T15:08:53.464973+00:00  
> 结果 JSON：`docs/enterprise-platform/evidence/production/dependency-upgrade-compatibility-20260927_1507.json`  
> JSON SHA-256：`3124DBBE4FF1A3628E695664922F8F71687EC4946A39F78807381C6AE1B7E47F`

## 1. 结论

OSV 已识别 27 个含 HIGH/CRITICAL 的 Maven 组件版本；其中 17 个进入 `zhicloud-server` runtime scope，10 个未进入。本文件给出**分批升级建议和兼容性风险**，不是升级完成证据，也不是生产就绪证据。

**本步骤没有修改任何 POM。** `production_ready=false`、`gate3_passed=false`、`R-005=OPEN`、`R-011=OPEN`、`R-029=OPEN`、`T-107=OPEN`、`independent_review=false` 保持不变。

## 2. 范围与限制

- 已做：OSV 修复版本候选、`zhicloud-server` runtime tree triage、Spring Boot/Netty BOM 属性覆盖可行性检查。

- 未做：实际改 POM、编译、全量测试、数据库/消息队列/对象存储真实集成、前端/镜像/历史 Secret/许可证治理、漏洞可利用性判定。

- `server_runtime=YES` 只表示进入当前 server runtime 依赖树，不等于漏洞可被利用；`NO` 只表示未进入该树，不等于无风险。

## 3. 推荐升级目标与机制

矩阵共 27 行；Server runtime YES=17，NO=10。目标版本是满足 OSV fixed 的最低稳定版本，不是无脑取最新版；`latest` 版本另行评估。

| 组件 | 当前版本 | Server runtime | HIGH/CRITICAL | 推荐目标 | 机制 | 兼容性判断 | 风险与验证点 |
|---|---:|---|---:|---|---|---|---|
| amqp-client | 5.25.0 | NO | 4 | 5.34.0 | Spring Boot BOM property rabbit-amqp-client.version | medium | Boot 3.5.16 基线为 5.25.0；5.x 内补丁升级，需用 MQ 模块编译和 RabbitMQ 容器契约验证。当前未进入 zhicloud-server runtime tree。 |
| bcprov-jdk18on | 1.80 | YES | 3 | 1.85 | zhicloud-dependencies property bouncycastle.version | medium | 覆盖 3 条 HIGH/CRITICAL；验证 JWT/TLS/加密工具、BC Provider 注册和认证回归。 |
| commons-beanutils | 1.9.4 | YES | 1 | 1.11.0 | zhicloud-dependencies 显式 dependencyManagement | medium | 来自 TinyFlow/Agents Flex/QLExpress 链；验证表达式链和 Bean 反射行为。 |
| grpc-netty-shaded | 1.53.0 | NO | 1 | 1.75.0 | zhicloud-dependencies 显式 grpc-bom 或 dependencyManagement | medium | 未进入 server runtime tree；统一版本可避免多版本并存。 |
| grpc-netty-shaded | 1.65.1 | YES | 1 | 1.75.0 | zhicloud-dependencies 显式 grpc-bom 或 dependencyManagement | medium | server 路径来自 Spring AI Qdrant store -> Qdrant client；验证向量库连接和 gRPC 调用。 |
| httpcore5 | 5.3.6 | YES | 1 | 5.4.3 | Spring Boot BOM property httpcore5.version | low | Nacos client/httpclient5 路径使用；验证配置中心连接、HTTP 客户端和超时行为。 |
| httpcore5-h2 | 5.3.6 | YES | 1 | 5.4.3 | Spring Boot BOM property httpcore5.version | low | 与 httpcore5 保持同版本；验证 HTTP/2 或长连接路径。 |
| ini4j | 0.5.4 | YES | 1 | exclude (no fixed) | 在 TinyFlow -> Agents Flex image-tencent 依赖上排除 | needs_change | OSV 无 fixed 候选；源代码当前未见直接使用，但排除后必须跑 AI 编译、启动和 Agent/Image 功能冒烟。 |
| jackson-core | 3.0.1 | YES | 3 | 3.1.4 | zhicloud-dependencies 显式 tools.jackson dependencyManagement | medium | Spring Boot BOM 当前只导入 Jackson 2 BOM；3.x 需单独管理。来自 Flowable 8 路径，验证 BPM JSON 序列化。 |
| jackson-databind | 3.0.1 | YES | 2 | 3.1.4 | zhicloud-dependencies 显式 tools.jackson dependencyManagement | medium | 与 jackson-core 同步升级；验证 BPM/CRM/OA 的对象映射、日期、多态和空值行为。 |
| lz4 | 1.3.0 | YES | 2 | exclude or replace (no fixed for artifact) | 在 TinyFlow -> Agents Flex image-volcengine 依赖上排除；如确需功能再评估 org.lz4:lz4-java:1.8.1 | needs_change | OSV 对该 artifact 无 fixed 候选；org.lz4:lz4-java 只是候选替换方向，包/API 兼容性未验证。 |
| lz4-java | 1.8.0 | NO | 2 | 1.8.1 | 显式 dependencyManagement | low | 未进入 server runtime tree；如引入后需验证压缩 API。 |
| mcp-core | 0.18.3 | YES | 1 | 1.0.0 | 先做 Spring AI/MCP SDK 兼容性 spike，再显式 dependencyManagement | high | 0.18 -> 1.0 是 major bump；Spring AI 1.1.8 当前拉入 0.18.3 且项目实际使用 MCP 客户端/服务端，禁止盲改版本。需要确认 mcp-spring-webmvc 与 mcp-core 1.0.0 的组合，或升级 Spring AI 版本。 |
| netty-codec-compression | 4.2.15.Final | YES | 1 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | AWS S3 netty client 路径；与其他 Netty 组件统一升级。 |
| netty-codec-haproxy | 4.2.15.Final | NO | 1 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | 未进入 server runtime tree；随 Netty BOM 统一。 |
| netty-codec-http | 4.2.15.Final | YES | 3 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | AWS S3、Redis、WebSocket、MQ 相关路径；需做启动和连接冒烟。 |
| netty-codec-http2 | 4.2.15.Final | YES | 1 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | AWS S3 netty client 路径；验证 HTTP/2 客户端连接。 |
| netty-codec-http3 | 4.2.15.Final | NO | 1 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | 未进入 server runtime tree；随 Netty BOM 统一。 |
| netty-codec-stomp | 4.2.15.Final | NO | 1 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | 未进入 server runtime tree；随 Netty BOM 统一。 |
| netty-codec-xml | 4.2.15.Final | NO | 2 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | 未进入 server runtime tree；随 Netty BOM 统一。 |
| netty-handler | 4.2.15.Final | YES | 1 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | 覆盖 CRITICAL；验证 TLS 握手、Redis、S3、WebSocket。 |
| netty-handler-ssl-ocsp | 4.2.15.Final | NO | 3 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | 未进入 server runtime tree；随 Netty BOM 统一。 |
| netty-transport-sctp | 4.2.15.Final | NO | 1 | 4.2.17.Final | zhicloud-dependencies property netty.version | low | 未进入 server runtime tree；随 Netty BOM 统一。 |
| opennlp-tools | 2.3.3 | YES | 3 | 2.5.9 | zhicloud-dependencies 显式 dependencyManagement 或 DashScope 依赖链覆盖 | medium | 来自 DashScope 依赖链；2.x 内升级，验证 NLP/Tokenizer/模型加载行为，避免跳到 3.0.0-M6。 |
| postgresql | 42.7.11 | YES | 1 | 42.7.12 | Spring Boot BOM property postgresql.version | low | 验证 Flyway 迁移、pgvector、连接池和 TLS 配置。 |
| protobuf-java | 3.20.1 | NO | 3 | 3.25.5 | zhicloud-dependencies 显式 dependencyManagement | medium | 未进入 server runtime tree；3.x 修复版本优先于 4.x，避免生成代码/API 连锁升级。 |
| tomcat-embed-core | 10.1.55 | YES | 3 | 10.1.58 | Spring Boot BOM property tomcat.version | low | 覆盖 3 条 CRITICAL；验证 Web、Session、Cookie/Header、TLS 和静态资源。 |

## 4. 属性覆盖可行性（只验证 effective POM）

以下属性已通过 `mvn -pl zhicloud-dependencies help:effective-pom` 做过解析验证；这只证明 Maven 能解析到目标版本，不证明运行时兼容。

| 属性 | 测试值 | 结果 |
|---|---:|---|
| `netty.version` | `4.2.17.Final` | Netty artifacts managed at 4.2.17.Final in effective POM |
| `tomcat.version` | `10.1.58` | tomcat-embed-core managed at 10.1.58 in effective POM |
| `postgresql.version` | `42.7.12` | postgresql managed at 42.7.12 in effective POM |
| `httpcore5.version` | `5.4.3` | httpcore5 and httpcore5-h2 managed at 5.4.3 in effective POM |
| `rabbit-amqp-client.version` | `5.34.0` | amqp-client managed at 5.34.0 in effective POM |

## 5. 建议执行顺序

1. **批次 A：低风险属性升级**。Netty -> 4.2.17.Final、Tomcat -> 10.1.58、PostgreSQL -> 42.7.12、HTTPCORE5 -> 5.4.3、Bouncy Castle -> 1.85。先跑全量后端测试，再补 Redis/S3/WebSocket/TLS/Nacos 冒烟。

2. **批次 B：显式 dependencyManagement 的中风险升级**。Jackson 3.1.4、commons-beanutils 1.11.0、opennlp-tools 2.5.9、grpc-netty-shaded 1.75.0。重点回归 Flowable/BPM、QLExpress、DashScope 和 Qdrant。

3. **批次 C：需要 spike 的升级**。`mcp-core` 0.18.3 -> 1.0.0 不能直接盲改；先确认与 Spring AI 1.1.8、`mcp-spring-webmvc` 的组合，必要时升级 Spring AI。`amqp-client` 5.34.0 和 `lz4-java` 1.8.1 虽然风险较低，但也不在本轮直接修改。

4. **批次 D：无 fixed 版本的处理**。`ini4j` 0.5.4 和旧 `net.jpountz.lz4:lz4` 1.3.0 无 OSV fixed 候选；当前源码未发现直接使用，建议先在对应传递依赖上排除并跑 AI/Agent/Image 功能冒烟。若功能确需保留，必须登记责任人、补偿控制和到期时间的例外。

5. **复扫闭环**。升级后重新生成 CycloneDX SBOM，重跑 OSV；有 NVD API Key 后重跑 OWASP Dependency-Check。确认 HIGH/CRITICAL 下降且新增依赖未引入新漏洞。

6. **发布门禁**。完成 `mvn clean test`、关键模块集成测试、镜像/K8s 环境可用后的部署冒烟；任何测试失败不得 suppress 或绕过。

## 6. 回滚

- 每个批次独立分支/PR，禁止混合提交。

- 升级前保存原 effective dependency tree、SBOM 和测试报告。

- 任一批次失败时回退该批次 POM/dependencyManagement 变更，重新生成依赖树并复跑测试；不得只回退测试或修改断言。

## 7. 不改变当前状态

- T-107 仍未关闭：历史 Secret 考古、前端/镜像扫描、许可证治理、真实 TLS/K8s、生产迁移和升级后复扫未完成。

- 本文件是方案证据，不能作为 `T-107` 关闭、`R-005` 关闭或生产上线批准依据。

