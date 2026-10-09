# 216 条 P1 安全基线发现 —— 处置台账（待人工裁决）

- 生成日期：2026-09-28
- 证据源：`docs/enterprise-platform/evidence/production/security-baseline-20260928_151500.json`
- 工具：`scripts/production/security_baseline_scan.py`（工作区静态文本扫描）
- 分支 `upgrade/production-readiness` / HEAD `553bc78021b535916eb8202968218faac9bf7755`
- 关联：`docs/enterprise-platform/12-风险登记册.md` R-022、R-030、R-031

---

## 0. 这份台账是什么，不是什么

**是**：216 条 P1 的**待裁决清单** —— 逐条给出「为什么它是 P1、为什么它没有阻断流水线、建议谁在什么期限内拿出什么证据」。

**不是**：

1. **不是已处置清单。** 216 条全部为 OPEN，无一具备 `owner` / `approved_by` / `expires_on` / `evidence`。证据 JSON 里 `suppression.applied=0`、`entries=0`、`enabled=false`。
2. **不是 suppression 输入。** 见 §5，禁止批量回填。
3. **不是「干净」的证据。** 见 §1.3：P0=0 是路径启发式的结果，可被文件改名的动作逆转。

本台账**不改变任何标记位**：`production_ready=false`、`gate3_passed=false`、`R-011=OPEN`、`R-029=OPEN`、`R-030=OPEN`、`R-031=OPEN` 全部保持原状。

---

## 1. 口径与边界（读代码确认，不依赖文档描述）

### 1.1 扫描的是工作区，不是 commit

`security_baseline_scan.py:144` `iter_files()` 用 `repo.rglob("*")`，即**直接读磁盘上的当前工作区**；`:162` 用 `path.read_text()`。
JSON 里的 `commit=553bc780…` 只来自 `:314 git_head()` 的一句 `git rev-parse HEAD`，是一个**并列记录的标签，不是溯源保证**。

因此证据中「对 commit 553bc780 扫描」的表述**不成立**：实际是对「扫描那一刻的工作区」扫描。这是交接文档的既有问题（上一轮已提），此处再确认一次，因为它决定了下一节。

### 1.2 台账里的行号已经漂移，复核者会被骗

扫描 `generated_at = 2026-09-28T15:14:21+08:00`。在 20 个命中文件中，唯一在扫描之后被修改的是：

| 文件 | mtime | 相对扫描 |
|---|---|---|
| `zhicloud-server/src/main/resources/application.yaml` | 2026-09-28 17:03:00 | 晚 1h49m |

其改动为 `@@ -111,0 +112,2 @@ flowable:` —— 在第 111 行之后插入 2 行。后果：

- 台账中 application.yaml 的 19 个行号是 **15:14 时的坐标，等于 HEAD 坐标**；
- 用**当前工作区**核对时，第 111 行之后的行需 **+2**。
- 例：台账记 `216`，当前实际在第 `218` 行（`api-key: ${OPENAI_API_KEY:sk-xxxx}`）；台账记 `393`，当前在 `395`（`api-key: ${WEB_SEARCH_API_KEY:sk-xxxx}`）。

其余 19 个文件的 mtime 均早于扫描时刻，其行号暂时可信 —— 但这个「暂时」依赖文件系统时间戳，一旦有人再改动，台账立刻失效。**台账必须跟 R-030 提到的 DDL 双源问题一样，被视为有保质期的数据。**

### 1.3 P0=0 究竟是怎么来的

`fail_on=P0`，而 `findings_by_severity={P1: 216}` —— 所以 `status=passed`。这一步没有任何可疑之处。

可疑的是 P1 的来源。`severity_for()`（`:121-135`）靠**路径字符串启发式**判定生产/非生产：

```
PROD_PATH_TOKENS     = prod | production | staging | values.yaml | k8s | helm |
                       zhicloud_platform.sql | /src/main/resources/db/migration/
NON_PROD_PATH_TOKENS = values-dev.yaml | values-local.yaml | application-dev.yaml |
                       application-local.yaml | .env.example | docker-compose
```

本台账认为其中两条是**真实缺陷，不是可接受的近似**：

**(a) `application.yaml` / `bootstrap.yml` 是 profile 无关的，却被判成非生产。**
Spring 总是先加载 `application.yaml`，再由 profile 专属文件覆盖。这两个文件路径里不含任何 prod token → `is_prod=False` → 其中的弱口令默认值被判 P1。但核实结果：

- `application-prod.yaml`（180 行）全文搜 `api-key|API_KEY|sk-xxxx|OPENAI` **只有 1 处命中，且是注释**（第 18 行 `KD_NIAO_API_KEY`）；
- 即：生产 profile **没有**覆盖 `application.yaml` 里那 19 处 `${XXX_API_KEY:sk-xxxx}`；
- 其中 `web-search` 段落本就是 `enable: true`（`application.yaml:394`）。

所以：若生产环境未注入对应环境变量，进程会以字面量 `sk-xxxx` 作为 API Key 启动。**这是生产可达的，却被归类为 P1。**

**(b) `docker-compose.yml` 因为名字里有 `docker-compose` 被判成非生产。**
`script/docker/docker-compose.yml:269,272` 的 JDBC URL 是 `useSSL=true&requireSSL=true&verifyServerCertificate=false` —— 开了 TLS 但不校验证书链。它因此只算 P1。若任何准生产/演示/培训环境用 compose 起，这是一张真实的中间人攻击面。

