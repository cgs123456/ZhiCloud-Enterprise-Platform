# 依赖漏洞 OSV 初筛证据（Maven partial）

> 证据版本：v1  
> 生成时间：2026-09-27T14:39:21.819029+00:00  
> 源 BOM SHA-256：`aeba1568cdd26ad9c637b4e7c69c3692408903b13207b79ed5fbf8bcb4388696`  
> 结果 JSON：`docs/enterprise-platform/evidence/production/osv-maven-partial-20260927_143422.json`  
> JSON SHA-256：`DF21C8C6314ADBD0D1E2433571D3833CEB10335F01CCFB25AFFCEEC72996219A`

## 1. 结论

OSV 初筛完成 827 个外部 Maven 组件，命中 40 个组件、85 条漏洞记录、79 个唯一漏洞 ID；其中 27 个组件存在 48 条 HIGH/CRITICAL 记录。

**这不是通过证据，也不是生产就绪证据。** 它只是 Maven 依赖初筛，不能解释运行时可达性，也不能替代修复、复扫、渗透测试或生产验收。

## 2. 结果摘要

| 指标 | 数值 |
|---|---:|
| 查询组件数 | 827 |
| 命中组件数 | 40 |
| 漏洞记录数 | 85 |
| 唯一漏洞 ID 数 | 79 |
| 唯一 CVE 别名数 | 76 |
| CRITICAL | 8 |
| HIGH | 37 |
| MODERATE | 32 |
| LOW | 2 |
| 已撤销漏洞记录 | 0 |
| 存在 HIGH/CRITICAL 的组件数 | 27 |
| HIGH/CRITICAL 记录数 | 48 |

## 3. 限制

- 覆盖范围：Maven aggregate BOM；源 BOM 共 863 个组件，排除 36 个 `cn.zhicloud` 内部构件后查询 827 个。
- 不覆盖：前端依赖、容器镜像、OS 基础层、Git 历史 Secret、CI 日志、备份、许可证治理和运行时可达性。
- OSV 只提供漏洞匹配和公开严重级别，不等同于对本系统路径的攻击面判定。
- 本地 OWASP Dependency-Check 尝试失败，原因是无 NVD API Key 时 NVD 更新失败；该失败已记录在 JSON 的 `failed_dependency_check_attempt`，不得解释为“无漏洞”。
- 不因为本报告改变 `production_ready=false`、`gate3_passed=false`、`R-005=OPEN` 或 `T-107=OPEN`。

## 4. HIGH/CRITICAL 组件概览

