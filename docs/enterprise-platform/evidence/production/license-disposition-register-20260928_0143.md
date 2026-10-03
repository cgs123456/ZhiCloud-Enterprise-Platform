# Maven 组件许可证处置台账（法务待办）

- 生成时间（本地）：2026-09-28T01:43:52+08:00
- 门禁输入：`docs\enterprise-platform\evidence\production\license-policy-gate-20260928_0107.json`
- 门禁输入 SHA256：`372d3b925b386db8330b3c91056931c67875305a70337e21c86ee30a079520c5`
- 门禁判定：`fail`
- 阻断项合计：**51**（deny_until_approved 13 + review_and_record 38）
- 处置状态：{"PENDING": 51} —— 全部 PENDING 时台账不解除任何门禁
- 运行时拆分：在服务端运行时闭包内 **38** 项（随制品分发，优先处置）；不在闭包内 **13** 项（多为测试 / provided / optional 依赖，通常可先评估直接移除）
> 注意：运行时拆分只用于排定处置优先级，不代表不在闭包内的组件无需处置 —— 源码分发、测试代码与构建产物同样可能触发许可证义务。

> 本台账由 `scripts/production/build_license_disposition_register.py` 生成，
> 只做工程整理，不构成法律意见，也不宣称任何许可证已获批准。

## 一、待法务裁定（工程无权自行决定）

1. 弱 Copyleft 的阻断强度：现行 policy 把 weak_copyleft 计为阻断（review_and_record），这是本台账 51 项与设计文档 14 项的差异来源；需法务裁定是维持阻断还是降级为仅记录。
2. GPL-2.0-only / GPL-3.0-only 的分类争议：二者当前被归为 weak_copyleft，通常应按强 Copyleft 处理，需法务裁定并同步修正映射表。
3. MulanPSL-2.0 未映射：归一结果为 UNMAPPED: 却被计为 permissive_standard，需补映射并复核该判定是否成立。
4. netty-tcnative-boringssl-static 静态链接 BoringSSL 原生代码：Maven 元数据声明 Apache-2.0，但内含第三方原生代码的许可证义务需单独确认。

## 二、deny_until_approved（未经批准禁止引入）（13 项）

| # | 组件（group:artifact:version） | 类别 | 运行时 | 策略动作 | 处置 | 批准人 | 批准日期 | 有效期至 | 工程提示 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `org.bouncycastle:bcjmail-jdk18on:1.84` | custom_or_non_spdx | 是 | deny_until_approved | PENDING | — | — | — | — |
| 2 | `org.bouncycastle:bcpkix-jdk18on:1.80` | custom_or_non_spdx | 是 | deny_until_approved | PENDING | — | — | — | — |
| 3 | `org.bouncycastle:bcprov-jdk18on:1.80` | custom_or_non_spdx | 是 | deny_until_approved | PENDING | — | — | — | — |
| 4 | `org.bouncycastle:bcutil-jdk18on:1.80` | custom_or_non_spdx | 是 | deny_until_approved | PENDING | — | — | — | — |
| 5 | `com.github.jai-imageio:jai-imageio-core:1.4.0` | strong_or_restrictive | 是 | deny_until_approved | PENDING | — | — | — | — |
| 6 | `com.github.junrar:junrar:7.6.0` | strong_or_restrictive | 是 | deny_until_approved | PENDING | — | — | — | — |
| 7 | `com.mysql:mysql-connector-j:9.7.0` | strong_or_restrictive | 是 | deny_until_approved | PENDING | — | — | — | — |
| 8 | `com.oracle.database.jdbc:ojdbc11:23.7.0.25.01` | strong_or_restrictive | 是 | deny_until_approved | PENDING | — | — | — | — |
| 9 | `com.oracle.database.jdbc:ojdbc8:23.7.0.25.01` | strong_or_restrictive | 否 | deny_until_approved | PENDING | — | — | — | 不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 10 | `com.oracle.database.jdbc:ucp:23.7.0.25.01` | strong_or_restrictive | 是 | deny_until_approved | PENDING | — | — | — | — |
| 11 | `com.oracle.database.spring:oracle-spring-boot-starter-ucp:23.4.0` | strong_or_restrictive | 是 | deny_until_approved | PENDING | — | — | — | — |
| 12 | `io.github.x-stream:mxparser:1.2.2` | strong_or_restrictive | 是 | deny_until_approved | PENDING | — | — | — | — |
| 13 | `org.jdom:jdom2:2.0.6.1` | strong_or_restrictive | 是 | deny_until_approved | PENDING | — | — | — | — |

完整 purl 见同名 `.json`（表格按坐标展示，purl 以 JSON 为准）。

## 三、review_and_record（弱 Copyleft / 需逐项登记）（38 项）

