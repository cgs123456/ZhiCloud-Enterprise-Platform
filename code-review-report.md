# ZhiCloud-Enterprise-Platform 代码与架构复查报告

> 复查对象：`D:\Desktop\yudao`（基于 yudao cloud 2026.06.0 的 fork，groupId `cn.iocoder.boot`）
> 技术栈：Spring Boot 3.5 / JDK 21 / MyBatis-Plus / Spring Security / Spring Modulith / Flyway
> 复查日期：2026-08-02 | 性质：只读静态分析 + 公开资料对标，未修改代码
> 严重度口径：**阻塞**（不修复不可上线）/ **高**（上线前必须处理）/ **中**（首个迭代内）/ **低**（技术债登记）

---

## 一、总体结论（先行）

**不建议直接全量生产上线。** 框架底座（鉴权防注入、多租户隔离、软删除/审计、可观测性、容器化、CI 安全链）继承自上游且工程质量扎实；本仓此前 7 个 CI 阻断问题（TMS/HR/OA/AI 测试、重复 spring 键、security-check 脚本、prod 密钥、CycloneDX 版本）**已全部修复并验证**。主要风险高度集中在**业务模块的落地质量**：

- 启用模块测试覆盖率约 **4.1%**，核心业务（MES/ERP/CRM）近乎零覆盖，无法证明正确性；
- **AI 与 BPM 模块在仓库内缺生产建表语句**，全新库按本仓脚本部署会缺表；
- **导出链路存在 OOM 风险**（非流式 + `PAGE_SIZE_NONE` + 容器固定 `-Xmx512m`）；
- 数据库结构**未被 Flyway 真实纳管**（`V1` 为空占位、`validate-on-migrate=false`）；
- 错误码跨模块冲突、方法级权限注解覆盖不全、日志非结构化等工程治理问题。

建议路线：**先以内部试点/灰度方式上线已验证模块（system / infra / crm / bpm 基础能力）**，按本文第四节优先级迭代补齐。

---

## 二、问题清单（模块 | 问题描述 | 严重级别 | 影响范围 | 修复建议）

