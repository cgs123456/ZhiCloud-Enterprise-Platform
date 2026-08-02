# 满分（Full-Marks）执行计划

> 目标：将 code-review-report.md 中确认的全部修改方案纳入统一执行计划，逐项落地，
> 使各项可量化指标**严格达到满分标准**（非“接近”）。本文档即“完成状态追踪表”，
> 每项含【修改内容 / 执行步骤 / 验收标准 / 状态追踪】四要素。
>
> 状态图例：✅ 已完成　🔧 执行中　⏳ 待执行　⛔ 阻塞（需用户决策）
> 自动化追踪：错误码唯一性门禁、裸抛异常门禁、JaCoCo 覆盖率门禁 作为硬门禁持续守护。

---

## 一、已完成阶段（Phase 1–7，已推送 master）

| 阶段 | 修改内容 | 执行步骤 | 验收标准 | 状态 |
|---|---|---|---|---|
| Phase 1–3 架构与高危项 | 安全/多租户/鉴权基线加固（前序会话完成） | 按评审报告执行并推送 | 相关模块编译通过、已评审 | ✅ |
| Phase 4 测试与覆盖率门禁 | 引入 JaCoCo `check` 绑定 `verify`；补 crm/erp 核心域单测 | `636045c` | `mvn verify` 在 wms/mes/bpm service 包≥0.30 通过 | ✅ |
| Phase 5 错误码唯一性 | `scripts/check_error_codes.py` 门禁；全量去重 140 冲突码（11 文件 149 常量重排） | `1b0bf13`→`d6c6e22` | 全仓错误码 0 冲突（2033 个）；CI 严格门禁绿 | ✅ |
| Phase 6 WMS 写接口鉴权护栏 | `scripts/check_missing_preauthorize.py` 扫描写接口缺 `@PreAuthorize` | `9438a00` | WMS 写接口 100% 含方法级 `@PreAuthorize` | ✅ |
| Phase 7 可观测性 | logback JSON 结构化日志 + Prometheus 告警规则 | `a55c4b7` | 日志 JSON 化；`/actuator/prometheus` 暴露关键指标 | ✅ |

---

## 二、待执行关键项（满分必达）

### Item 8 — 裸抛异常统一为 `ServiceException(ErrorCode)`　🔧

**背景**：code-review-report.md 第 35 行——“裸抛异常 79 处（`RuntimeException`/`IllegalStateException`），绕过 `ErrorCode` 体系，被 `GlobalExceptionHandler` 兜底成 500 且带原始堆栈；错误信息泄露、前端无法友好提示”。满分要求：全部统一。

**修改内容**
1. `ServiceException` 增加两个构造函数，复用全局 `INTERNAL_SERVER_ERROR(500)`：
   - `ServiceException(ErrorCode errorCode, String message)`
   - `ServiceException(ErrorCode errorCode, String message, Throwable cause)`
2. 将 `*/src/main/java` 下 `cn/iocoder/yudao/**` 包内全部 `throw new RuntimeException(...)` / `throw new IllegalStateException(...)` 转换为 `ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, ...)`，并保留原始 message 与 cause。
3. **排除范围**（避免改动重打包外部类、引发不可控回归）：路径包含 `org/springframework/`、`org/flowable/`（含 `yudao-sql/.../flowable-patch`）的 `org.*` 重打包类不转换。
4. 新增 CI 回归门禁 `scripts/check_bare_throws.py`：扫描 `src/main` 残留裸抛（`org/` 与 `yudao-sql` 除外），>0 即失败。

**执行步骤**
1. 编辑 `yudao-framework/yudao-common/.../ServiceException.java` 增加上述两构造函数。
2. 编写 `scripts/convert_bare_throws.py`（平衡括号 + 字符串字面量感知的精确替换；单参 Throwable→`(code, e.getMessage(), e)`，单参字符串→`(code, msg)`，双参 `(msg, e)`→`(code, msg, e)`；自动补 import）。
3. 先 `python3 scripts/convert_bare_throws.py --dry-run` 核对数量与 diff，再正式执行。
4. 编写 `scripts/check_bare_throws.py` 并在 `.github/workflows/maven.yml` 增加步骤。
5. 全量编译（`mvn test-compile`）+ 跑门禁确认 0 残留。
6. commit + push。