**推论：P0=0 是一个可被改名动作逆转的结论。**把 `application-local.yaml` 改名为 `application-preprod.yaml`，同样的 `${MYSQL_PASSWORD:123456}` 会立刻从 P1 变成 P0。用一个不稳定启发式得出的「无 P0」，不能作为生产就绪的依据。

### 1.4 扫描面盲区（后缀白名单）

`SCAN_SUFFIXES = {.yaml, .yml, .properties, .sql, .env, .tpl, .sh, .xml}`。

**完全不在扫描面内**：`.py`、`.java`、`.json`、`.md`、`.tf`、`.gradle`、`.Dockerfile`。

受影响的实际生产资产举例：`scripts/production/init_bootstrap_admin.py` —— 这是生产首账号（`zhicloud_admin`）的唯一落地路径。逐行核实后该脚本本身设计是好的（随机口令 + `__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__` 占位符渲染，成本因子 12，24 字节熵，还有一段弱口令黑名单），但关键在于：**它从不出现在任何基线扫描结果里**，因此它身上发生的任何回归都不会被基线门禁发现。

同理不可见的还有：`scripts/production/*.py` 里可能出现的默认口令、`deploy/` 下所有非 yaml 文件。

### 1.5 复算了什么

为保证 216 这个数字可信，做了两项独立复算（不依赖 JSON 自述）：

| 复算项 | 独立结果 | 与 JSON 是否一致 |
|---|---|---|
| 全部 `.sql` 中 `INSERT ... INTO system_users` 行数 | 168 | — |
| R7 覆盖（160）+ 占位符模板 `*_bootstrap.sql`（8，含 `__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__`） | 160 + 8 = 168 | 一致 |
| 全部 `.sql` 中 `INSERT ... INTO system_oauth2_client/social_client` | 0 | 与 R10=0 一致 |

> **以上三行全部是「工作区口径」，不是 HEAD 口径。** 这个区别不只是行文严谨问题，它会直接推翻 §1.3 的结论，详见 §4.4。

R7 的阴性判定是**正确的**：8 份 `*_bootstrap.sql` 里的值确实是占位符而非真实哈希，被 `(?!__ZHICLOUD_)` 负向前瞻排除。这一点应当记入「扫描器做对了的事」，避免下次有人「修掉」它。

---

## 2. 汇总

### 2.1 按规则

| 规则 | 代码 | 条数 | 占 216 | 涉及文件数 |
|---|---|---:|---:|---:|
| R1 | `JDBC_TLS_DISABLED` | 5 | 2.3% | 3 |
| R2 | `JDBC_PUBLIC_KEY_RETRIEVE` | 5 | 2.3% | 3 |
| R3 | `DEFAULT_CREDENTIAL` | 33 | 15.3% | 5 |
| R4 | `PLACEHOLDER_SECRET` | 7 | 3.2% | 4 |
| R5 | `EMPTY_SECRET_VALUE` | 0 | 0.0% | 0 |
| R6 | `LITERAL_WEAK_SECRET` | 1 | 0.5% | 1 |
| R7 | `LITERAL_USER_ACCOUNT_SEED` | 160 | 74.1% | 8 |
| R8 | `DEMO_MODE_ENABLED` | 0 | 0.0% | 0 |
| R9 | `JDBC_TLS_IDENTITY_NOT_VERIFIED` | 5 | 2.3% | 3 |
| R10 | `SQL_LITERAL_SEED_CREDENTIAL` | 0 | 0.0% | 0 |
| **合计** | | **216** | **100%** | **20** |

R5 / R8 / R10 = 0，**不等于通过**，见 §4。

### 2.2 按文件（20 个）

| 文件 | 条数 | 规则构成 |
|---|---:|---|
| `demo-data/sql/dm/ruoyi-vue-pro-dm8_demo_seed.sql` | 20 | R7×20 |
| `demo-data/sql/highgo/ruoyi-vue-pro_demo_seed.sql` | 20 | R7×20 |
| `demo-data/sql/kingbase/ruoyi-vue-pro_demo_seed.sql` | 20 | R7×20 |
| `demo-data/sql/mysql/zhicloud_platform_demo_seed.sql` | 20 | R7×20 |
| `demo-data/sql/opengauss/ruoyi-vue-pro_demo_seed.sql` | 20 | R7×20 |
| `demo-data/sql/oracle/ruoyi-vue-pro_demo_seed.sql` | 20 | R7×20 |
| `demo-data/sql/postgresql/ruoyi-vue-pro_demo_seed.sql` | 20 | R7×20 |
| `demo-data/sql/sqlserver/ruoyi-vue-pro_demo_seed.sql` | 20 | R7×20 |
| `zhicloud-server/src/main/resources/application.yaml` | 19 | R3×19 |
| `zhicloud-server/src/main/resources/application-local.yaml` | 11 | R1×2 + R2×2 + R3×7 |
| `zhicloud-server/src/main/resources/application-dev.yaml` | 10 | R1×2 + R2×2 + R3×5 + R6×1 |
| `script/docker/docker-compose.yml` | 3 | R3×1 + R9×2 |
| `deploy/helm/zhicloud/templates/_helpers.tpl` | 2 | R4×2 |
| `deploy/helm/zhicloud/templates/secret.yaml` | 2 | R4×2 |
| `deploy/helm/zhicloud/values-dev.yaml` | 2 | R9×2 |
| `k8s/zhicloud-server.yaml` | 2 | R4×2 |
| `zhicloud-server/src/test/resources/application-integration-mysql.yaml` | 2 | R1×1 + R2×1 |
| `deploy/helm/zhicloud/values.yaml` | 1 | R4×1 |
| `script/shell/disaster-recovery-drill.sh` | 1 | R9×1 |
| `zhicloud-server/src/main/resources/bootstrap.yml` | 1 | R3×1 |
| **合计** | **216** | |