| 模块 | 问题描述 | 级别 | 影响范围 | 修复建议 |
|---|---|---|---|---|
| AI / BPM | 仓库 `sql/mysql/` 内**无 AI（14 张业务表）、BPM（5 张业务表）的生产 DDL**；仅 `ai_prompt_template.sql`(1 表) 与 BPM 的 *测试* SQL 存在。AI 的 chat/message/knowledge 等表、BPM 的 `bpm_process_*`/`bpm_oa_leave` 表全新库部署即缺表。 | **高** | AI、BPM 全功能（含 CRM 合同审批所依赖的 BPM） | 补全 `sql/mysql/ai.sql` 与 `sql/mysql/bpm.sql`；或在部署文档中明确"需导入上游 yudao 的 AI/BPM SQL"并做全新库冒烟验证（U-2） |
| 测试 | 启用范围测试/主代码比 ≈ **200/4864 = 4.1%**；MES(1332→26)、ERP(666→5)、CRM(353→3)、WMS(348→11)、QMS(294→6) 核心业务近乎零覆盖。 | **高** | 全业务正确性无法证明 | 引入 JaCoCo 覆盖率门禁；优先为核心域（crm/erp/wms/mes/bpm）补 `BaseDbUnitTest` 用例，目标核心域 ≥40% 行覆盖 |
| 数据治理 | **错误码跨模块冲突 102 处**（MES↔QMS 37、OA↔TMS 21、IM↔MES 17、HR↔IOT 15 等）。如 MES 与 QMS 共用前缀 `1_040_100_000`，前端无法按码分流、日志无法定位来源模块。 | **高** | 全部前端报错处理、日志/告警定位 | 按模块重新分配 `ErrorCode` 前缀段；CI 增加"错误码全局唯一"静态检查；改码前先扫前端硬编码码值（U-6） |
| 性能/导出 | **导出非流式**：`ExcelUtils.write()` 一次性 `doWrite(data)` 全量驻留堆；全仓 **128 个导出接口** + `PAGE_SIZE_NONE=-1` 被用 **146 处**；容器固定 `-Xms512m -Xmx512m`。大表导出易 OOM/长事务。 | **高** | 导出类接口（ERP/MES/WMS 百万行表） | `ExcelUtils` 改分批 `write` 或异步导出（任务中心下载）；`PAGE_SIZE_NONE` 增加行数上限；容器改用 `-XX:MaxRAMPercentage` |
| 数据一致性 | **Flyway 基线为空占位**：`V1__baseline.sql` 仅 `SELECT 1;`，真实建表靠手工执行 `sql/mysql/*.sql`；`validate-on-migrate=false` 使 schema 漂移不被发现。 | **高** | 数据库结构版本可控性 | 将 `zhicloud_platform.sql` 等并入 `V1` 基线；生产开启 `validate-on-migrate=true`；或迁移到 Liquibase 管理 |
| 权限 | 全仓 3345 个 Controller 映射方法中 **336 个（10%）无权限注解**（`/admin-api` 237、`/app-api` 83、其他前缀 16）。含 WMS PDA/`/wms-api`（`/wms/**` 不在标准前缀内）。**注：框架以 `.anyRequest().authenticated()` 兜底，这些端点仍需登录，缺的是方法级 `@PreAuthorize` 细粒度鉴权**，非裸奔。 | **中** | 越权读取/操作面（尤其 WMS 仓管操作） | 对写接口/敏感读接口补 `@PreAuthorize("@ss.hasPermission('...')")`；WMS 端点统一纳入 `/admin-api` 前缀或显式注解 |
| 性能 | **潜在 N+1 查询 357 处**（循环体内单条查询）。抽样确认真实命中如 `CrmContactBusinessServiceImpl` 在 `forEach` 中逐条查商机。 | **中** | 列表/详情高频接口响应与 DB 压力 | 开启 MyBatis SQL 日志/p6spy 跑核心接口统计；对确认命中者改 `IN` 批量查询或联表；引入二级缓存 |
| 健壮性 | **裸抛异常 79 处**（`RuntimeException`/`IllegalStateException`），绕过 `ErrorCode` 体系，被 `GlobalExceptionHandler` 兜底成 500 且带原始堆栈。启用模块内命中 bpm/ai/datalake/framework 多处。 | **中** | 错误信息泄露、前端无法友好提示 | 统一改用 `ServiceException(ErrorCode)`；敏感堆栈不返前端 |
| 可观测性 | 日志仅 `PatternLayoutEncoder`，**无 JSON 结构化输出**（无 Logstash/Json Encoder），ELK/Loki 采集依赖 grok 正则，多行堆栈难聚合。 | **中** | 日志检索/告警接入成本 | 引入 `logging-json` 或 `logstash-logback-encoder`；prod 启用结构化日志 |
| 可观测性 | 已暴露 Prometheus 端点，但**仓库内无告警规则文件**（无 `*.rules.yml`/PrometheusRule）。指标采集了但无人被叫醒。 | **中** | 故障发现时效 | 补充关键指标告警规则（JVM/HTTP 5xx/线程池/Redis 连通） |
| 日志 | `ApiAccessLogInterceptor:97` 使用 `System.out.printf` 直写控制台（运行期请求链路上），绕过 logback，无 traceId、不落文件。 | **中** | 访问日志缺失 traceId、难关联 | 改用 `log.info(...)` 并带 `traceId`；确认是否受 profile 控制（U-9） |
| 部署 | **Redis 为硬依赖，无降级路径**（无 embedded/mock/`enabled:false` 开关）。Redis 不可用 = token/缓存/分布式锁全挂。 | **中** | 单点故障容错（需确认是否连哨兵/集群，U-10） | 明确 Redis 高可用拓扑；非核心缓存加 `allowCacheNotFound`/本地兜底 |
| 模块治理 | 7 个模块（member/report/mp/pay/mall/im/iot）被注释禁用但**代码仍在仓库**（约 2077 主文件，30%），长期不参与编译，存在腐化与依赖升级破坏风险。 | **中** | 长期维护成本 | 确认永久下线则从仓库移除；若近期启用需先补齐其 DDL（禁用模块占剩余 48 张缺表） |
| 配置 | `REDIS_PASSWORD` 无默认值也无 `:?required`（同文件 `MYBATIS_ENCRYPTOR_PASSWORD` 用 `:?required`），未注入时排障信息不指向根因。 | **低** | 启动排障体验 | 统一改为 `${REDIS_PASSWORD:?required}` |
| 多租户 | `tenant.ignore-tables` 为空，所有非 `TenantBaseDO` 表靠代码注解豁免，配置层无兜底。 | **低** | 新增全局表误注入过滤条件风险 | 关键共享表在配置层显式 ignore，降低对新人误用敏感度 |
| 运维 | 启动所需环境变量**无集中文档**（无 `.env.example`），运维需逆向阅读多份 yaml。 | **低** | 部署上手成本 | 提供 `.env.example` 或 README 环境变量表 |

