# Maven 许可证清单生成器复现验证（T-107）

- 生成时间（本地）：2026-09-28 01:27
- 状态：**验证通过（存在一处已知、已解释的差异）**；T-107 仍为 OPEN

## 1. 背景

`maven-license-inventory-20260928_0028.json` 此前由**未入库的一次性脚本**生成，
导致生产部署流水线第 4 步“重新生成 inventory”在干净 checkout 上不可复现。
本次将生成逻辑固化为 `scripts/production/generate_license_inventory.py`，
并回归验证其输出与该基线是否一致。

## 2. 复现命令

```
python scripts/production/generate_license_inventory.py \
  --bom docs/enterprise-platform/evidence/production/bom-maven-aggregate-20260928_0028.json \
  --runtime-tree docs/enterprise-platform/evidence/production/dependency-tree-server-runtime-after-jcip-cleanroom-20260928_0028.log \
  --output <output>.json
```

## 3. 输入/产物 SHA-256

| 对象 | 路径 | SHA-256 |
| --- | --- | --- |
| 生成器脚本 | `scripts/production/generate_license_inventory.py` | `1D773457405B20F0D21CFEF805F6D7E162696F6F46842D78904F7F56A520CC00` |
| 门禁脚本 | `scripts/production/check_license_policy.py` | `8471BEEFF0093C3884F37FA740024A9CF46DFD8A0D476173462FABFE8F2A1B2B` |
| 策略文件 | `scripts/production/license-policy.json` | `845EE46B6B763E5B6B8ADA0AC021E5BFD3323C297BCB5325F9654E327EBC8AE1` |
| 基线 BOM | `docs/enterprise-platform/evidence/production/bom-maven-aggregate-20260928_0028.json` | `95A33DD1AA419157833BE14A8B77C85F379B33677888A5B3B60EA8CA547CBB2E` |
| 基线 runtime tree | `docs/enterprise-platform/evidence/production/dependency-tree-server-runtime-after-jcip-cleanroom-20260928_0028.log` | `26A56098FB6FEEDAB44E5EF572C26D631E0BF927AF39CEFC0C153C44F7D8CB41` |
| 基线 inventory（门禁权威输入） | `docs/enterprise-platform/evidence/production/maven-license-inventory-20260928_0028.json` | `4A5CC32CFEED85F101EAA150096D77DF3BC90CC1F6BDC07E1A0EFA96F8CDFACD` |
| 本次复现 inventory | `docs/enterprise-platform/evidence/production/maven-license-inventory-regen-20260928_0127.json` | `95EF2E1B3540BDCF29C13D015A373A298FAB95D5C5B4389789BD365E71FA9708` |
| 本次复现 inventory 的门禁结果 | `docs/enterprise-platform/evidence/production/license-policy-gate-regen-20260928_0127.json` | `EB6B6E9B3A861288DFE17EB8A606E09F88FCF554453AC5FD253E36255B1AAA06` |
| 回归测试 | `scripts/production/tests/test_generate_license_inventory.py` | `53DEC81361A19E02725E606B8D3C00B9BE1F41ED734C90DA4574282F40149F05` |

## 4. 比对结果

| 维度 | 基线 20260928_0028 | 本次复现 | 结论 |
| --- | --- | --- | --- |
| 组件总数 | 862 | 862 | 一致 |
| internal / external | 36 / 826 | 36 / 826 | 一致 |
| purl 集合 | — | — | 完全一致 |
| 类别桶（全部） | 见基线 | 见基线 | 一致 |
| 类别桶（external） | 见基线 | 见基线 | 一致 |
| review_required（设计口径 14） | 14 | 14 | 一致 |
| **server runtime 组件数** | **707** | **719** | **差异 +12，已定位并解释** |
| 门禁 decision | fail | fail | 一致 |
| 门禁 denied / review | 13 / 38 | 13 / 38 | 逐 purl 一致 |
| allowlist 命中 | 0 | 0 | 一致（allowlist 为空） |