**集中度**：`demo-data/` 下 8 个文件独占 160 条 = **74.1%**；`zhicloud-server/src/main/resources/` 独占 31 条 = 14.4%。换言这 216 条的**绝大多数不是生产配置问题，而是数据与本地配置问题** —— 这一点决定了 §3 的处置顺序。

---

## 3. 逐组处置台账

字段说明：

- **建议处置**：`mitigated`（有证据证明被消解）或 `accepted_risk`（有签字的人承担） —— 二者是 suppression 策略 `allowed_dispositions` 里仅有的两个合法值。
- **为何是 P1 却没阻断**：回答「它为什么没让流水线红」。
- **未决条件**：在签字之前必须先成立的事实；不成立就不能签字。
- **验收证据**：签字时贴在 suppression 条目里的 `evidence` 字段内容要求。
- **建议期限**：不超过 `max_expiry_days=180`，即 **2027-03-27**。期限只是排期，**不自动生效**。

### R1 `JDBC_TLS_DISABLED` —— 5 条 / 3 个文件

**命中明细**（行号 = 15:14 扫描当时的工作区坐标；application.yaml 需按 §1.2 加 2）

| 文件 | 条数 | 行号 |
|---|---:|---|
| `zhicloud-server/src/main/resources/application-dev.yaml` | 2 | 50,55 |
| `zhicloud-server/src/main/resources/application-local.yaml` | 2 | 50,55 |
| `zhicloud-server/src/test/resources/application-integration-mysql.yaml` | 1 | 7 |

- **建议处置**：accepted_risk（限 demo/dev/test 三个 profile）
- **为何是 P1 却没阻断流水线**：连接串里写 `useSSL=false`。TLS 关闭意味着凭证与业务数据明文过链路。之所以没阻断：① 三个文件分别被 `application-dev` / `application-local` / `test` 路径命中 `NON_PROD_PATH_TOKENS`；② `fail_on=P0` 根本不拦 P1。
- **签字前的未决条件**：
  - 证明生产 profile 的 JDBC URL 不来自这三个文件：`application-prod.yaml` 用 `${MASTER_DATASOURCE_URL}` 注入，Helm / k8s 路径由 C11/C12 控制，且已核实 `values-prod.yaml` 中 `verifyServerCertificate` 不为 false。
  - `application-integration-mysql.yaml` 指向 `127.0.0.1:53306/zhicloud_sandbox_chain`，是 MySQL 沙箱 runner 的测试用配置；需确认沙箱端口不对公网监听（否则 `useSSL=false` + `allowPublicKeyRetrieval=true` 是本地可被窃听的组合）。
  - **不得**以「三个都没启用」为由签字：这三个文件是否存在与否由 `spring.profiles.active` 决定，属运行时事实，静态扫描看不到。
- **验收证据**：生产 profile 的 JDBC URL 来源追踪（application-prod.yaml → Helm values-prod.yaml → Secret），外加一次启动后实际生效 URL 的运行时取证（不能只看 yaml）。
- **责任角色**：平台/后端负责人 + 发布负责人
- **建议期限**：2027-03-27（超出即失效，需重新取证）

### R2 `JDBC_PUBLIC_KEY_RETRIEVE` —— 5 条 / 3 个文件

**命中明细**（行号 = 15:14 扫描当时的工作区坐标；application.yaml 需按 §1.2 加 2）

| 文件 | 条数 | 行号 |
|---|---:|---|
| `zhicloud-server/src/main/resources/application-dev.yaml` | 2 | 50,55 |
| `zhicloud-server/src/main/resources/application-local.yaml` | 2 | 50,55 |
| `zhicloud-server/src/test/resources/application-integration-mysql.yaml` | 1 | 7 |

- **建议处置**：accepted_risk（限 demo/dev/test 三个 profile）
- **为何是 P1 却没阻断流水线**：`allowPublicKeyRetrieval=true`：客户端未缓存服务端公钥时，会用一次可 MITM 的握手去取公钥。因此它几乎总是与 R1 绑定出现：本组 5 条与 R1 的 5 条落在完全相同的行上（dev 50/55、local 50/55、test 7）。
- **签字前的未决条件**：
  - 若 R1 已被判为「生产不启用」，R2 同批受限于同一组文件，可一并签字；反之必须一并修。
  - 注意二者**永远是同一行**（50/55 在 dev 与 local 里各 2 行，第 7 行在测试文件里），任何「单独处理 R2」的签字都会留下自相矛盾的台账。
- **验收证据**：同 R1 的运行时 URL 取证；两规则共享同一份 evidence。
- **责任角色**：平台/后端负责人（与 R1 同一人，避免双签矛盾）
- **建议期限**：2027-03-27（超出即失效，需重新取证）

### R3 `DEFAULT_CREDENTIAL` —— 33 条 / 5 个文件

**命中明细**（行号 = 15:14 扫描当时的工作区坐标；application.yaml 需按 §1.2 加 2）