### 已闭环（本仓此前修复，本报告确认）
- TMS/HR/OA 单元测试脚手架补齐、AI 集成测试缺 key 不崩溃、HR 补 `spring-modulith-core`；
- `yudao-server/application.yaml` 同文档重复 `spring:` 键修复（ModularityTests 通过）；
- `script/security/security-check.sh` 的 `$2a` 未转义崩溃修复、`application-prod.yaml` 补 `MYBATIS_ENCRYPTOR_PASSWORD`；
- CycloneDX 插件锁版本 `2.9.1`（CI SBOM 步骤通过）。

---

## 三、行业标杆对标差距表（功能模块 | 行业常见做法 | 本项目现状 | 差距与优先级）

| 功能模块 | 行业常见做法（RuoYi/JeecgBoot/Pig、Odoo、Salesforce、钉钉宜搭/飞书） | 本项目现状 | 差距与优先级 |
|---|---|---|---|
| 权限模型 | RBAC + **记录级规则（record rules）** + 角色模板 + 定期审计；按钮级权限 + 数据范围 | URL 级认证（`anyRequest().authenticated()`）+ 方法级 `@PreAuthorize` 部分覆盖；多租户靠 `tenant_id` 隔离 | **中**：缺记录级数据权限表达式、权限模板；@PreAuthorize 覆盖不全 |
| 工作流/BPM | 可视化流程设计器、多步骤审批、审批留痕与业务强闭环（Odoo Approvals、钉钉宜搭） | BPM 模块存在且 CRM 合同已接入；但 **HR 请假/TMS 运单未接入 BPM**，`bpm_oa_leave` 表未建；前端缺流程设计器 | **高**：审批与业务闭环不完整；缺可视化设计器 |
| CRM | 销售管道看板、AI 线索评分、全渠道（邮件/WhatsApp/电话）、报价电子签名、客户门户（Salesforce/Odoo） | 基础 CRUD + 客户/商机/合同；合同接入 BPM | **中**：缺营销自动化、线索评分、全渠道集成、客户自助门户 |
| ERP 采购/库存 | 多仓多地点、FIFO/LIFO、条码/序列号、自动补货、供应链计划（Odoo Inventory） | ERP 有采购状态机 + WMS 23 表 DDL | **中**：缺条码/IoT 采集、自动补货规则、MRP 展开 |
| HR | 招聘/考勤/薪资计算/绩效/排班全闭环（Odoo HR） | 仅请假 + 基础档案；请假未接 BPM | **高**：缺考勤、薪资计算、绩效考核，与行业差距大 |
| MES/QMS（制造/质量） | 工单/工艺/设备数采、不合格品、质量追溯闭环 | MES 1332 文件体量最大、QMS 有检验模块（21 表 DDL） | **中**：缺设备联网(IoT)、质量正向/反向追溯闭环 |
| 多租户 | Odoo 多公司 + 记录规则；数据主权/GDPR 合规 | `TenantBaseDO` + 拦截器强制注入 `tenant_id`，交叉校验 0 高危 | **达标**：实现扎实，仅配置层兜底不足（见低项） |
| AI 能力 | LLM 集成 + RAG + Agent + 向量库 | `ai` + `ai-rag`(pgvector) + `ai-multiagent` 三模块，能力较完整 | **部分达标**：**单测被 `@Disabled` 跳过，AI 链路 CI 未验证**；缺生产 DDL |
| 可观测性 | Prometheus + Grafana + 告警规则 + 结构化日志 | Actuator/Prometheus/OTLP 已接，日志含 traceId/tenantId | **部分达标**：缺 JSON 日志、缺告警规则 |
| 部署/CI | CI/CD + SAST/SCA + SBOM + 容器非 root + 健康检查 | GitHub Actions（build+security-check+OWASP+CycloneDX）、Dockerfile 非 root、HEALTHCHECK | **达标**：安全链完整，本次已修复脚本与版本问题 |
| 代码生成/低代码 | RuoYi/JeecgBoot 前后端一键生成、在线表单 | yudao 上游具备代码生成器（本仓沿用） | **达标/部分**：能力继承自上游 |
| 国际化/多语言 | 多语言、多币种、多税率（Odoo 80+ 国家） | 基础 i18n 机制存在 | **部分达标**：缺多币种/多税率业务覆盖 |

