# Maven 许可证治理清单（T-107）

## 结论

- 生成时间：`2026-09-28T00:28:56+08:00`。本清单是工程治理证据，不是法律意见。

- 源 BOM 共 **862** 个组件：内部 `cn.zhicloud` 组件 **36**，外部组件 **826**。

- 内部组件 **0** 个缺少 license 元数据；外部组件 **826** 个中 **0** 个缺少 license 标签；外部共 **48** 个不同 raw label。

- `707` 个组件进入 `zhicloud-server` runtime scope；外部/全部组件按类别分布见下表。

## 组件类别

| 范围 | permissive_standard | weak_copyleft | strong_or_restrictive | custom_or_non_spdx | expression_or_dual | missing |
|---|---:|---:|---:|---:|---:|---:|
| 全部 | 811 | 37 | 9 | 4 | 1 | 0 |
| 外部 | 775 | 37 | 9 | 4 | 1 | 0 |
| server runtime | 669 | 25 | 8 | 4 | 1 | 0 |

## 需优先治理

### 1. 内部 POM 缺少 license 元数据

- 数量：0。

- 处置：在根 POM 和父 POM 增加 `<licenses>`、`<name>`、`<url>` 等元数据；重新生成 SBOM 验证 36 个内部组件不再 missing。

### 2. 外部缺 license 标签

- 数量：0。

- 组件：。

- 处置：从上游发行包/POM/项目主页确认许可证，写入内部许可库和 SBOM 策略。

### 3. strong/restrictive/custom/expression 组件

- 数量：14。

- 处置：先区分 runtime/test/optional；对 runtime 中的强 copyleft、Oracle FUTC、UnRar 和不可识别 custom 许可证做法务评审，或替换/隔离。

### 4. CI 许可证门禁

- 建议把本清单转成 allow/deny/review 策略：标准 permissive 自动允许；weak copyleft 记录；strong/restrictive/custom 阻断或强制 review。

- 在 CI 重新生成 CycloneDX BOM 后跑策略；新增未映射许可证默认 fail-closed。

## JSON 完整性

- SHA-256：`4A5CC32CFEED85F101EAA150096D77DF3BC90CC1F6BDC07E1A0EFA96F8CDFACD`

- Sidecar：`maven-license-inventory-20260927_2347.json.sha256`