| 组件 | 当前版本 | HIGH/CRITICAL 数 | 漏洞 ID |
|---|---:|---:|---|
| amqp-client | 5.25.0 | 4 | GHSA-68mj-5wr7-6fgg<br>GHSA-6g32-pxv4-2wfj<br>GHSA-93j5-89vc-pph4<br>GHSA-jh4v-gfqj-7rhx |
| bcprov-jdk18on | 1.80 | 3 | GHSA-574f-3g2m-x479<br>GHSA-9pwp-9qqc-pr26<br>GHSA-qp49-qgx5-5m26 |
| jackson-core | 3.0.1 | 3 | GHSA-2m67-wjpj-xhg9<br>GHSA-6v53-7c9g-w56r<br>GHSA-r7wm-3cxj-wff9 |
| netty-codec-http | 4.2.15.Final | 3 | GHSA-6jqx-86gh-f27w<br>GHSA-jppx-w49h-x2qq<br>GHSA-mvh2-crg5-v77c |
| netty-handler-ssl-ocsp | 4.2.15.Final | 3 | GHSA-272m-gcwp-mpwg<br>GHSA-g7hg-vrcf-mvmr<br>GHSA-wc96-39fc-566f |
| opennlp-tools | 2.3.3 | 3 | GHSA-4v8g-86x5-3vrc<br>GHSA-659w-93r5-9j6m<br>GHSA-cx4m-2p55-rw7j |
| protobuf-java | 3.20.1 | 3 | GHSA-4gg5-vx3j-xwc7<br>GHSA-735f-pc8j-v9w8<br>GHSA-g5ww-5jh7-63cx |
| tomcat-embed-core | 10.1.55 | 3 | GHSA-9xv2-5v5q-p794<br>GHSA-gcx9-497g-6cp6<br>GHSA-h3x4-894j-xpx5 |
| jackson-databind | 3.0.1 | 2 | GHSA-j3rv-43j4-c7qm<br>GHSA-rmj7-2vxq-3g9f |
| lz4 | 1.3.0 | 2 | GHSA-cmp6-m4wj-q63q<br>GHSA-vqf4-7m7x-wgfc |
| lz4-java | 1.8.0 | 2 | GHSA-cmp6-m4wj-q63q<br>GHSA-vqf4-7m7x-wgfc |
| netty-codec-xml | 4.2.15.Final | 2 | GHSA-4qhr-g3c6-fcfx<br>GHSA-v74w-7mr3-4qg3 |
| commons-beanutils | 1.9.4 | 1 | GHSA-wxr5-93ph-8wr9 |
| grpc-netty-shaded | 1.53.0 | 1 | GHSA-prj3-ccx8-p6x4 |
| grpc-netty-shaded | 1.65.1 | 1 | GHSA-prj3-ccx8-p6x4 |
| httpcore5 | 5.3.6 | 1 | GHSA-hf6x-8p5f-cgmf |
| httpcore5-h2 | 5.3.6 | 1 | GHSA-v3jc-474w-2wm6 |
| ini4j | 0.5.4 | 1 | GHSA-jr6h-r7vg-f9mc |
| mcp-core | 0.18.3 | 1 | GHSA-8jxr-pr72-r468 |
| netty-codec-compression | 4.2.15.Final | 1 | GHSA-558v-64gr-wgg4 |
| netty-codec-haproxy | 4.2.15.Final | 1 | GHSA-q6cq-mhr2-jmr5 |
| netty-codec-http2 | 4.2.15.Final | 1 | GHSA-93wv-jw9v-4972 |
| netty-codec-http3 | 4.2.15.Final | 1 | GHSA-hpcc-26xq-25fv |
| netty-codec-stomp | 4.2.15.Final | 1 | GHSA-vhch-2wf3-m8rp |
| netty-handler | 4.2.15.Final | 1 | GHSA-c4c3-7fpv-j4q5 |
| netty-transport-sctp | 4.2.15.Final | 1 | GHSA-2qj4-mmr9-4v2f |
| postgresql | 42.7.11 | 1 | GHSA-j92g-9f8w-j867 |

## 5. 下一步

1. 先做可达性 triage：确认每个命中的依赖是否进入生产 classpath、实际启用的协议/功能和暴露面。
2. 按漏洞修复版本统一升级或排除，禁止无理由 suppress；如需例外，必须登记风险、责任人和到期时间。
3. 携带 NVD API Key 重跑 OWASP Dependency-Check，并保存完整报告。
4. 前端、镜像、Git 历史、许可证和历史 Secret 考古继续作为 T-107 的未闭合项。
5. 升级后重新生成 SBOM、重新执行 OSV/Dependency-Check，并做回归测试。

## 6. HIGH/CRITICAL 依赖可达性 triage（partial）

下表只回答“是否进入 `zhicloud-server` 的 runtime scope 依赖树”，不判断漏洞是否可被实际利用；也不覆盖运行时 profile、feature toggle、网络暴露和真实流量。`NO` 只表示未进入当前 `zhicloud-server` runtime tree，不能判定为无风险。