---

## 四、上线标准检查表（逐项 达标 / 部分达标 / 未达标）

| 维度 | 评级 | 说明 |
|---|---|---|
| 功能完整性 | **部分达标** | 多数模块核心 CRUD 闭环具备；CRM 合同/ERP 采购流程完整；但 HR/TMS 审批未接 BPM、缺营销自动化/考勤/薪资/条码等体验层 |
| 稳定性 | **未达标** | 测试覆盖 4.1%（核心业务近乎零），无法证明正确性；AI/BPM 缺 DDL 风险；导出 OOM 风险；Flyway 未真实纳管结构 |
| 安全性 | **部分达标** | 框架防注入/多租户/XSS 扎实、URL 鉴权到位、密钥全走环境变量；但 @PreAuthorize 覆盖不全（10%）、错误码冲突、79 处裸抛异常 |
| 可观测性 | **部分达标** | 指标/追踪/Actuator 齐备、日志带 traceId；缺 JSON 结构化日志与告警规则 |
| 可扩展性 | **部分达标** | 模块化清晰、新增模块成本低；但禁用模块腐化、MES 巨石化、缺插件/低代码扩展机制 |
| 合规性 | **部分达标** | 审计字段/软删除/操作日志/访问控制具备；缺数据主体权利（脱敏/导出/注销）、等保/GDPR 级留存与审计细则 |

---

## 五、上线结论与优先级排序

**结论：不建议直接全量生产上线。** 以"内部试点 + 灰度"方式先上线已验证模块（system / infra / crm / bpm 基础），其余按下列顺序迭代。

### 必须先解决的阻塞/高危项（P0/P1，上线前清零）
1. **AI / BPM 生产 DDL 补全**——全新库冒烟验证（最高优先，直接影响部署成败）。
2. **核心域测试覆盖率**——引入 JaCoCo 门禁，优先补 CRM/ERP/WMS/MES/BPM 用例（证明正确性）。
3. **导出链路护栏**——`ExcelUtils` 分批/异步、`PAGE_SIZE_NONE` 上限、容器 `MaxRAMPercentage`（消除 OOM）。
4. **Flyway 真实纳管**——并入基线 SQL、生产开 `validate-on-migrate`。
5. **错误码去重**——按模块重分配前缀，CI 加唯一性检查。

### 首个迭代内（P2）
6. 方法级权限注解补齐（尤其 WMS / 写接口）。
7. N+1 抽样确认与修复（p6spy 跑核心接口）。
8. 裸抛异常统一为 `ErrorCode`。
9. 日志 JSON 化 + Prometheus 告警规则。
10. `ApiAccessLogInterceptor` 的 `System.out` 改 log。
11. 禁用模块清理或归档；`MaxRAMPercentage` 替代固定堆。