| # | 组件（group:artifact:version） | 类别 | 运行时 | 策略动作 | 处置 | 批准人 | 批准日期 | 有效期至 | 工程提示 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | `javax.annotation:javax.annotation-api:1.3.2` | expression_or_dual | 是 | review | PENDING | — | — | — | — |
| 2 | `ch.qos.logback:logback-classic:1.5.34` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 3 | `ch.qos.logback:logback-core:1.5.34` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 4 | `com.github.albfernandez:juniversalchardet:2.5.0` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 5 | `com.github.jsqlparser:jsqlparser:5.2` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 6 | `com.google.code.findbugs:annotations:3.0.1` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 7 | `com.h2database:h2:2.3.232` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 8 | `com.rabbitmq:amqp-client:5.25.0` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 9 | `com.xkcoding.http:simple-http:1.0.5` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；分类争议：纯 GPL（无 classpath exception）当前归为 weak_copyleft，通常应按强 Copyleft 处理，需法务裁定 |
| 10 | `com.xkcoding.justauth:justauth-spring-boot-starter:1.4.0` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；分类争议：纯 GPL（无 classpath exception）当前归为 weak_copyleft，通常应按强 Copyleft 处理，需法务裁定 |
| 11 | `dev.tinyflow:tinyflow-java-core:1.2.6` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；分类争议：纯 GPL（无 classpath exception）当前归为 weak_copyleft，通常应按强 Copyleft 处理，需法务裁定 |
| 12 | `jakarta.annotation:jakarta.annotation-api:2.1.1` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 13 | `jakarta.json:jakarta.json-api:2.1.3` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 14 | `jakarta.json.bind:jakarta.json.bind-api:3.0.2` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 15 | `jakarta.mail:jakarta.mail-api:2.1.5` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 16 | `jakarta.servlet:jakarta.servlet-api:6.0.0` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 17 | `junit:junit:4.13.2` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 18 | `net.java.dev.jna:jna:5.13.0` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 19 | `net.java.dev.jna:jna:5.14.0` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 20 | `net.java.dev.jna:jna-platform:5.17.0` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 21 | `org.aspectj:aspectjweaver:1.9.25.1` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 22 | `org.codelibs:jhighlight:1.1.1` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 23 | `org.eclipse:yasson:3.0.4` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 24 | `org.eclipse.angus:angus-mail:2.0.5` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 25 | `org.eclipse.angus:jakarta.mail:2.0.5` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 26 | `org.eclipse.collections:eclipse-collections:11.1.0` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 27 | `org.eclipse.collections:eclipse-collections-api:11.1.0` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 28 | `org.eclipse.collections:eclipse-collections-forkjoin:11.1.0` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 29 | `org.eclipse.parsson:parsson:1.1.6` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 30 | `org.javassist:javassist:3.21.0-GA` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 31 | `org.javassist:javassist:3.28.0-GA` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 32 | `org.jgrapht:jgrapht-core:1.4.0` | weak_copyleft | 是 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断 |
| 33 | `org.junit.jupiter:junit-jupiter:5.12.2` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 34 | `org.junit.jupiter:junit-jupiter-api:5.12.2` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 35 | `org.junit.jupiter:junit-jupiter-engine:5.12.2` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 36 | `org.junit.jupiter:junit-jupiter-params:5.12.2` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 37 | `org.junit.platform:junit-platform-commons:1.12.2` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |
| 38 | `org.junit.platform:junit-platform-engine:1.12.2` | weak_copyleft | 否 | review_and_record | PENDING | — | — | — | 阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断；不在服务端运行时闭包内（构建/测试/可选依赖）：可优先考虑直接移除依赖，这是最快解除阻断的路径 |

完整 purl 见同名 `.json`（表格按坐标展示，purl 以 JSON 为准）。

## 四、处置流程

1. 法务（或授权人）逐项填写 `disposition`：`approve` / `reject` / `replace`，
   并补齐 `approved_by`、`approved_on`、`expires_on`（建议不超过 365 天）、`scope`。
2. 工程按结论执行：`reject` / `replace` 的组件必须真正从依赖树里移除，
   不能只改台账文字——移除后重跑 inventory，该 purl 不再出现才算闭环。
3. `approve` 的条目由工程写入 `scripts/production/license-policy.json` 的 `allowlist`，
   字段与台账一一对应；allowlist 在审批发生前必须为空。
4. 重跑 `check_license_policy.py`；依赖升级后必须重扫并重新审批，不得沿用过期结论。

## 五、禁止事项

- 台账是工程脚手架，不是法律意见；每行的处置结论必须由法务或授权人填写。
- 台账转绿不等于合规：放行仅表示该项已按内部策略完成审批与留痕。
- 依赖升级后必须重扫 inventory 并重新审批，不得沿用过期结论。
- 不得为让门禁变绿而预填台账、预填 allowlist、放宽 policy，
  或把 deny 类别改成 permissive —— 那等于取消门禁。

- 语义标记：{"production_ready": false, "gate3_passed": false, "t107_status": "OPEN", "independent_review": false}

