# Maven 许可证 CI 门禁设计（T-107）

- 生成时间：`2026-09-28 00:34:38 +08:00`
- 证据基础：`maven-license-inventory-20260928_0028.json` / `.md`
- 性质：工程门禁设计，不是法律意见，也不表示已完成法务审批。

## 1. 目标

1. 当前 SBOM 保持可重复生成，内部 `cn.zhicloud` 组件必须带 MIT license 元数据。
2. 外部组件不得出现 `MISSING` license 标签。
3. 新增依赖默认 fail-closed；未识别许可证、强 copyleft、custom/restrictive 许可证必须显式审批。
4. `zhicloud-server` runtime scope 的风险优先级高于 test/provided/optional。

## 2. 当前基线

- 总组件：862（internal=36，external=826）。
- external missing license：0。
- external raw labels：48。
- `zhicloud-server` runtime scope：707。
- 需 review 外部组件：14。
- `net.jcip:jcip-annotations:1.0` 已从 runtime 传递路径移除；需要该注解的 `podam` 使用 `com.github.stephenc.jcip:jcip-annotations:1.0-1`（Apache-2.0）cleanroom 实现替代。

## 3. 策略规则

| 类别 | 默认动作 | 说明 |
|---|---|---|
| internal MIT | allow | 仍校验 license 元数据存在且为 MIT。 |
| external permissive_standard | allow | 记录进 SBOM，不需法务逐项审批。 |
| external weak_copyleft | review + record | 允许后必须写入 approved allowlist，包含 purl、用途、scope、审批人和有效期。 |
| external strong_or_restrictive | deny until approved | runtime scope 必须法务审批；未审批前 CI fail。 |
| external custom_or_non_spdx | deny until approved | 即使常见许可证名也不能自动放行。 |
| external expression_or_dual | review | 必须确认选择的许可证分支满足使用场景。 |
| missing / UNMAPPED | deny | 需要上游确认或显式补齐后重扫。 |

## 4. 当前 14 个必须 review 的外部组件

| 组件 | 当前标签 | runtime scope | 处置 |
|---|---|---:|---|
| `com.mysql:mysql-connector-j:9.7.0` | GPL-2.0 with Universal FOSS Exception | 是 | 法务确认 FOSS Exception 适用本项目分发方式；同时评估 MariaDB 驱动作为替代。 |
| `com.oracle.database.spring:oracle-spring-boot-starter-ucp:23.4.0` | Oracle FUTC | 是 | 法务确认 FUTC 分发和再发布边界；若不满足，仅在构建期使用或切换驱动/连接池方案。 |
| `com.oracle.database.jdbc:ojdbc11:23.7.0.25.01` | Oracle FUTC | 是 | 同上。 |
| `com.oracle.database.jdbc:ucp:23.7.0.25.01` | Oracle FUTC | 是 | 同上。 |
| `com.oracle.database.jdbc:ojdbc8:23.7.0.25.01` | Oracle FUTC | 否 | 非 runtime，可低优先级审批；确认无下沉到 runtime。 |
| `io.github.x-stream:mxparser:1.2.2` | Indiana University Extreme! Lab Software License | 是 | 法务确认条款；必要时评估 xpp3 等替代或隔离模块。 |
| `org.bouncycastle:bcpkix-jdk18on:1.80` | Bouncy Castle Licence | 是 | 确认为 MIT-like 后进入 allowlist；继续跟踪 1.80 到 1.84 的升级矩阵。 |
| `org.bouncycastle:bcutil-jdk18on:1.80` | Bouncy Castle Licence | 是 | 同上。 |
| `org.bouncycastle:bcprov-jdk18on:1.80` | Bouncy Castle Licence | 是 | 同上。 |
| `org.bouncycastle:bcjmail-jdk18on:1.84` | Bouncy Castle Licence | 是 | 同上，并确认 1.80/1.84 混用无兼容问题。 |
| `com.github.jai-imageio:jai-imageio-core:1.4.0` | BSD 3-clause with nuclear disclaimer | 是 | 法务确认附加 disclaimer；评估是否可通过图像栈替换/排除。 |
| `org.jdom:jdom2:2.0.6.1` | Apache-like custom text | 是 | 法务确认文本与 Apache-2.0 兼容；通过后进入 allowlist。 |
| `com.github.junrar:junrar:7.6.0` | UnRar License | 是 | 法务确认再发布限制；必要时移除 RAR 能力或隔离到可选模块。 |
| `javax.annotation:javax.annotation-api:1.3.2` | CDDL-1.0 OR GPL-2.0-with-classpath-exception | 是 | 选择 CDDL 分支并记录；确认与 jakarta annotation 依赖无重复路径。 |

## 5. CI 执行流程

1. `mvn -q validate`
2. `mvn -q org.cyclonedx:cyclonedx-maven-plugin:2.9.1:makeAggregateBom`
3. `mvn -pl zhicloud-server dependency:tree -Dscope=runtime`
4. 重新生成 license inventory JSON。
5. 运行 `check_license_policy`（后续落地为脚本）：
   - 读取 BOM、runtime tree、`license-policy.json`。
   - 拒绝 missing/UNMAPPED 和未经审批的 deny/review 组件。
   - 输出 machine-readable JSON，包含 `decision`、`review_required`、`approved_allowlist_hits`。
   - exit=0 才能进入后续构建/发布。
6. 新增依赖 PR 必须包含更新后的 SBOM 和 policy 决策证据。

## 6. 退出条件

- 14 个 review 组件全部获得法务处置：approved、approved-with-exception、replace、exclude 或 isolate。
- CI 门禁可在干净 checkout 上复现，且新增未知组件会 fail。
- 每次升级后重新扫描，不使用过期 allowlist。