| 文件 | 条数 | 行号 |
|---|---:|---|
| `script/docker/docker-compose.yml` | 1 | 130 |
| `zhicloud-server/src/main/resources/application-dev.yaml` | 5 | 22,52,57,115,147 |
| `zhicloud-server/src/main/resources/application-local.yaml` | 7 | 22,52,57,73,123,155,222 |
| `zhicloud-server/src/main/resources/application.yaml` | 19 | 216,222,228,230,232,335,339,343,347,351,355,359,363,367,371,375,379,385,393 |
| `zhicloud-server/src/main/resources/bootstrap.yml` | 1 | 13 |

- **建议处置**：**分三类，不可一刀切**：① application.yaml 19 条 → `mitigated`（需先修） ② dev/local/docker-compose/bootstrap.yml 14 条 → `accepted_risk` ③ 无第三类
- **为何是 P1 却没阻断流水线**：`${ENV_VAR:弱默认值}` 形态：环境变量缺失时进程**静默降级**到弱口令，而不是报错退出。这是本组比其他组更值得先处理的原因 —— 它是**失败开放**（fail-open）而不是「写了个弱值」。
- **签字前的未决条件**：
  - **① 组（19 条，真正的生产暴露面）**：`application-prod.yaml` 已确认**未覆盖**这些 AI `api-key`（全文仅 1 处注释级命中）。签字前必须先做一件事 —— 要么在 prod profile 显式覆盖并把值改成必填（缺 env 即启动失败），要么补充 C-level 控件把 `${VAR:弱值}` 形态本身列为生产禁例。**在此之前这 19 条不能以任何形式签字。**
  - `web-search` 段落 `enable: true` 且 `api-key: ${WEB_SEARCH_API_KEY:sk-xxxx}` —— 这是 19 条里最有代表性的一条，建议作为验收样例。
  - **② 组**：`application-local.yaml:222` 的 `mock-secret: ${MOCK_SECRET:test-only-for-local}` 注释说「P0 安全修复后 mock 模式密钥，仅 local 使用」，这条请替换为值审计证据；`bootstrap.yml:13` 的 `password: ${NACOS_PASSWORD:nacos}` 是 Nacos 注册中心口令，**bootstrap.yml 在所有 profile 下都加载**（它是 bootstrap 阶段，先于 application-* 生效）—— 它不是纯粹的 dev 文件，与 §1.3(a) 同理。
  - `script/docker/docker-compose.yml:130` 的 `AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY:-}` 默认值是**空串**：空 ≠ 弱，它属于「显式不配置」；若 S3 相关功能在该 compose 环境里被启用，这里需要单独判断。
- **验收证据**：①组：prod profile 覆盖后的 diff + 一次「不注入环境变量启动时进程必须失败」的实测记录。②组：各 profile 用途说明 + bootstrap.yml 生产加载路径的确认材料。
- **责任角色**：①组归安全负责人 + 平台负责人；②组归各环境运维负责人
- **建议期限**：2027-03-27（超出即失效，需重新取证）

### R4 `PLACEHOLDER_SECRET` —— 7 条 / 4 个文件

**命中明细**（行号 = 15:14 扫描当时的工作区坐标；application.yaml 需按 §1.2 加 2）

| 文件 | 条数 | 行号 |
|---|---:|---|
| `deploy/helm/zhicloud/templates/_helpers.tpl` | 2 | 121,135 |
| `deploy/helm/zhicloud/templates/secret.yaml` | 2 | 5,12 |
| `deploy/helm/zhicloud/values.yaml` | 1 | 222 |
| `k8s/zhicloud-server.yaml` | 2 | 45,74 |

- **建议处置**：**先修扫描器，再谈处置** —— 见下方「本组是假阳性」，在修完之前不签字
- **为何是 P1 却没阻断流水线**：7 条命中的**全部 7 行，没有一行是真正的占位符值**。逐一核对：`_helpers.tpl:121`（注释）、`_helpers.tpl:135`（门禁自身的 `hasPrefix "CHANGE_ME"` 判断代码）、`secret.yaml:5` 与 `:12`（注释）、`values.yaml:222`（注释）、`k8s/zhicloud-server.yaml:45` 与 `:74`（注释）。**规则 R4 把讲解占位符的说明文字、以及拦截占位符的守卫代码，全部当成了占位符本身。**
- **签字前的未决条件**：
  - **真阳性被漏掉了**：真正需要被拦的生产占位符默认值用的是**另一个 token** —— `deploy/helm/zhicloud/values.yaml` 里 **11 处 `__REQUIRED__`**（240/242/244/246/248/250/252/254/258/260/262 行）。R4 的正则是 `re.compile(r"CHANGE_ME", re.I)`，**不匹配 `__REQUIRED__`**。换句话说：R4 报的全是注释，真正的占位符一个都没报。
  - **目前真正拦住那 11 个 `__REQUIRED__` 的是 Helm 渲染期门禁**（`_helpers.tpl:135`：`eq $value ""` / `hasPrefix "CHANGE_ME"` / `hasPrefix "__REQUIRED__"` 三者之一成立即 fail），且 `:136-137` 的逃生舱 `allowInsecureDefaults` 在 prod profile 下失效。**这个保护是真实存在的，但它不在 R4 的功劳簿上。**
  - `k8s/zhicloud-server.yaml` 经核实**不含任何占位符秘钥**（Secret 外置为 `zhicloud-server-secret`，由 ESO/Vault/sealed-secrets 同步）。这条可以明确排除。
  - 处置动作应为：把 R4 正则改成 `(CHANGE_ME|__REQUIRED__)` 并排除纯注释行；同时保留 C3 作为运行时兜底。改完重跑，数字会从 7 变成 11 左右 —— **这个上升是修好了，不是变差了，需要提前跟评审人讲清楚。**
