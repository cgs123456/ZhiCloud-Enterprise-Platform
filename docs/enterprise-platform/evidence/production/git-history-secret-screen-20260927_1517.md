# Git 历史 Secret 初筛（T-107）

## 结论

- 本报告是 **regex 初筛和可复核证据**，不是完整 Secret 扫描器；不能证明“无 Secret”，也不能证明命中都是真实泄漏。

- 扫描 `108` 个历史提交的新增行：命中 **445** 条（HIGH=20，CANDIDATE=425）。

- HIGH 中 **18 条 JWT-like 值** 在扫描时间已过期；解码字段显示 7 个唯一 payload 组合，涉及 IoT 测试上下文的 device/product 字段。过期 JWT 不能再用作访问令牌，但硬编码 token 仍是坏实践，建议改成测试期生成并在用例结束撤销。

- HIGH 中 **2 条为同一硬编码 Google API Key 模式**（历史路径和重命名路径各 1 条；当前重命名路径仍存在）。未调用或验证该 Key，不能判断是否真实有效；必须由所有者确认。若真实，立即在 Google Cloud 控制台禁用/轮换，并检查账单、日志和配额。

- CANDIDATE 未确认 live credential。自动分诊为：reference/env placeholder **195**、known placeholder/default **22**、test fixture **51**、需人工复核 **143**、高熵需人工复核 **14**。

- 当前 HEAD 精确行命中：**277 / 445**。该指标用于优先级排序；旧路径重命名会导致历史重复路径显示 false，不表示内容已消失。

## 证据边界

- 扫描对象：`git log --all -p --unified=0 --no-renames --no-textconv --no-ext-diff`。

- 扫描范围：全部 refs 历史提交的新增行；**未扫描**工作区未提交文件、二进制 blob、CI 日志、容器镜像层、备份和对象存储。

- 隐私处理：JSON 不保存疑似 Secret 明文，只保存规则、路径、行号、提交、整行 SHA-256和非敏感元数据（如 JWT exp/device/product）。

- 本初筛不替代 gitleaks/trufflehog；后续应把专用扫描器加入 CI 并阻断新增命中。

## HIGH 摘要

| 类型 | 数量 | 当前处置 |
|---|---:|---|
| Google API Key 模式 | 2 | 所有者确认；若真实则禁用/轮换并审计使用 |
| JWT-like token | 18 | 扫描时间已过期；改为测试期生成/撤销，不作为生产凭据 |

## 候选分诊

| 分诊 | 数量 | 含义 |
|---|---:|---|
| reference_or_env_placeholder | 195 | 环境变量、配置引用、模板占位符 |
| known_placeholder_or_default | 22 | 明显样例/默认/占位值 |
| test_fixture_needs_review | 51 | 测试文件上下文，仍需确认 |
| needs_manual_review | 143 | 未归类，需按路径和上下文复核 |
| needs_manual_review_high_entropy | 14 | 高熵值，优先人工复核 |

## JSON 完整性

- SHA-256：`5A7CA43AC196A507D7CCA42F8DCA96A1439163723C1F5A2F912203BBF5D76EEE`

- 生成时间：`2026-09-27T23:38:44+08:00`

- Sidecar：`git-history-secret-screen-20260927_1517.json.sha256`

## 后续动作

1. 由平台安全所有者确认 Google API Key；不要在聊天中粘贴明文。

2. 将 IoT 测试 JWT 改为测试运行时生成；若对应 product/device 凭据仍有效，一并轮换。

3. 优先人工复核 14 条高熵候选，再复核 143 条普通候选；确认真实凭据后吊销/轮换，并保留处置记录。

4. 引入 gitleaks/trufflehog 到 CI，补扫工作区、镜像层、CI 日志、备份和对象存储。

5. 若决定重写 Git 历史，必须评估 force push、协作者、分支/标签、CI 缓存和 fork 的影响；吊销凭据是第一优先级。