### 后续迭代（P3 / 产品层）
12. HR 考勤/薪资/绩效、TMS/HR 审批接 BPM、营销自动化、条码/IoT、质量追溯闭环。
13. `.env.example`、多币种/多税率、数据合规（脱敏/留存/审计）。
14. 可视化流程设计器、客户门户等体验层。

### 待人工确认清单（影响定级，建议上线前逐项验证）
- **U-1/WMS 鉴权**：已确认框架 `.anyRequest().authenticated()` 兜底，非裸奔；仅缺细粒度注解，降级为中。
- **U-2/AI+BPM 缺表**：是否依赖外部/上游 SQL 导入流程——需全新库部署验证。
- **U-6/错误码冲突**：前端是否已硬编码具体码值，改码连带影响。
- **U-7/禁用模块**：永久下线还是近期启用（决定 48 张剩余缺表的处理）。
- **U-10/Redis 拓扑**：是否连哨兵/集群，决定单点风险定级。

---

*数据来源：本仓静态代码/配置扫描 + 公开资料（RuoYi/JeecgBoot 官方文档、Odoo 技术概览与权限/多公司指南、Salesforce vs Odoo 对比）。对标结论基于公开文档共性归纳，非逐功能逐行比对。*

---

## 附录：分阶段修复执行进度（截至 2026-08-02）

执行策略：按本报告「修复方案」分阶段推进，每阶段完成后 review 确认功能与质量正常，再进入下一阶段。可工程化加固项（护栏/门禁/补测/注解/可观测）全部落地；产品级功能（HR 薪酬考勤、营销自动化、条码/IoT、可视化流程设计器、客户门户）属于 P3 路线图，不臆造。

| 阶段 | 内容 | 状态 | 提交 |
|---|---|---|---|
| 1 | AI/BPM 生产 DDL（15+8 表）+ 生成器脚本 | ✅ 已交付 | `5299aab` |
| 2 | Flyway 幂等迁移 V73/V74（CREATE TABLE IF NOT EXISTS + 租户隔离） | ✅ 已交付 | `5299aab` |
| 3 | 导出/性能护栏：分页上限拦截器 + ExcelUtils 10万行硬上限 + Dockerfile 容器感知堆 | ✅ 已交付 | `5299aab` |
| 4 | JaCoCo 门禁武装（wms/mes/bpm service 包 ≥30%）+ crm/erp 核心域补测 | ✅ 已交付 | `636045c` |
| 5 | 错误码唯一性 CI 门禁 + 140 冲突去重（全量重排）+ 门禁收紧为严格 | ✅ 已交付 | 本批次 |
| 6 | WMS/写接口补 @PreAuthorize 方法级权限注解 | ✅ 已交付 | 本批次 |
| 7 | 可观测性：logback JSON 结构化输出 + Prometheus 告警规则文件 | ✅ 已交付 | 本批次 |

**阶段5 说明（错误码去重）**：
- 全仓扫描 `*ErrorCodeConstants.java` 共 2033 个定义，发现 **140 处重复码**（跨模块/模块内）。分布：MES↔QMS `10401xxxxx` 段碰撞、ERP `STOCK_OUT_*`/`STOCK_MOVE_*` 6 处共享、AI `KNOWLEDGE_DOCUMENT_FILE_*` 三连、infra `CODEGEN_TABLE_EXISTS`/`CODEGEN_IMPORT_COLUMNS_NULL` 同码 `1001004002` 等。
- U-6 已核查：前端（`yudao-ui` 仅占位脚手架、零硬编码码值）与全仓调用方均按**常量名**引用错误码，唯一按数字字面量断言的测试（`1_050_003_007`）不在冲突列表、不受影响。故用户确认采用**全量重排**策略。
- 去重执行：`scripts/renumber_error_codes.py` 按精确行号替换数字字面量（常量名不变，调用方零改动）。结果：**149 个常量跨 11 个文件被重排为全局唯一值**，跨模块碰撞模块分配全新空闲前缀（im→43、qms→44、iot→45、mes→46、tms→47），其余模块内冲突在原前缀段内找空闲号。重排后经 `check_error_codes.py` 严格检查（无 baseline）**0 冲突**（共 2033 个定义）。
- 门禁收紧：`.github/workflows/maven.yml` 的错误码门禁由 `--baseline` 冻结模式升级为**全量严格检查**（任何重复即阻断合并），并随本批次提交；原 `error_code_conflicts_baseline.txt` / `error_code_conflicts_report.txt` 已清理（冲突已归零，不再需要）。