- **验收证据**：扫描器改造 PR + 重跑后的 diff（预期 R4 命中从「7 条注释」变为「11 条真实占位符」）+ `helm template` 在 `__REQUIRED__` 未替换时确实失败的一次实测输出。
- **责任角色**：工具链负责人（扫描器）+ Helm chart 负责人验证门禁
- **建议期限**：2027-03-27（超出即失效，需重新取证）

### R6 `LITERAL_WEAK_SECRET` —— 1 条 / 1 个文件

**命中明细**（行号 = 15:14 扫描当时的工作区坐标；application.yaml 需按 §1.2 加 2）

| 文件 | 条数 | 行号 |
|---|---:|---|
| `zhicloud-server/src/main/resources/application-dev.yaml` | 1 | 213 |

- **建议处置**：accepted_risk（单条，dev-only）
- **为何是 P1 却没阻断流水线**：`application-dev.yaml:213` 的 `client-secret: xx`。R6 的弱值白名单含 `xx`/`xxx`/`123456`/`admin`/`changeme` 等，这条命中的是 `xx`。单条、落在 dev profile。
- **签字前的未决条件**：
  - `xx` 不在 human-meaningful 口令字典里还好，但它暗示 dev 环境「随便填」的惯例；签字前请确认 OAuth client 在 dev 环境对接的是哪个下游 —— 若下游是共享测试租户而非本地 mock，这条就不是纯本地噪音。
  - 注意 216 条里只有 **1 条** LITERAL_WEAK_SECRET，而仓库里有 13,277 个受管文件（`git ls-files` 实测），yaml/properties 配置的体量远不是一条单行正则能覆盖的。这个「1」更像是 R6 正则过窄（`^"?(xx|xxx|123456|1234|admin|root|test|changeme|password)"?$` 要求**整行就是这个字面量**），而不是仓库真的只有一个弱字面量。签这条时不要顺带得出「字面量弱口令问题已受控」。
- **验收证据**：dev profile OAuth client 的环境归属说明。
- **责任角色**：后端负责人
- **建议期限**：2027-03-27（超出即失效，需重新取证）

### R7 `LITERAL_USER_ACCOUNT_SEED` —— 160 条 / 8 个文件

**命中明细**（行号 = 15:14 扫描当时的工作区坐标；application.yaml 需按 §1.2 加 2）

| 文件 | 条数 | 行号 |
|---|---:|---|
| `demo-data/sql/dm/ruoyi-vue-pro-dm8_demo_seed.sql` | 20 | 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28 |
| `demo-data/sql/highgo/ruoyi-vue-pro_demo_seed.sql` | 20 | 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28 |
| `demo-data/sql/kingbase/ruoyi-vue-pro_demo_seed.sql` | 20 | 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28 |
| `demo-data/sql/mysql/zhicloud_platform_demo_seed.sql` | 20 | 10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29 |
| `demo-data/sql/opengauss/ruoyi-vue-pro_demo_seed.sql` | 20 | 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28 |
| `demo-data/sql/oracle/ruoyi-vue-pro_demo_seed.sql` | 20 | 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28 |
| `demo-data/sql/postgresql/ruoyi-vue-pro_demo_seed.sql` | 20 | 9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28 |
| `demo-data/sql/sqlserver/ruoyi-vue-pro_demo_seed.sql` | 20 | 11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30 |

- **建议处置**：accepted_risk（整批 160 条按「一组」签字，**不逐条签**）
- **为何是 P1 却没阻断流水线**：8 份 demo 种子库里各自 20 行 `INSERT INTO system_users (... password ...)`，写入的是**真实的 BCrypt 哈希**（已排除的 8 份 `*_bootstrap.sql` 才是占位符）。没有阻断流程的原因只有一条：`severity_for()` 里 R7 是 `return "P0" if lowered.startswith("sql/") else "P1"` —— 这些文件在 `demo-data/sql/` 下，**不以 `sql/` 开头**，于是降级。**如果哪天有人把它们挪到 `sql/` 下，这 160 条会一夜之间全部变成 P0 并让流水线红。**
- **签字前的未决条件**：
  - 这批单据必须**整批处理**：8 个方言文件 × 20 行 = 160，逐条签字毫无意义且制造虚假工作量。
  - 签字前提不是「它们加了 demo 前缀」，而是**有一份可执行的、可验证的承诺：这 8 个文件不会被任何生产/准生产部署、初始化脚本、CI 作业、备份恢复流程引用。** 上一轮核实 `deploy/` 与 `k8s/` 全文均无 `demo_seed|demo-data|DEMO_` 命中，`script/docker/docker-compose.yml` 的 initdb 挂载也不含 demo-data —— 这是目前唯一的支撑证据，靠否定式检索，强度偏弱。
  - **真正的残留风险在这里**：哈希本身在版本库里，任何拿到仓库的人都能离线破解这 20 个账号的口令。若这 20 个口令与其他环境（含生产）复用 —— 哪怕只有一个复用 —— 这条 P1 的实际影响就是 P0 级别。签字前至少要做一次跨环境口令复用排查，或者把这 20 个哈希换成惰性不可登录值。
  - 与 R-022（模拟数据隔离）**不是同一件事**：R-022 说的是租户隔离（`tenant_id` 9001+、`DEMO_` 前缀、`creator=demo-seed`），台账说的是「哈希在库里」。隔离做得好不影响哈希可被离线暴力破解。两者都要有，不要相互替代。