| 组件 | 当前版本 | Server runtime | 依赖路径 / 说明 | HIGH/CRITICAL |
|---|---:|---:|---|---:|
| amqp-client | 5.25.0 | NO | 不在 zhicloud-server runtime tree；其他模块树出现 3 次 | 4 |
| bcprov-jdk18on | 1.80 | YES | org.springframework.cloud:spring-cloud-starter:jar:4.3.3 -> org.bouncycastle:bcprov-jdk18on:jar:1.80 | 3 |
| jackson-core | 3.0.1 | YES | org.flowable:flowable-spring-boot-starter-actuator:jar:8.0.0 -> org.flowable:flowable-bpmn-model:jar:8.0.0 -> tools.jackson.core:jackson-databind:jar:3.0.1 -> tools.jackson.core:jackson-core:jar:3.0.1 | 3 |
| netty-codec-http | 4.2.15.Final | YES | software.amazon.awssdk:s3:jar:2.46.17 -> software.amazon.awssdk:netty-nio-client:jar:2.46.17 -> io.netty:netty-codec-http:jar:4.2.15.Final | 3 |
| netty-handler-ssl-ocsp | 4.2.15.Final | NO | 不在 zhicloud-server runtime tree；其他模块树出现 4 次 | 3 |
| opennlp-tools | 2.3.3 | YES | com.alibaba.cloud.ai:spring-ai-alibaba-starter-dashscope:jar:1.1.2.2 -> com.alibaba.cloud.ai:spring-ai-alibaba-autoconfigure-dashscope:jar:1.1.2.2 -> com.alibaba.cloud.ai:spring-ai-alibaba-dashscope:jar:1.1.2.2 -> org.apache.opennlp:opennlp-tools:jar:2.3.3 | 3 |
| protobuf-java | 3.20.1 | NO | 不在 zhicloud-server runtime tree；其他模块树出现 3 次 | 3 |
| tomcat-embed-core | 10.1.55 | YES | cn.zhicloud:zhicloud-spring-boot-starter-web:jar:2026.06.0 -> org.springframework.boot:spring-boot-starter-web:jar:3.5.16 -> org.springframework.boot:spring-boot-starter-tomcat:jar:3.5.16 -> org.apache.tomcat.embed:tomcat-embed-core:jar:10.1.55 | 3 |
| jackson-databind | 3.0.1 | YES | org.flowable:flowable-spring-boot-starter-actuator:jar:8.0.0 -> org.flowable:flowable-bpmn-model:jar:8.0.0 -> tools.jackson.core:jackson-databind:jar:3.0.1 | 2 |
| lz4 | 1.3.0 | YES | dev.tinyflow:tinyflow-java-core:jar:1.2.6 -> com.agentsflex:agents-flex-bom:jar:1.4.0 -> com.agentsflex:agents-flex-image-volcengine:jar:1.4.0 -> net.jpountz.lz4:lz4:jar:1.3.0 | 2 |
| lz4-java | 1.8.0 | NO | 不在 zhicloud-server runtime tree；其他模块树出现 3 次 | 2 |
| netty-codec-xml | 4.2.15.Final | NO | 不在 zhicloud-server runtime tree；其他模块树出现 4 次 | 2 |
| commons-beanutils | 1.9.4 | YES | dev.tinyflow:tinyflow-java-core:jar:1.2.6 -> com.agentsflex:agents-flex-bom:jar:1.4.0 -> com.agentsflex:agents-flex-chain-qlexpress:jar:1.4.0 -> commons-beanutils:commons-beanutils:jar:1.9.4 | 1 |
| grpc-netty-shaded | 1.53.0 | NO | 不在 zhicloud-server runtime tree；其他模块树出现 3 次 | 1 |
| grpc-netty-shaded | 1.65.1 | YES | org.springframework.ai:spring-ai-starter-vector-store-qdrant:jar:1.1.8 -> org.springframework.ai:spring-ai-qdrant-store:jar:1.1.8 -> io.qdrant:client:jar:1.13.0 -> io.grpc:grpc-netty-shaded:jar:1.65.1 | 1 |
| httpcore5 | 5.3.6 | YES | com.alibaba.nacos:nacos-client:jar:3.0.3 -> org.apache.httpcomponents.core5:httpcore5:jar:5.3.6 | 1 |
| httpcore5-h2 | 5.3.6 | YES | com.alibaba.nacos:nacos-client:jar:3.0.3 -> org.apache.httpcomponents.client5:httpclient5:jar:5.5.2 -> org.apache.httpcomponents.core5:httpcore5-h2:jar:5.3.6 | 1 |
| ini4j | 0.5.4 | YES | dev.tinyflow:tinyflow-java-core:jar:1.2.6 -> com.agentsflex:agents-flex-bom:jar:1.4.0 -> com.agentsflex:agents-flex-image-tencent:jar:1.4.0 -> org.ini4j:ini4j:jar:0.5.4 | 1 |
| mcp-core | 0.18.3 | YES | org.springframework.ai:spring-ai-starter-mcp-server-webmvc:jar:1.1.8 -> io.modelcontextprotocol.sdk:mcp-spring-webmvc:jar:0.18.3 -> io.modelcontextprotocol.sdk:mcp-core:jar:0.18.3 | 1 |
| netty-codec-compression | 4.2.15.Final | YES | software.amazon.awssdk:s3:jar:2.46.17 -> software.amazon.awssdk:netty-nio-client:jar:2.46.17 -> io.netty:netty-codec-http:jar:4.2.15.Final -> io.netty:netty-codec-compression:jar:4.2.15.Final | 1 |
| netty-codec-haproxy | 4.2.15.Final | NO | 不在 zhicloud-server runtime tree；其他模块树出现 4 次 | 1 |
| netty-codec-http2 | 4.2.15.Final | YES | software.amazon.awssdk:s3:jar:2.46.17 -> software.amazon.awssdk:netty-nio-client:jar:2.46.17 -> io.netty:netty-codec-http2:jar:4.2.15.Final | 1 |
| netty-codec-http3 | 4.2.15.Final | NO | 不在 zhicloud-server runtime tree；其他模块树出现 4 次 | 1 |
| netty-codec-stomp | 4.2.15.Final | NO | 不在 zhicloud-server runtime tree；其他模块树出现 4 次 | 1 |
| netty-handler | 4.2.15.Final | YES | software.amazon.awssdk:s3:jar:2.46.17 -> software.amazon.awssdk:netty-nio-client:jar:2.46.17 -> io.netty:netty-handler:jar:4.2.15.Final | 1 |
| netty-transport-sctp | 4.2.15.Final | NO | 不在 zhicloud-server runtime tree；其他模块树出现 4 次 | 1 |
| postgresql | 42.7.11 | YES | org.springframework.ai:spring-ai-starter-vector-store-pgvector:jar:1.1.8 -> org.springframework.ai:spring-ai-pgvector-store:jar:1.1.8 -> org.postgresql:postgresql:jar:42.7.11 | 1 |