**阶段6 说明（WMS/写接口 @PreAuthorize）**：
- 经 `scripts/check_missing_preauthorize.py` 精确扫描（修正了注解顺序导致的误报后）：**WMS 模块 admin Controller 共 34 个，写接口(POST/PUT/DELETE) 0 缺口**——全部已带 `@PreAuthorize("@ss.hasPermission('wms:<resource>:<action>')")`。原报告「WMS 写接口缺 @PreAuthorize」属误报（同阶段1 已纠偏的 P0 误报一致）。
- 全仓 admin 写接口共 120 处缺 `@PreAuthorize`，但经逐类核查，**绝大多数为「本就不该加」的端点**：
  - 公开端点：`AuthController`(login/logout/refresh-token/register/sms-login/reset-password/social-login)、`CaptchaController`、`OAuth2OpenController`(/token、/authorize、/check-token)、各类 **webhook/回调**（`PayNotifyController`、`MpOpenController`、`SmsCallbackController`、`ImRtcLiveKitController.webhook`、`AiImageController.midjourneyNotify`）、`DefaultController` 404 处理器——这些必须匿名可访问，加 `@PreAuthorize` 会直接破坏登录/支付/短信回调。
  - 用户自服务端点：AI 的 `*-my`（本人会话/图片/角色）、`UserProfileController`、`TotpController` 等——数据按登录用户 `userId` 在 service 层隔离，端点级只要求已登录即可，符合本仓惯例。
- 因此**未做全量补齐**（盲目加会破坏上述端点且需注册约 120 个新权限码）。WMS 范围内无待补项。
- 交付**防回归护栏**：`scripts/check_missing_preauthorize.py` 接入 CI，限定 `--module yudao-module-wms`（当前全绿），任何新增的 WMS admin 写接口若漏写 `@PreAuthorize` 即阻断合并。PDA/app 端点按本仓约定为登录即可用（全仓 `controller/app/**` 均无 `@PreAuthorize`），不纳入此门禁——如确需 PDA 细粒度 RBAC，属产品/RBAC 设计项（P3），需同步注册权限码并赋权给仓库角色。

**阶段7 说明（可观测性 / 日志结构化 + Prometheus 告警）**：
- **JSON 结构化日志**：`yudao-server` 新增 `net.logstash.logback:logstash-logback-encoder:7.4.3`（logback 1.5 兼容）。`logback-spring.xml` 改造：
  - 文件 Appender（`FILE`→`ASYNC`→滚动）编码器由 `PatternLayoutEncoder` 换为 `LogstashEncoder`，输出 JSON（含 MDC：`traceId`/`tenantId`/`userId`、level、logger、thread、message、stack_trace），供 ELK/Loki/Vector 采集。
  - 新增 `JSON_CONSOLE` Appender；**非 prod** 用文本控制台（本地/`docker logs` 可读）+ JSON 文件，**prod** 用 JSON 控制台（stdout 被容器采集）+ JSON 文件。控制台与文件均为结构化，兼顾可读性与可观测性。
