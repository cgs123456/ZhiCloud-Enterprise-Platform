# Maven 许可证 CI 门禁决策（T-107）

- 生成时间：`2026-09-28T01:07:48+08:00`
- 门禁脚本：`scripts/production/check_license_policy.py`（本次新增，已实现）
- 策略文件：`scripts/production/license-policy.json`（本次新增，allowlist 为空）
- 输入库存：`maven-license-inventory-20260928_0028.json`（SHA-256 `4a5cc32cfeed85f1…`）
- **决策：`FAIL`（fail-closed，退出码 1）**

## 1. 结论

门禁脚本已落地并可在干净 checkout 上复现；真实库存下必然失败，因为 51 个外部组件（13 deny + 38 review）尚未获得任何法务审批，allowlist 为空。**法务审批未发生就是未发生**，不允许预置 allowlist 或放宽策略把门禁变绿。`T-107` 保持 OPEN。

## 2. 计数

| 指标 | 值 |
|---|---:|
| components | 862 |
| internal | 36 |
| external | 826 |
| allowed | 811 |
| denied | 13 |
| review_required | 38 |
| allowlist_hits | 0 |
| runtime_blocked | 38 |
| invalid_entries | 0 |

对账：36 internal + 775 permissive_standard = 811 allowed；9 strong + 4 custom = 13 denied；37 weak_copyleft + 1 expression_or_dual = 38 review_required；811 + 13 + 38 = 862。

## 3. denied（13，action=deny_until_approved）

| purl | 类别 | runtime | 规则 |
|---|---|:--:|---|
| `pkg:maven/com.mysql/mysql-connector-j@9.7.0?type=jar` | strong_or_restrictive | 是 | LIC-EXT-001 |
| `pkg:maven/com.oracle.database.jdbc/ojdbc8@23.7.0.25.01?type=jar` | strong_or_restrictive | 否 | LIC-EXT-001 |
| `pkg:maven/com.oracle.database.spring/oracle-spring-boot-starter-ucp@23.4.0?type=jar` | strong_or_restrictive | 是 | LIC-EXT-001 |
| `pkg:maven/com.oracle.database.jdbc/ojdbc11@23.7.0.25.01?type=jar` | strong_or_restrictive | 是 | LIC-EXT-001 |
| `pkg:maven/com.oracle.database.jdbc/ucp@23.7.0.25.01?type=jar` | strong_or_restrictive | 是 | LIC-EXT-001 |
| `pkg:maven/io.github.x-stream/mxparser@1.2.2?type=jar` | strong_or_restrictive | 是 | LIC-EXT-001 |
| `pkg:maven/org.bouncycastle/bcpkix-jdk18on@1.80?type=jar` | custom_or_non_spdx | 是 | LIC-EXT-001 |
| `pkg:maven/org.bouncycastle/bcutil-jdk18on@1.80?type=jar` | custom_or_non_spdx | 是 | LIC-EXT-001 |
| `pkg:maven/org.bouncycastle/bcprov-jdk18on@1.80?type=jar` | custom_or_non_spdx | 是 | LIC-EXT-001 |
| `pkg:maven/com.github.jai-imageio/jai-imageio-core@1.4.0?type=jar` | strong_or_restrictive | 是 | LIC-EXT-001 |
| `pkg:maven/org.bouncycastle/bcjmail-jdk18on@1.84?type=jar` | custom_or_non_spdx | 是 | LIC-EXT-001 |
| `pkg:maven/org.jdom/jdom2@2.0.6.1?type=jar` | strong_or_restrictive | 是 | LIC-EXT-001 |
| `pkg:maven/com.github.junrar/junrar@7.6.0?type=jar` | strong_or_restrictive | 是 | LIC-EXT-001 |

## 4. review_required（38，需 record / 确认分支）

| purl | 类别 | runtime | action |
|---|---|:--:|---|
| `pkg:maven/org.aspectj/aspectjweaver@1.9.25.1?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/jakarta.servlet/jakarta.servlet-api@6.0.0?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/ch.qos.logback/logback-classic@1.5.34?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/ch.qos.logback/logback-core@1.5.34?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/jakarta.annotation/jakarta.annotation-api@2.1.1?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/org.junit.jupiter/junit-jupiter@5.12.2?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/org.junit.jupiter/junit-jupiter-api@5.12.2?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/org.junit.platform/junit-platform-commons@1.12.2?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/org.junit.jupiter/junit-jupiter-params@5.12.2?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/org.junit.jupiter/junit-jupiter-engine@5.12.2?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/org.junit.platform/junit-platform-engine@1.12.2?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/com.google.code.findbugs/annotations@3.0.1?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/com.github.jsqlparser/jsqlparser@5.2?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/com.rabbitmq/amqp-client@5.25.0?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/org.javassist/javassist@3.21.0-GA?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/com.h2database/h2@2.3.232?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/org.javassist/javassist@3.28.0-GA?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/org.eclipse.angus/jakarta.mail@2.0.5?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/com.xkcoding.http/simple-http@1.0.5?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/com.xkcoding.justauth/justauth-spring-boot-starter@1.4.0?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/org.eclipse.angus/angus-mail@2.0.5?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/jakarta.mail/jakarta.mail-api@2.1.5?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/net.java.dev.jna/jna@5.13.0?type=jar` | weak_copyleft | 否 | review_and_record |
| `pkg:maven/net.java.dev.jna/jna-platform@5.17.0?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/org.codelibs/jhighlight@1.1.1?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/com.github.albfernandez/juniversalchardet@2.5.0?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/dev.tinyflow/tinyflow-java-core@1.2.6?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/javax.annotation/javax.annotation-api@1.3.2?type=jar` | expression_or_dual | 是 | review |
| `pkg:maven/junit/junit@4.13.2?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/org.eclipse.parsson/parsson@1.1.6?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/jakarta.json/jakarta.json-api@2.1.3?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/jakarta.json.bind/jakarta.json.bind-api@3.0.2?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/org.eclipse/yasson@3.0.4?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/org.eclipse.collections/eclipse-collections-api@11.1.0?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/org.eclipse.collections/eclipse-collections-forkjoin@11.1.0?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/org.eclipse.collections/eclipse-collections@11.1.0?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/net.java.dev.jna/jna@5.14.0?type=jar` | weak_copyleft | 是 | review_and_record |
| `pkg:maven/org.jgrapht/jgrapht-core@1.4.0?type=jar` | weak_copyleft | 是 | review_and_record |

## 5. 语义标志

- `gate3_passed` = `False`
- `independent_review` = `False`
- `production_ready` = `False`
- `t107_status` = `OPEN`

## 6. 局限

- 门禁只判断策略命中，不做法律定性；放行不等于合规。
- 审批未发生时 allowlist 必须为空，门禁必然 fail-closed。
- 依赖升级后必须重扫 inventory 并重新审批，不得沿用过期 allowlist。