`YES = 17`，`NO = 10`。

## 7. HIGH/CRITICAL 修复版本候选（OSV affected ranges）

下表由 OSV 漏洞详情中的 `affected.ranges.events.fixed` 汇总而来，仅是“至少需要达到的版本候选”，不是兼容性验证结果。同一个组件如果不同漏洞修复版本不同，应取其中的最高版本；实际升级前必须检查依赖兼容性并回归测试。

| 组件 | 当前版本 | Server runtime | 漏洞 ID | OSV 严重级别 | Fixed version 候选 |
|---|---:|---:|---|---|---|
| amqp-client | 5.25.0 | NO | GHSA-68mj-5wr7-6fgg | HIGH | 5.33.1 |
| amqp-client | 5.25.0 | NO | GHSA-6g32-pxv4-2wfj | HIGH | 5.33.0 |
| amqp-client | 5.25.0 | NO | GHSA-93j5-89vc-pph4 | HIGH | 5.33.1 |
| amqp-client | 5.25.0 | NO | GHSA-jh4v-gfqj-7rhx | HIGH | 5.34.0 |
| bcprov-jdk18on | 1.80 | YES | GHSA-574f-3g2m-x479 | CRITICAL | 1.80.2, 1.81.1, 1.84 |
| bcprov-jdk18on | 1.80 | YES | GHSA-9pwp-9qqc-pr26 | CRITICAL | 1.85 |
| bcprov-jdk18on | 1.80 | YES | GHSA-qp49-qgx5-5m26 | HIGH | 1.85 |
| jackson-core | 3.0.1 | YES | GHSA-2m67-wjpj-xhg9 | HIGH | 3.1.1 |
| jackson-core | 3.0.1 | YES | GHSA-6v53-7c9g-w56r | HIGH | 3.1.0 |
| jackson-core | 3.0.1 | YES | GHSA-r7wm-3cxj-wff9 | HIGH | 3.1.4 |
| netty-codec-http | 4.2.15.Final | YES | GHSA-6jqx-86gh-f27w | HIGH | 4.2.16.Final, 4.1.136.Final |
| netty-codec-http | 4.2.15.Final | YES | GHSA-jppx-w49h-x2qq | HIGH | 4.2.16.Final, 4.1.136.Final |
| netty-codec-http | 4.2.15.Final | YES | GHSA-mvh2-crg5-v77c | HIGH | 4.2.16.Final, 4.1.136.Final |
| netty-handler-ssl-ocsp | 4.2.15.Final | NO | GHSA-272m-gcwp-mpwg | HIGH | 4.2.16.Final, 4.1.136.Final |
| netty-handler-ssl-ocsp | 4.2.15.Final | NO | GHSA-g7hg-vrcf-mvmr | HIGH | 4.2.16.Final, 4.1.136.Final |
| netty-handler-ssl-ocsp | 4.2.15.Final | NO | GHSA-wc96-39fc-566f | HIGH | 4.2.16.Final, 4.1.136.Final |
| opennlp-tools | 2.3.3 | YES | GHSA-4v8g-86x5-3vrc | CRITICAL | 2.5.9, 3.0.0-M3 |
| opennlp-tools | 2.3.3 | YES | GHSA-659w-93r5-9j6m | HIGH | 2.5.9, 3.0.0-M3 |
| opennlp-tools | 2.3.3 | YES | GHSA-cx4m-2p55-rw7j | CRITICAL | 2.5.9, 3.0.0-M3, 1.9.5 |
| protobuf-java | 3.20.1 | NO | GHSA-4gg5-vx3j-xwc7 | HIGH | 3.16.3, 3.19.6, 3.20.3, 3.21.7 |
| protobuf-java | 3.20.1 | NO | GHSA-735f-pc8j-v9w8 | HIGH | 3.25.5, 4.27.5, 4.28.2 |
| protobuf-java | 3.20.1 | NO | GHSA-g5ww-5jh7-63cx | HIGH | 3.16.3, 3.19.6, 3.20.3, 3.21.7 |
| tomcat-embed-core | 10.1.55 | YES | GHSA-9xv2-5v5q-p794 | CRITICAL | 11.0.25, 10.1.58, 9.0.121 |
| tomcat-embed-core | 10.1.55 | YES | GHSA-gcx9-497g-6cp6 | CRITICAL | 11.0.25, 10.1.58, 9.0.121 |
| tomcat-embed-core | 10.1.55 | YES | GHSA-h3x4-894j-xpx5 | CRITICAL | 11.0.25, 10.1.58, 9.0.121 |
| jackson-databind | 3.0.1 | YES | GHSA-j3rv-43j4-c7qm | HIGH | 3.1.4 |
| jackson-databind | 3.0.1 | YES | GHSA-rmj7-2vxq-3g9f | HIGH | 3.1.4 |
| lz4 | 1.3.0 | YES | GHSA-cmp6-m4wj-q63q | HIGH | N/A |
| lz4 | 1.3.0 | YES | GHSA-vqf4-7m7x-wgfc | HIGH | N/A |
| lz4-java | 1.8.0 | NO | GHSA-cmp6-m4wj-q63q | HIGH | N/A |
| lz4-java | 1.8.0 | NO | GHSA-vqf4-7m7x-wgfc | HIGH | 1.8.1 |
| netty-codec-xml | 4.2.15.Final | NO | GHSA-4qhr-g3c6-fcfx | HIGH | 4.2.16.Final, 4.1.136.Final |
| netty-codec-xml | 4.2.15.Final | NO | GHSA-v74w-7mr3-4qg3 | HIGH | 4.2.16.Final, 4.1.136.Final |
| commons-beanutils | 1.9.4 | YES | GHSA-wxr5-93ph-8wr9 | HIGH | 1.11.0 |
| grpc-netty-shaded | 1.53.0 | NO | GHSA-prj3-ccx8-p6x4 | HIGH | 1.75.0 |
| grpc-netty-shaded | 1.65.1 | YES | GHSA-prj3-ccx8-p6x4 | HIGH | 1.75.0 |
| httpcore5 | 5.3.6 | YES | GHSA-hf6x-8p5f-cgmf | HIGH | 5.4.3, 5.5-beta2 |
| httpcore5-h2 | 5.3.6 | YES | GHSA-v3jc-474w-2wm6 | HIGH | 5.4.3, 5.5-beta2 |
| ini4j | 0.5.4 | YES | GHSA-jr6h-r7vg-f9mc | HIGH | N/A |
| mcp-core | 0.18.3 | YES | GHSA-8jxr-pr72-r468 | HIGH | 1.0.0 |
| netty-codec-compression | 4.2.15.Final | YES | GHSA-558v-64gr-wgg4 | HIGH | 4.2.16.Final |
| netty-codec-haproxy | 4.2.15.Final | NO | GHSA-q6cq-mhr2-jmr5 | HIGH | 4.2.16.Final, 4.1.136.Final |
| netty-codec-http2 | 4.2.15.Final | YES | GHSA-93wv-jw9v-4972 | HIGH | 4.2.16.Final, 4.1.136.Final |
| netty-codec-http3 | 4.2.15.Final | NO | GHSA-hpcc-26xq-25fv | HIGH | 4.2.16.Final |
| netty-codec-stomp | 4.2.15.Final | NO | GHSA-vhch-2wf3-m8rp | HIGH | 4.2.16.Final, 4.1.136.Final |
| netty-handler | 4.2.15.Final | YES | GHSA-c4c3-7fpv-j4q5 | CRITICAL | 4.2.17.Final, 4.1.137.Final |
| netty-transport-sctp | 4.2.15.Final | NO | GHSA-2qj4-mmr9-4v2f | HIGH | 4.2.17.Final, 4.1.137.Final |
| postgresql | 42.7.11 | YES | GHSA-j92g-9f8w-j867 | HIGH | 42.7.12 |

`Server runtime=YES` 表示组件在当前 `zhicloud-server` runtime scope 依赖树中出现；不代表漏洞可被利用，也不代表可安全升级。