- **验收证据**：① 生产/准生产部署清单与 demo-data 无交集的**正向**证据（而非仅 grep 否定结果）；② 20 个哈希与其他环境口令的复用排查结论；③ 换惰性值或加盐差异化的变更记录（若采纳）。
- **责任角色**：数据/运维负责人（执行官）+ 安全负责人（背书）
- **建议期限**：2027-03-27（超出即失效，需重新取证）

### R9 `JDBC_TLS_IDENTITY_NOT_VERIFIED` —— 5 条 / 3 个文件

**命中明细**（行号 = 15:14 扫描当时的工作区坐标；application.yaml 需按 §1.2 加 2）

| 文件 | 条数 | 行号 |
|---|---:|---|
| `deploy/helm/zhicloud/values-dev.yaml` | 2 | 42,43 |
| `script/docker/docker-compose.yml` | 2 | 269,272 |
| `script/shell/disaster-recovery-drill.sh` | 1 | 338 |

- **建议处置**：**不可按现状签字** —— 3 个文件需分别处置，其中 docker-compose 与 drill.sh 建议 `mitigated`
- **为何是 P1 却没阻断流水线**：`verifyServerCertificate=false`：开了 TLS 却不校验证书链与主机名，等价于「加密但无身份验证」。这类配置的风险在于**看起来水是安全的**（`useSSL=true`），评审和运维都容易放行。
- **签字前的未决条件**：
  - `deploy/helm/zhicloud/values-dev.yaml:42-43` —— dev 环境，可接受 `accepted_risk`，与 R1/R2 的 dev 组同批处理。
  - `script/docker/docker-compose.yml:269,272` —— 主库与从库两条 URL 都带 `verifyServerCertificate=false`，且**该文件的降级理由是它名字里有 `docker-compose`**（§1.3b）。若这个 compose 只用于本地开发，签字；若它出现在任何准生产/演示/培训环境的上线手册里，**必须先修**。这条的分歧点不是一个技术问题，是「compose 的实际使用范围」这个事实问题，需要你给出明确答复。**本台账不替你假设它是非生产。**
  - `script/shell/disaster-recovery-drill.sh:338` —— **这是最需要被单独拎出来的一条**。它是灾备演练脚本，按定义会连接**另一套真实存在的 MySQL 实例**（不一定是本机的 `DRILL_MYSQL_CONTAINER`）。灾备演练通常在准生产或生产旁路做，且演练往往是唯一会连到跨机房/跨 VPC 库的场景。建议在它的 URL 上启用 `verifyServerCertificate=true` 并注入 truststore，**在此之前不签字**。
- **验收证据**：① docker-compose 的使用范围声明（运维负责人签字，而非推断）；② drill.sh 修改后的 diff + 一次演练实测连接成功的记录；③ dev 组与 R1/R2 共用同一份 profile 取证。
- **责任角色**：运维/SRE 负责人（compose 与 drill）+ 平台负责人（values-dev）
- **建议期限**：2027-03-27（超出即失效，需重新取证）


---

## 4. 零命中的规则，不等于「通过」

`findings_by_rule` 里没有 R5 / R8 / R10。逐条核实后，三者的性质完全不同 —— **不能合并成一句「其余规则均无发现」**：

### R5 EMPTY_SECRET_VALUE = 0 —— 规则被自己的豁免表吃干净了

R5 匹配 `^\s*\w*(PASSWORD|SECRET|KEY)\w*\s*:\s*""\s*$`。全仓库符合该形状的行确实存在 6 行：

| 文件 | 行 | 内容 | 是否被豁免 |
|---|---:|---|---|
| `deploy/helm/zhicloud/values.yaml` | 177 | `existingSecret: ""` | 是，命中 `EXISTINGSECRET` |
| `deploy/helm/zhicloud/values.yaml` | 179 | `existingSecretRevision: ""` | 是，命中 `EXISTINGSECRETREVISION` |
| `deploy/helm/zhicloud/values.yaml` | 236 | `existingSecretRevision: ""` | 是 |
| `deploy/helm/zhicloud/values.yaml` | 288 | `passwordSecretName: ""` | 是，命中引用名家族 |
| `deploy/helm/zhicloud/values-prod.yaml` | 56 | `existingSecret: ""` | 是 |
| `deploy/helm/zhicloud/values-prod.yaml` | 84 | `passwordSecretName: ""` | 是 |

**6 行全部落在 `SECRET_REFERENCE_NAME_RE` 的豁免名单内，有效命中率 0/6。** 豁免本身是合理的（这些字段存的是 Secret 的引用名而不是凭据值），但由此只能得出一个结论：**R5 在这套仓库里结构性命中不了任何东西，它的 0 值不携带信息量。**

