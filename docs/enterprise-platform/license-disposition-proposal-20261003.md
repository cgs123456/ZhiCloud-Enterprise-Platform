# 许可证待签清单（License Policy Gate 判红处置提案）

生成时间：2026-10-03。触发点：CI 的 `License Policy Gate (fail-closed)` 对 7 个组件判 `LIC-EXT-002 DENY`，
全部由本轮启用 `report/pay` 等模块后进入运行时依赖树。**本文件只列证据与建议，不做合规结论**；
每一项都需要所有者签字后我才写进 `scripts/production/license-policy.json` 的 `approved_decisions`/`allowlist`。

取证口径：优先看**实际进包的 jar**（`~/.m2/repository/**/META-INF/LICENSE*`、`MANIFEST.MF` 的 `Bundle-License`），
其次看 Maven Central 上的 POM `<licenses>`，最后才参考上游仓库。没取到的一律标 `未证实`，不当作"应该没问题"。

## 一、判红组件与证据

| # | 组件 | 进入路径 | 已取到的证据 | 结论强度 | 建议 |
|---|---|---|---|---|---|
| 1 | `dom4j:dom4j:1.6.1` | `zhicloud-module-pay` 的依赖树 | jar 内 `META-INF/LICENSE.txt` 首段为 `Copyright 2001-2005 (C) MetaStuff, Ltd. … Redistribution and use …`（BSD 风格） | **中**：文本在包里，但 POM 无 `<licenses>`，且这是 2005 年代的旧坐标 | 优先方案：改依赖 `org.dom4j:dom4j`（本仓库 `system` 模块已在用 **2.1.3**，属 BSD-类许可的新坐标），旧坐标用 `<exclusions>` 去掉，可**直接消除这一项**而不用豁免 |
| 2 | `javax.xml.bind:jaxb-api:2.3.1` | 传递依赖 | jar 内 `META-INF/LICENSE.txt` 是 CDDL-1.1 全文；`MANIFEST.MF` 里 `Bundle-License: https://oss.oracle.com/licenses/CDDL+GPL-1.1` | **高**：双重许可 CDDL-1.1 / GPL-2.0+Classpath Exception，属可使用的常见形态 | 建议登记为 `cddl-1.1-or-gpl2-cpe`（许可明确、有包内证据）。仅需你确认"以 CDDL 选项使用" |
| 3 | `org.xhtmlrenderer:core-renderer:R8`（Flying Saucer） | `zhicloud-module-report` 的依赖树 | Central POM 明确声明 `GNU Lesser General Public License (LGPL), version 2.1`，URL `gnu.org/.../lgpl-2.1.html` | **高**（许可类型明确）：**弱 copyleft** | 两个选项：(a) 接受 LGPL-2.1 并补依赖告示（保留 LGPL 文本、声明使用了该库）；(b) 若报表截图/HTML 转图功能不是必需，去掉该链路。**需要你的合规取向** |
| 4 | `gui.ava:html2image:2.0.1` | `zhicloud-module-report` 的依赖树（core-renderer 的父级链路） | ① Central 上该 GAV **404**；② jar 内无任何 LICENSE/NOTICE；③ POM 无 `<licenses>`；④ 本地 `_remote.repositories` 显示它来自 `huaweicloud` 镜像（`pom.xml:268` 声明的仓库），下载时间 2026-10-02 18:08 | **未证实**：许可与来源都取不到硬证据，且**只在镜像里有**（含供应链风险，不只是许可证问题） | 建议**剔除/替换**：该库是早期 Google Code 时代产物，报表 HTML 转图可用已在依赖树里的其他方案替代。若坚持保留，需要你承担"来源不可核实"的风险并显式签字 |
| 5-7 | `com.aliyun:aliyun-java-sdk-core:4.5.10`<br>`-ram:3.1.0`<br>`-kms:2.11.0` | 依赖树内（`report` 模块的树里出现，父链路未逐条确认） | Central POM 有 `<license>` 但 `<name>/<url>` **全为空**；上游仓库 GitHub license API 返回 `NOASSERTION ("Other")`；jar 内无 LICENSE 文件 | **未证实**：机器可读证据不足（社区普遍按 Apache-2.0 看待，但我没有取到可引用的权威声明） | 若本部署不使用阿里云短信/KMS/RAM 通道，建议 `<exclusions>` 或标 `optional`，**问题直接消失**；若要用，我再去找官方 LICENSE 全文取证后请你签字 |

补充事实（影响可信度，一并写明）：`settings.xml` 在本机不存在，`huaweicloud` 仓库 id 来自**仓库根 `pom.xml:268`**，
所以这不是我这台机器的特例，CI 也走同一份声明；但 CI 的 inventory 与本地取到的坐标集合一致，
说明判红不是镜像差异造成的假象。

## 二、我建议的处置顺序（等你逐项签字）

1. `pay` 的 `dom4j:1.6.1` → 换 `org.dom4j:dom4j:2.1.3` 或 `<exclusions>`：**无需任何豁免**即可消红（最干净）。
2. `report` 的 `html2image` → 剔除/替换：既解决许可未证实，也解决"Central 没有、只有镜像有"的供应链问题。
3. 阿里云三个 SDK → 确认本部署是否真的用到；不用则 exclude，问题消失。
4. `core-renderer` LGPL-2.1 → 只剩这一项是真正的"接受弱 copyleft"取向问题，需要你定。
5. `jaxb-api` → 证据充分，建议直接登记（CDDL-1.1 选项）。

也就是说：**7 项里预计 5 项可以靠"不引入未核实依赖"消掉**，只有 1 项需要你表达合规取向、1 项需要确认证据。

## 三、签字栏

| 组件 | 你的决定（接受 / 剔除 / 换坐标 / 继续核实） | 备注 |
|---|---|---|
| `dom4j:dom4j:1.6.1` | ☐ | 建议：换 `org.dom4j:dom4j` |
| `javax.xml.bind:jaxb-api:2.3.1` | ☐ | 建议：按 CDDL-1.1 登记 |
| `org.xhtmlrenderer:core-renderer:R8` | ☐ | LGPL-2.1，取向问题 |
| `gui.ava:html2image:2.0.1` | ☐ | 许可与来源均未证实 |
| `com.aliyun:aliyun-java-sdk-core/ram/kms` | ☐ | 是否真的用到阿里云通道 |

在拿到签字前，我**不会**改 `license-policy.json`，master 的 CI 会继续保持这一项红；
其余门禁（裸抛、迁移契约、重复 DDL、错误码、事务原子性、会员端授权静态守卫）都按实测保持绿。
