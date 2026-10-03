# Maven 许可证治理清单（T-107）

## 结论

- 生成时间：`2026-09-27T23:50:56+08:00`。本清单是工程治理证据，不是法律意见。

- 源 BOM 共 **863** 个组件：内部 `cn.zhicloud` 组件 **36**，外部组件 **827**。

- **先前“832 个 UNKNOWN 许可证”的口径错误**。原始 BOM 实际是：内部组件 **36** 个缺少 license 元数据；外部组件 **827** 个中只有 **1** 个缺少 license 标签；外部共 **49** 个不同 raw label。

- `708` 个组件进入 `zhicloud-server` runtime scope；外部/全部组件按类别分布见下表。

## 组件类别

| 范围 | permissive_standard | weak_copyleft | strong_or_restrictive | custom_or_non_spdx | expression_or_dual | missing |
|---|---:|---:|---:|---:|---:|---:|
| 全部 | 775 | 37 | 9 | 4 | 1 | 37 |
| 外部 | 775 | 37 | 9 | 4 | 1 | 1 |
| server runtime | 637 | 25 | 8 | 4 | 1 | 33 |

## 需优先治理

### 1. 内部 POM 缺少 license 元数据

- 数量：36。

- 处置：在根 POM 和父 POM 增加 `<licenses>`、`<name>`、`<url>` 等元数据；重新生成 SBOM 验证 36 个内部组件不再 missing。

### 2. 外部缺 license 标签

- 数量：1。

- 组件：`net.jcip:jcip-annotations:1.0`。

- 处置：从上游发行包/POM/项目主页确认许可证，写入内部许可库和 SBOM 策略。

### 3. strong/restrictive/custom/expression 组件

- 数量：15。

- 处置：先区分 runtime/test/optional；对 runtime 中的强 copyleft、Oracle FUTC、UnRar 和不可识别 custom 许可证做法务评审，或替换/隔离。

| 分类 | server runtime | 组件 | raw license |
|---|---|---|---|
| `custom_or_non_spdx` | YES | `org.bouncycastle:bcjmail-jdk18on:1.84` | `Bouncy Castle Licence` |
| `custom_or_non_spdx` | YES | `org.bouncycastle:bcpkix-jdk18on:1.80` | `Bouncy Castle Licence` |
| `custom_or_non_spdx` | YES | `org.bouncycastle:bcprov-jdk18on:1.80` | `Bouncy Castle Licence` |
| `custom_or_non_spdx` | YES | `org.bouncycastle:bcutil-jdk18on:1.80` | `Bouncy Castle Licence` |
| `expression_or_dual` | YES | `javax.annotation:javax.annotation-api:1.3.2` | `(CDDL-1.0 OR GPL-2.0-with-classpath-exception)` |
| `missing` | YES | `net.jcip:jcip-annotations:1.0` | `MISSING` |
| `strong_or_restrictive` | YES | `com.github.jai-imageio:jai-imageio-core:1.4.0` | `BSD 3-clause License w/nuclear disclaimer` |
| `strong_or_restrictive` | YES | `com.github.junrar:junrar:7.6.0` | `UnRar License` |
| `strong_or_restrictive` | YES | `com.mysql:mysql-connector-j:9.7.0` | `The GNU General Public License, v2 with Universal FOSS Exception, v1.0` |
| `strong_or_restrictive` | YES | `com.oracle.database.jdbc:ojdbc11:23.7.0.25.01` | `Oracle Free Use Terms and Conditions (FUTC)` |
| `strong_or_restrictive` | NO | `com.oracle.database.jdbc:ojdbc8:23.7.0.25.01` | `Oracle Free Use Terms and Conditions (FUTC)` |
| `strong_or_restrictive` | YES | `com.oracle.database.jdbc:ucp:23.7.0.25.01` | `Oracle Free Use Terms and Conditions (FUTC)` |
| `strong_or_restrictive` | YES | `com.oracle.database.spring:oracle-spring-boot-starter-ucp:23.4.0` | `Oracle Free Use Terms and Conditions (FUTC)` |
| `strong_or_restrictive` | YES | `io.github.x-stream:mxparser:1.2.2` | `Indiana University Extreme! Lab Software License` |
| `strong_or_restrictive` | YES | `org.jdom:jdom2:2.0.6.1` | `Similar to Apache License but with the acknowledgment clause removed` |

### 4. CI 许可证门禁

- 建议把本清单转成 allow/deny/review 策略：标准 permissive 自动允许；weak copyleft 记录；strong/restrictive/custom 阻断或强制 review。

- 在 CI 重新生成 CycloneDX BOM 后跑策略；新增未映射许可证默认 fail-closed。

## JSON 完整性

- SHA-256：`1FA9F0022177B2861AD323AA379FFF26E41455B035B84F68351BF0EDB39DDD8C`

- Sidecar：`maven-license-inventory-20260927_2347.json.sha256`