更值得注意的是一处错位：`values.yaml` 里真正的生产占位符是 11 处 `__REQUIRED__` 形状为 `KEY: "__REQUIRED__"` —— 既不是空串（R5 不认），也不含 `CHANGE_ME`（R4 不认）。**这两条规则中间夹着一条缝，11 个占位符正好从缝里漏过去**，目前全靠 Helm 渲染期门禁 `_helpers.tpl:135` 兜住。

### R8 DEMO_MODE_ENABLED = 0 —— 这是真阴性，且可以用证据固化

R8 正则为 `^\s*demo\s*:\s*true\s*$`（行锚定、且必须是裸 `true`）。全仓 yaml / yml / properties 中符合 `^\s*demo\s*:` 形状的行共 4 处：

| 文件 | 行 | 值 |
|---|---:|---|
| `zhicloud-server/src/main/resources/application-prod.yaml` | 135 | `demo: false` |
| `zhicloud-server/src/main/resources/application-dev.yaml` | 186 | `demo: false` |
| `zhicloud-server/src/main/resources/application-local.yaml` | 234 | `demo: false` |
| `zhicloud-module-iot/.../src/test/resources/application-unit-test.yaml` | 52 | `demo: false` |

**全部为 false，prod profile 显式关闭。** 所以 R8=0 是**可信的真阴性**，也是本台账唯一愿意正面背书的零值。

但防线仍要留一层：这条与 §1.3(a) 同源 —— 若将来有人在基底的 `application.yaml` 里加一行 `demo: true`，它会因为路径不含 prod token 而被判非生产，而 R8 在非生产路径下**降为 P2**，直接落到 `fail_on=P0` 阈值以下消失。建议把「基底配置严禁 `demo: true`」做成独立控件，不要指望 R8。

### R10 SQL_LITERAL_SEED_CREDENTIAL = 0 —— 复算为 0，但**这是一个尚未提交的删除动作造成的**

遍历全部 `.sql`（排除 `.git` / `target` / `node_modules`）搜 `INSERT ... INTO system_oauth2_client | system_social_client`，工作区结果确实 **0 行**，与 JSON 的 `R10=0` 一致。

但把同一个正则打到 `git show HEAD:<file>` 上，结果是 **12 行**。差额来自一笔**尚未提交的改动**：

```
 M zhicloud-server/src/main/resources/db/migration/V1.1__system_base_schema.sql   | 32 -
```

这 32 行删除的构成（逐 Catholicism 行核对 `git diff -U0`）：

| 表 | 删除行数 | HEAD 上的 R10 命中数（同一套正则） |
|---|---:|---:|
| `system_oauth2_client` | 4 | 4 |
| `system_social_client` | 8 | 8 |
| `system_users` | 20 | 20（由 R7 计） |
| **合计** | **32** | **32** |

被删掉的内容包括：`client_id='default'` / `secret='admin123'` 的默认 OAuth 客户端、`client_id='test'` / `secret='test2'`、两个 SSO 演示客户端（secret 均为 `test`）、8 行三方社交登录配置（含钉钉 `client_secret` 明文）、以及 20 个带真实 BCrypt 哈希的账号。

### 4.4 这笔未提交删除改变了「P0=0」的性质

把上面的事实排起来：

1. `V1.1__system_base_schema.sql` 的路径 `zhicloud-server/src/main/resources/db/migration/...` 命中 `PROD_PATH_TOKENS` 里的 `/src/main/resources/db/migration/`，且不含任何 `NON_PROD_PATH_TOKENS` → `is_prod=True`；
2. `severity_for()` 中 R7 与 R10 都在「`is_prod` 则返回 P0」的集合里；
3. 所以 **在 HEAD 上，这 32 行会让扫描产出 32 条 P0**，`fail_on=P0` 下 `status` 为 **failed**；
4. 而本次扫描读的是工作区（§1.1），那里这 32 行已经被删掉，于是 P0=0、`status=passed`。

**结论：`security-baseline-20260928_151500.json` 里那个 `status=passed`，是「把一段还躺在 HEAD 上的生产级明文凭据删掉之后」才取得的。** 它记录的 `commit=553bc780…` 让人以为是在那个 commit 上通过的 —— 恰恰相反，在 553bc780 上它会红。

这件事有三个后果，都必须在审阅时处理：

- **① 这笔删除不能与 evidence 分开提交。** 如果先把 32 行删除 revert、或者在 CI 上按 HEAD 构建，测试环境会立刻出现 32 条 P0。交接文档把 `P0=0` 当作已达成状态，是不成立的。
- **② 删了 V1.1 不等于删了库里已有的行。** Flyway 对每个版本只执行一次；改已应用迁移的内容会改变其校验和，而 `application.yaml:35` 配的是 `validate-on-migrate: true`（`:30 baseline-on-migrate: true`、`:31 baseline-version: 1`）。因此对**已经跑过** V1.1 的存量库，下次启动会直接校验失败；对**没跑过**的新库，这 20 个账号和 12 条客户端凭据不会被写入。这两类库需要不同处理，目前没有看到任何一处写明由谁、在什么时点做 `flyway repair` 或补一次新的迁移来把存量数据删干净。
- **③ `'default' / 'admin123'` 属于公开可知的默认客户端凭据。** 存量环境只要曾经装过 HEAD 版本的 V1.1，这条凭据就仍然有效 —— 它不是「删了文件就消失」类的问题。

可以下的结论只能是：「**工作区里**没有字面量 client secret 躺在 SQL 文件里」。不能推广成「OAuth client 凭据受控」，也不能推广成「commit 553bc780 通过基线」。