**验收标准**
- `grep -rn "throw new RuntimeException\|throw new IllegalStateException" --include=*/src/main/**/*.java` 在 `cn/iocoder/yudao` 包内返回 **0**（排除 org/ 重打包类）。
- `python3 scripts/check_bare_throws.py` 退出码 0。
- 全 reactor `mvn test-compile` **BUILD SUCCESS**。
- 现有 `catch (RuntimeException)`（BpmModelController:152）仍正常捕获（ServiceException 是 RuntimeException 子类）。
- 严格错误码检查仍为 0 冲突。

**状态追踪**：CI 门禁 `check_bare_throws.py` 持续守护；本计划状态列 = 🔧。

---

### Item C — 核心域测试覆盖率门禁 ≥40%　⏳

**背景**：报告第 29 行——启用模块测试覆盖率 ≈4.1%，核心业务（MES/ERP/CRM）近乎零覆盖。满分目标：**核心业务域 line/instruction 覆盖 ≥40%**。

**修改内容**
1. 将根 `pom.xml` 的 `jacoco.minimum` 由 `0.30` 上调至 `0.40`。
2. 扩大 `<includes>`（JaCoCo `check` 执行）至 `crm / erp / wms / mes / bpm / ai / datalake / system / pay` 的 `**/service/**` 包。
3. 为上述模块补充 `BaseDbUnitTest` 用例，使每个被纳入模块 instruction 覆盖 ≥40%（优先 MES/ERP/CRM/WMS 业务校验分支）。

**执行步骤**
1. 编辑 `pom.xml`：`jacoco.minimum=0.40`；`<includes>` 增加各模块 service 包。
2. 逐模块补单测（参照 Phase 4 已落地的 crm/erp 用例模板）。
3. 本地 `mvn verify -Djacoco.minimum=0.40` 验证各模块达标；未达标模块先从 `<includes>` 暂退（带 TODO），达标后纳入。
4. 门禁随单测补齐逐步“点亮”，避免一次性 brick CI。
5. commit + push。

**验收标准**
- `mvn verify` 在纳入 `<includes>` 的每个模块 service 包 instruction 覆盖 **≥40%**。
- 报告附录登记各模块实际覆盖率。

**状态追踪**：JaCoCo `check` 门禁（绑定 `verify`）持续守护；按模块登记覆盖率数字。

---

### Item U-2 / U-7 / U-10 — 用户决策阻塞项　⛔

> 以下三项**无法由工程侧单独闭环**，需用户/业务侧给出结论后才能判定满分。计划列出其修改内容与验收标准，状态=阻塞。

| 项 | 决策内容 | 修改内容（决议后） | 验收标准 |
|---|---|---|---|
| **U-2** AI+BPM 缺表 | 是否依赖外部/上游 SQL 导入流程？全新库部署验证 | 若需自建：补 Flyway 脚本 V75+；若依赖外部：文档固化依赖 | 全新库 `flyway migrate` 0 失败，应用启动无缺表报错 |
| **U-7** 禁用模块 | 48 张剩余缺表对应模块永久下线 or 近期启用 | 下线：从构建/启动排除并文档说明；启用：补表+单测 | 启动日志无缺失模块告警；架构决策记录(ADR)归档 |
| **U-10** Redis 拓扑 | 是否连哨兵/集群？决定单点风险定级 | 集群：补 `spring.redis` 哨兵/集群配置；单点：补降级开关与本地缓存兜底 | 容灾演练：Redis 宕机时非核心路径不雪崩 |

**状态追踪**：本计划状态列 = ⛔；待用户回复后转为 ⏳ 并执行。

---

## 三、完成状态追踪方式（总览）

| 编号 | 项 | 状态 | 硬门禁 |
|---|---|---|---|
| P1–P7 | Phase 1–7 | ✅ | 编译/单测 |
| **Item 8** | 裸抛统一 | 🔧 | `check_bare_throws.py` |
| **Item C** | 覆盖率≥40% | ⏳ | JaCoCo `check` |
| U-2 | AI+BPM 缺表 | ⛔ | —（用户决策） |
| U-7 | 禁用模块 | ⛔ | —（用户决策） |
| U-10 | Redis 拓扑 | ⛔ | —（用户决策） |

> 说明：严格“满分”需 Item 8 + Item C 全绿且 U-2/U-7/U-10 三项用户决策落地。
> 工程侧可在本会话内闭环 Item 8 与 Item C；U 系列需您确认后继续。

---

*文档由执行代理于 2026-08-02 生成，随执行进度持续更新状态列与报告附录。*