### 4.1 唯一差异：12 个 netty classifier 构件

这些构件在 `mvn dependency:tree` 中以 **6 字段**形态出现
（`group:artifact:type:classifier:version:scope`），基线解析器只按 5 字段匹配，因而漏统计。
本次解析器已支持可选 classifier。以下 12 个构件均声明 Apache-2.0、归类 permissive_standard，
**不改变门禁结论**。

- `pkg:maven/io.netty/netty-resolver-dns-native-macos@4.2.15.Final?classifier=osx-aarch_64&type=jar`
- `pkg:maven/io.netty/netty-resolver-dns-native-macos@4.2.15.Final?classifier=osx-x86_64&type=jar`
- `pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=linux-aarch_64&type=jar`
- `pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=linux-x86_64&type=jar`
- `pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=osx-aarch_64&type=jar`
- `pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=osx-x86_64&type=jar`
- `pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=windows-x86_64&type=jar`
- `pkg:maven/io.netty/netty-transport-native-epoll@4.2.15.Final?classifier=linux-aarch_64&type=jar`
- `pkg:maven/io.netty/netty-transport-native-epoll@4.2.15.Final?classifier=linux-riscv64&type=jar`
- `pkg:maven/io.netty/netty-transport-native-epoll@4.2.15.Final?classifier=linux-x86_64&type=jar`
- `pkg:maven/io.netty/netty-transport-native-kqueue@4.2.15.Final?classifier=osx-aarch_64&type=jar`
- `pkg:maven/io.netty/netty-transport-native-kqueue@4.2.15.Final?classifier=osx-x86_64&type=jar`

反向差异（仅基线有、本次没有）：无

## 5. 回归测试

`scripts/production/tests/test_generate_license_inventory.py`（30 项）将上述差异固化为断言：

- 复现计数 862 / 36 / 826 与基线一致；
- `runtime_delta == 12 个已知 netty purl`，`基线 runtime - 本次 runtime == 空集`；
- 两次 inventory 经 `check_license_policy.py` 的 denied / review purl 集合逐项相等；
- 未知标签 → `UNMAPPED:<raw>` / `unmapped` → 门禁 deny（fail-closed）；
- 缺失许可证 → `MISSING` / `missing`，internal 与 external 分别记账；
- 多许可证优先级：expression > strong > weak > permissive；
- 5 字段 / 6 字段坐标解析、internal 前缀覆盖（大小写不敏感）；
- BOM 缺失 / 非 CycloneDX / JSON 非法 / tree 缺失 一律 exit=2。

全量 Python 门禁：`352 passed, 3 skipped, 152 subtests passed`（3 项 skip 为依赖 Docker 的容器构建检查）。

## 6. 结论与限制

1. **CI 第 4 步现已可复现**：生成逻辑入库，输入与命令可追溯。
2. **基线 `maven-license-inventory-20260928_0028.json` 仍是门禁权威输入**；
   `maven-license-inventory-regen-20260928_0127.json` 仅作为复现性证据，不替换基线。
3. **门禁结论未变**：decision=fail，denied=13、review_required=38、allowlist 命中=0。
   `production_ready=false` / `gate3_passed=false` / `T-107=OPEN` 全部保持不变。
4. **未获法务审批前不得放行**：51 个外部组件（13 deny + 38 review）仍未处置；
   其中 37 个 weak_copyleft 是因为策略将 `weak_copyleft` 设为 blocking（设计文档口径为 14，偏严）。
5. **待裁决的归类争议**（不在脚本内静默变更）：
   - `GPL-2.0-only` / `GPL-3.0-only` 被基线归类 weak_copyleft，通行理解属强 copyleft；
   - `MulanPSL-2.0` 归一为 `UNMAPPED:` 却归类 permissive_standard；
   - `netty-tcnative-boringssl-static` 声明 Apache-2.0，但静态链接内嵌 BoringSSL 原生代码，
     第三方原生代码许可需法务确认。
6. 本文件为工程证据，不构成法律意见。

