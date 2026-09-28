# Gitleaks 专用扫描证据（T-107）

- 生成时间：`2026-09-28T00:32:50+08:00`

- 工具：`gitleaks 8.30.1 windows_x64`，默认规则集，`--redact=100`。

- 工作区终态扫描：**108** 条命中（rules：`{'gcp-api-key': 1, 'generic-api-key': 98, 'jwt': 9}`）。

- Git 历史交叉扫描：**67** 条命中（rules：`{'gcp-api-key': 2, 'generic-api-key': 47, 'jwt': 18}`；unique commits=3）。

- 两份 JSON 的 `Secret` 字段均已检查为 `REDACTED`；AIza / eyJ 明文模式命中均为 0。

- 该扫描补足了先前 regex 初筛，但不能替代镜像层、CI 日志、备份和对象存储扫描；这些仍需外部环境。

- 结论：**T-107 保持 OPEN**，命中需逐项 triage 和轮换/例外审批。

## 报告完整性

- Gitleaks 工作区 JSON：`docs/enterprise-platform/evidence/production/gitleaks-worktree_dir-20260928_0032.json`，SHA-256 `6D653A62522206D4C97F5530D9E67FEF1E60E42AD17ED1C96F3FFEBD5C819A1A`

- Gitleaks Git 历史 JSON：`docs/enterprise-platform/evidence/production/gitleaks-git_history-20260928_0032.json`，SHA-256 `FBDF493EB130D13D66BB85837C81705893A63C0E71B237FBC1BB9D95C4AE55D3`

- 本摘要 JSON SHA-256：`A909C12842869C7FB890DAF2067627FC39D1C017402BD3E49075F1A8BB09701B`