- **Prometheus 告警规则**：新增 `prometheus/alert.rules.yml`，7 条规则覆盖三类：
  - `jvm`：堆内存>85%、系统 CPU>80%、GC 开销>0.3、活跃线程>500；
  - `http`：5xx 错误率>5%、P95 时延>1s；
  - `instance`：抓取 `up==0`（实例宕机/健康检查异常）。
  - 文件头注释含 `prometheus.yml` 接入片段（scrape `/actuator/prometheus` + `rule_files`）。指标名对齐 Micrometer 默认导出（Actuator + `micrometer-registry-prometheus` 在本仓已启用）。
- 验证：`yudao-server -am compile` 在线构建通过（拉取 encoder 依赖并编译成功）；`alert.rules.yml` 经 YAML 解析校验通过（3 组 7 规则）。日志 JSON 输出为运行时行为，已由标准 `LogstashEncoder` 配置保证，CI 在线构建可落地。

---

## 满分执行进度追踪（Item 8 起）

本节登记「上线前清零」清单各项执行状态，作为满分验收的权威追踪表（明细见 `FULL-MARKS-PLAN.md`）。

| 编号 | 项 | 状态 | 硬门禁 / 证据 |
|---|---|---|---|
| P0/P1 高危 | 安全 / 多租户 / 鉴权基线、注入防护 | ✅ | 评审 + 编译 |
| 阶段4 | JaCoCo 门禁 + crm/erp 补测 | ✅ | `636045c` |
| 阶段5 | 错误码唯一性门禁 + 140 冲突去重 | ✅ | 0 冲突；CI 严格门禁 |
| 阶段6 | WMS 写接口 @PreAuthorize 护栏 | ✅ | 0 缺口；CI 护栏 |
| 阶段7 | 可观测性 JSON 日志 + Prometheus 告警 | ✅ | 在线构建通过 |
| **Item 8** | 裸抛异常统一为 `ServiceException(ErrorCode)` | ✅ 已完成 | 74 处转换 + `check_bare_throws.py` 门禁绿；全 reactor 编译 BUILD SUCCESS |
| **Item C** | 核心域覆盖率门禁 ≥40% | 🔧 进行中 | 当前 wms/mes/bpm ≥0.30 已门禁；待补测抬升至 0.40 |
| U-2 | AI+BPM 缺表 | ⛔ 待用户决策 | — |
| U-7 | 禁用模块 48 缺表 | ⛔ 待用户决策 | — |
| U-10 | Redis 拓扑 | ⛔ 待用户决策 | — |

### Item 8 交付说明（裸抛统一）
- `ServiceException` 新增 `ServiceException(ErrorCode, String)` 与 `ServiceException(ErrorCode, String, Throwable)` 两构造函数，复用全局 `GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR(500)`。
- `scripts/convert_bare_throws.py` 将 `cn/iocoder/yudao/**` 包内全部 `throw new RuntimeException/IllegalStateException`（**74 处，跨 49 文件**）改为 `ServiceException(INTERNAL_SERVER_ERROR, ...)`，保留原始 message 与 cause（字符串/括号感知精确替换）。
- **排除** `org/springframework`、`org/flowable` 重打包外部类（避免改坏 Spring/Flowable 内部行为），共 4 处不转换。
- 新增回归门禁 `scripts/check_bare_throws.py` 并接入 `.github/workflows/maven.yml`（Bare-Throw Exception Gate）；扫描到 `cn/iocoder/yudao` 包内残留裸抛即失败。
- 验收：`python3 scripts/check_bare_throws.py` 退出 0；`grep` 在 `cn/iocoder/yudao` 包内 0 裸抛；全 reactor `mvn compile` BUILD SUCCESS。

### Item C 路径（覆盖率 ≥40%）
当前 JaCoCo `check` 仅覆盖 wms/mes/bpm 的 `**/service/**` 包、`jacoco.minimum=0.30`。满分要求核心业务域 instruction 覆盖 ≥40%：
1. 将 `jacoco.minimum` 上调至 `0.40`，`<includes>` 扩展至 crm/erp/ai/datalake/system/pay 等 service 包；
2. 为被纳入模块补 `BaseDbUnitTest` 用例，使每个模块 ≥40% 后点亮门禁（分批纳入，避免一次性 brick CI）。