---

## 5. 为什么现在不要开 suppression

证据里的 `suppression.policy`：

```
fail_closed          = true
never_hidden         = true
max_expiry_days      = 180
allowed_dispositions = [mitigated, accepted_risk]
required_fields      = [file, rule, disposition, reason, owner, approved_by, expires_on, evidence]
policy.note          = "模板为空数组；任何抑制都必须人工填写 reason/owner/approved_by/expires_on/evidence。"
```

这套策略设计得是对的 —— 尤其 `fail_closed=true` 配合缺字段即失败，以及 `never_hidden=true` 保证被抑制项仍然出现在报告里。**不要为了「把 216 降下去」而破坏它。**

三条硬性要求：

1. **本台账不产出任何 suppression 条目。** 这里写的「建议处置」只是建议；转成 suppression 必须由人逐条填写 8 个必填字段，其中 `owner` 与 `approved_by` **不得为同一人**。
2. **`suppression.enabled` 保持 `false`。** 当前 `enabled=false / applied=0 / entries=0 / invalid=0 / unmatched=0`，这是最安全的状态。
3. **禁止批量回填。** 尤其禁止对 R7 的 160 条一次性抑制 —— 它们是目前唯一在提醒「版本库里躺着 20 个真实 BCrypt 哈希」的信号。批量的代价不是工作量，是这条信号的可见性会随行数一起塌掉。

还有一个容易踩的坑：`expires_on` 是硬上限 180 天。以 2026-09-28 起算，**2027-03-27** 封顶。台账里不要按「项目结束」或「下次重构」定期限，超期会直接 fail_closed。

---

## 6. 与其他登记项的关系，以及下一步

### 6.1 关系

| 项 | 与本台账的关系 |
|---|---|
| R-022 模拟数据隔离 | **不等同于 R7 的处置。** R-022 讲的是租户隔离（`tenant_id` 9001+、`DEMO_` 前缀、`creator=demo-seed`）；台账讲的是「哈希躺在版本库里，拿到仓库的人可离线破解，且这 8 个文件可被恢复到任意环境」。隔离做得好，不代表哈希不可被攻击。**两者都要有，不能互相替代。** |
| R-030 Schema 双源漂移 | 与本台账无交集（DDL 来源问题），但共享同一个根因：拿工作区当真相。保持 OPEN。 |
| R-031 Quartz 表仅存在于安装脚本 | 无交集。保持 OPEN。 |
| R-011 / R-029 | 无交集。保持 OPEN。 |

### 6.2 下一步，按依赖顺序（而不是按数量）

1. **先修扫描器**，它阻塞后面所有步骤。至少 4 处改动：
   - R4 正则扩到 `(CHANGE_ME|__REQUIRED__)`，并跳过纯注释行与门禁代码本身；
   - `severity_for()` 把 profile 无关的基底文件（`application.yaml`、`bootstrap.yml`）按生产计 —— 做法是新增一个「profile-agnostic」判定，而不是把它们硬塞进 `PROD_PATH_TOKENS`；
   - `SCAN_SUFFIXES` 至少加 `.py`，否则 `scripts/production/*.py`（含生产首账号脚本）永远在盲区；
   - `PROD_FLYWAY_ROOT = "src/main/resources/db/migration/"` 配 `startswith`，在 monorepo 根目录下**永远不会为真**（实际相对路径是 `zhicloud-server/src/main/resources/db/migration/...`）。它目前之所以还没出事，是因为 `PROD_PATH_TOKENS` 里恰好有一条带前导斜杠的 `/src/main/resources/db/migration/` 兜住。**一条死代码加一个脆弱依赖，应当合并成一处。**
2. **冻结后再重跑。** 先把整理出的分组提交落地（见 `docs/enterprise-platform/handoff/uncommitted-review-groups-20260928.md`），或至少在干净快照上跑；否则 §1.2 的行号漂移会重演。
3. **不要把 216 平均分给人。** 按 §3 的 7 组派活：R4 组归工具链（先修器再谈处置）；R3 的 19 条归安全 + 平台（唯一确认生产可达的一组）；R9 的 drill.sh 归 SRE；R7 的 160 条整批归数据 / 运维，安全背书；R1 / R2 / R6 随 dev 环境打包签字。
4. **重跑之前，这份台账的行号只能当历史坐标用。** 要按行定位请用 `git show 553bc780:<file>`，不要用当前工作区。

---

## 7. 本台账自身的局限

- 全部结论来自**一次**静态扫描产物 + 对扫描器源码的阅读 + 若干 grep / python 复算。**没有运行时取证**：没有真的用生产配置启动过进程，因此 R3 第①组「不注入环境变量会怎样」是推断（`application-prod.yaml` 未覆盖 → Spring 绑定占位符默认值 → 进程带着 `sk-xxxx` 起来），不是实测。这一步必须由一次启动实验来闭合，不要拿推断当已经验证过的事实。
- R4 的「11 处 `__REQUIRED__`」只统计了 `values.yaml` 与 `values-prod.yaml`；`_helpers.tpl`、`secret.yaml` 及其他 Helm 模板未做同口径统计。
- 「生产与准生产不加载 demo-data」目前只有**否定式 grep** 支撑（`deploy/`、`k8s/` 全文无 `demo_seed|demo-data|DEMO_` 命中），没有正向的部署清单或白名单证据。**这个证据强度不足以支撑 R7 的签字。**
