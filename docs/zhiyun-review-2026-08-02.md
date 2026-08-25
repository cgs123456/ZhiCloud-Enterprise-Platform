# 智云·企业数字化平台（ZhiCloud）— 代码与架构复查 · 行业对标 · 上线评估

> 评审日期：2026-08-02 ｜ 版本基线：master（`fdf0bdd` 之后）｜ 技术栈：Spring Boot 3.5.16 / JDK 21 / Maven / H2(测试)+MySQL(生产) / Flyway
> 方法：静态代码审计（grep/Read 实证）+ 行业对标公开资料检索 + CI/部署产物实测复现。
> **透明说明**：首轮代码审计代理（Explore）因内部故障未返回，已改用稳健代理重启；本报告所有"代码级"结论均来自实测复现，对标结论已按本 fork 实际代码**修正**（见文末"修正记录"）。

---

## 0. 总体结论（TL;DR）

智云不是"玩具 Demo"，而是基于 zhicloud/ruoyi-vue-pro 做了**实质性扩展**的企业级 fork：已具备总账+财务报表、租户感知 RAG、RAGAS 式评估、Helm/ArgoCD/Canary 部署、SBOM 与依赖扫描、质量门禁。但其"广度达标、深度不足、闭环缺失、生产工程薄弱"的特征明显——**距离"可全面上线（尤其政企/金融/regulated 多租户规模化）"存在结构性差距**；作为中小客户 MVP / 内部系统 / 作品集则具备较强基础。

**已验证的平台亮点（加分项，作品集可强调）**
- 工程化：CI 含错误码唯一性门禁、`WMS @PreAuthorize` 门禁、裸抛异常门禁、**事务原子性门禁**、`mvn verify` 全量测试、security-check.sh、OWASP 依赖扫描、CycloneDX SBOM。四道自研静态门禁均由本次复查中发现的真实缺陷驱动补齐，并以「基线为 0 + 显式豁免需写明理由」的方式防回归。
- 安全基座：RBAC（`@SaCheckPermission`）、字段级加密（`EncryptTypeHandler`）、OAuth2/社交登录、向量库 `tenant_id` 强制过滤（防跨租户泄露）、AI-RAG 四指标评估。
- 业务深度：ERP 已含总账/凭证/账套/资产负债表/现金流量表/合并引擎；AI-RAG 已含评估体系与 `@PreAuthorize`。
- 部署：Helm + ArgoCD + K8s（含 canary 灰度）。

> **本轮增量（2026-08-02 第三阶段加固）**：全仓 21 模块事务原子性扫描，去除两类扫描器误报后确认真实缺口 12 处并全部修复，其中 4 处为高危——CRM 跟进记录 8 写无事务、**停用用户后令牌未撤销的越权窗口**、批量令牌撤销半成功、**限时折扣关闭不彻底导致按折扣价成交的直接资损**。另独立发现并修复 AI-RAG「向量库删除失败被静默吞掉、DB 记录照删」导致已删文档仍被 RAG 召回的孤儿向量缺陷。详见 §1.3。

**最致命的结构性缺口**
1. 等保 2.0 三级合规缺位（政企/金融一票否决）。
2. 跨模块最终一致性无 Saga / 对账补偿机制（单体争抢同一 DB）——模块内原子性已在本轮补齐，但跨模块链路仍无补偿。
3. 无企业级 SSO（SAML/OIDC）+ MFA + 字段级/记录级权限 + 审计日志防篡改留存。
4. 可观测性停留在"能看"，无 SLO/错误预算/on-call。
5. MES 无设备实时接入（OPC-UA）、QMS 无 SPC/CAPA 闭环、TMS 无 VRP、OA/HR 无薪酬考勤等业务域。

---

## 1. 问题清单（模块 ｜ 问题描述 ｜ 严重级别 ｜ 影响范围 ｜ 修复建议）

> 严重级别：阻塞 = 阻断上线/CI 红；高 = 上线前必须解决（安全/合规/核心闭环）；中 = 上线后短期补；低 = 体验/优化。

### 1.1 测试与 CI（已实测）

| 模块 | 问题描述 | 严重级别 | 影响范围 | 修复建议 |
|---|---|---|---|---|
| CRM | 测试上下文加载失败：`BaseDbUnitTest` 找不到 `classpath:/sql/create_tables.sql`，致 CI 全量 `mvn verify` 在 CRM 处 18/18 Error，后续所有模块 SKIP。现已补文件并**提交 git**，本地重跑 24/24 全绿 | 阻塞→**已修复** | CI 整条流水线 | 推送触发 CI 复跑确认全量反应器下通过；将"测试 schema 存在性"纳入 CI 预检 |
| ai-multiagent / datalake / iot / mall / mp | **零测试类**（实测 `find *Test.java` = 0），模块无任何自动化测试 | 高 | 这些模块回归无保障 | 至少补核心 Service 的 `BaseDbUnitTest` 冒烟测试；新模块禁止零测试合入（加 CI 门禁） |
| 全模块 | 测试覆盖严重不均：MES 46、system 38、WMS 26、infra 26、im 21、bpm 20；erp/qms 仅 6、crm/tms 4、ai-rag 2、oa/hr/ai 3 | 中 | 整体质量置信度低 | 按风险定覆盖率红线（核心域 ≥60%），纳入 CI 门禁 |

> **门禁逃生舱的设计原则（本轮补充）**：提交前全量复跑四道门禁时，`Bare-Throw Exception Gate` 报出 1 处新命中——`DatabaseMigrationRunner`（Flyway 迁移 CLI，运行在 K8s PreSync Job 里、**脱离 Spring 容器**）的 `requireEnv` 抛了 `IllegalStateException`。
> 这里**不应**机械套用 `ServiceException`：后者必须携带一个注册在全局错误码表里的 `ErrorCode`，而该异常只会被自身 `catch(Throwable)` 接住转成非 0 退出码，永远不会返回给任何 API 调用方，硬加错误码纯属污染错误码空间。
> 处置方式与事务门禁的 `@tx-ignore` 保持同一套约定：在 throw 上方写 `// @bare-throw-ignore <理由>` 显式豁免，**理由随代码进入 review**，且脚本会把全部豁免项打印到 CI 日志，防止悄悄累积成新的技术债——而不是在脚本里维护一份不可见的白名单。

### 1.2 安全（已实测 + 对标）

| 模块 | 问题描述 | 严重级别 | 影响范围 | 修复建议 |
|---|---|---|---|---|
| 全平台 | 无企业级 SSO（SAML 2.0 / OIDC）对接企业 IdP，无 SCIM 账号生命周期同步 | 高 | 政企/集团客户 | 集成 Keycloak / 商业 IAM，提供 OIDC/SAML |
| 全平台 | 无 MFA（TOTP/FIDO2/国密 UKey） | 高 | 等保 2.0 三级硬门槛 | 接入 MFA；关键操作双因素 |
| CRM / 全平台 | CRM 仅菜单/按钮 + 部门数据权限，**无字段级权限、无记录级共享规则**（客户手机号对全员可见） | 高 | 数据泄露（合规） | 引入字段级 FLS + Sharing Rules；敏感字段脱敏 |
| 全平台 | 操作/访问日志无**防篡改**与**留存策略**（疑似可被管理员直接 DELETE） | 高 | 等保/审计追溯 | 日志写 WORM/只增表或独立存储；定义 ≥6 个月留存 |
| AI-RAG | 向量库 `tenant_id` 过滤已做（P0-2 修复），但无回归测试固化，且文档级 ACL/密级过滤粒度待补 | 中 | 多租户越权残留风险 | 补跨租户检索的单元测试；增加文档密级 metadata 过滤 |
| 全平台 | `@PreAuthorize` CI 门禁**仅覆盖 WMS admin**；其他模块 admin 写接口无统一门禁 | 中 | 鉴权回归 CI 不拦 | 将门禁扩展到 ERP/MES/QMS/CRM 等 admin 控制器 |
| 全平台 | 静态扫描确认：**所有 MyBatis `${}` 均为枚举常量 OGNL 引用 `${@XxxEnum@CONSTANT}`，无用户可控输入进 SQL** | 正向(低) | 注入风险低 | 维持；CI 加 `${}` 用法静态检查防止退化为拼接 |

### 1.3 数据一致性与事务（全仓 21 模块扫描 + 逐条调用链核验 + **已修复**）

扫描方法：`scripts/scan_tx.py`——识别「单个方法内写入 ≥2 个不同 Mapper/Service 却未标注事务注解」的方法，再逐个回溯调用链，排除「调用方已开事务（默认 REQUIRED 传播会加入外层事务）」的假阳性。

> **扫描器两轮自纠（重要）**：初版脚本存在两类系统性误报，均已修复，此前基于初版结果的结论作废。
> 1. **跨行注解漏读**：脚本只检查方法签名紧邻的上一行，当 `@LogRecord(type=…, subType=…,\n  success=…)` 这类跨行注解夹在中间时，更上方的 `@Transactional` 被漏看。CRM 一度报出 11 处，实际其中 10 处早已有事务，**真实缺口只有 1 处**。现改为按括号配平向上回溯完整注解块。
> 2. **只认 `@Transactional`**：本项目多数据源场景使用 dynamic-datasource 的 `@DSTransactional`（同样提供本地事务语义），`TenantPackageServiceImpl.updateTenantPackage` 因此被误报。现已将 `@DSTransactional` / `@GlobalTransactional` 一并识别为事务注解。
>
> 教训：静态扫描结论必须经人工回溯核验后才可写入报告，脚本本身也需要被审计。

扫描范围扩大到全部 21 个业务模块（原报告仅覆盖 6 个核心模块），去除误报后**确认真实缺口 12 处，本次已全部修复**：

| 模块 | 方法 | 问题与影响 | 严重级别 | 处置 |
|---|---|---|---|---|
| **CRM** | `CrmFollowUpRecordServiceImpl.createFollowUpRecord`(L66, **写 8 处**) | Controller 直接入口且外层无事务。先 insert 跟进记录，再按 bizType 更新 客户/商机/线索/联系人/合同，最后批量更新 contactIds/businessIds 的 nextTime。任一步失败即留下「孤儿跟进记录 + 业务对象跟进时间未更新」，无自愈 | **高** | ✅ 已补 `@Transactional` |
| **系统安全** | `AdminUserServiceImpl.updateUserStatus`(L256) | 禁用用户时先改 status 再撤销令牌。若撤销失败，账号在 DB 中已封禁但持有的 access token 仍有效，形成**「已封禁账号仍可调用接口」的越权窗口** | **高** | ✅ 已补 `@Transactional` |
| **系统安全** | `OAuth2TokenServiceImpl.removeAccessToken(Long,Integer)`(L159) | 同文件的 `removeAccessToken(String)` 重载已有事务，此批量重载遗漏。循环撤销令牌中途失败 => 部分令牌已撤销、部分仍有效，而调用方（改密码/强制下线/停用用户）会认为已全部踢出 | **高** | ✅ 已补 `@Transactional`（Redis 删除不受事务管理，回滚方向为「Redis 已清、DB 保留」，令牌校验以 Redis 为准，属 fail-safe） |
| **商城促销** | `DiscountActivityServiceImpl.closeDiscountActivity`(L170) | 活动状态与活动商品状态两表联动。第 2 步失败则活动显示「已关闭」但商品仍挂生效中折扣，**用户继续按折扣价成交 => 直接资损**，且运营在后台看到「已关闭」不会察觉 | **高** | ✅ 已补 `@Transactional` |
| BPM | `BpmModelServiceImpl.cleanModel`(L320, 写 6 处) | 姊妹方法 `deleteModel` 已有事务，本方法遗漏。三段清理跨 4 个 Flowable Service，中途失败残留「运行时已删但历史仍在」的孤儿数据，流程管理台查询报错且无法自愈 | 中-高 | ✅ 已补 `@Transactional`（已注明：万级实例场景应改分批异步清理以避免长事务锁 `ACT_RU_*`） |
| AI 知识库 | `AiKnowledgeDocumentServiceImpl.updateKnowledgeDocumentStatus`(L139) | 禁用文档时若切片删除失败，文档已标记禁用但切片仍在检索范围内 => **已下线文档仍被 RAG 召回** | 中-高 | ✅ 已补 `@Transactional` |
| AI 知识库 | `AiKnowledgeDocumentServiceImpl.updateKnowledgeDocument`(L119) | 文档更新与旧切片删除不原子，导致检索结果与文档设置不符 | 中 | ✅ 已补 `@Transactional` |
| IM | `ImPrivateMessageServiceImpl.readPrivateMessages`(L242) | 回执置 DONE 与推进 `im_conversation_read` 读位置不原子。读位置是「唯一权威」，分叉后对方 UI 显示已读、本端未读数不清零，长期不自愈 | 中 | ✅ 已补 `@Transactional` |
| 商城客服 | `KeFuMessageServiceImpl.sendKefuMessage`(L81) | 同文件 `updateKeFuMessageReadStatus` 已有事务，本方法遗漏。会话「最后一条消息」冗余字段更新失败 => 客服端看不到新消息而用户以为已送达 | 中 | ✅ 已补 `@Transactional` |
| 商城商品 | `ProductPropertyServiceImpl.deleteProperty`(L77) | 同文件 `updateProperty` 已有事务。属性已删但属性值残留，且因属性不存在，孤儿属性值再无法从后台清理 | 中 | ✅ 已补 `@Transactional` |
| 商城商品 | `ProductPropertyValueServiceImpl.updatePropertyValue`(L54) | 属性值改名后 SKU 冗余名未同步 => 前台规格展示错乱 | 中 | ✅ 已补 `@Transactional` |
| 商城促销 | `DiscountActivityServiceImpl.deleteDiscountActivity`(L185) | 残留孤儿 `discount_product`，主活动已删无法再清理 | 中 | ✅ 已补 `@Transactional` |
| ERP | `ErpPurchaseOrderServiceImpl.updatePurchaseOrderInCount`/`ReturnCount`(L227,247)、`ErpSaleOrderServiceImpl.updateSaleOrderOutCount`/`ReturnCount`(L285,307) | 循环内逐项 `updateById`，中途抛「数量超限」异常会使订单头与订单项数量撕裂。**核验结论：调用方 `ErpPurchaseInServiceImpl` / `ErpSaleOutServiceImpl` 各入口均已开事务，实际运行时已被外层覆盖** | 低(防御性) | ✅ 已补注解，明确方法自身的事务契约（REQUIRED 复用外层事务，行为不变） |

**有意不加事务的例外（已在代码中显式豁免）**：

| 模块 | 方法 | 为何不能加 |
|---|---|---|
| MP 公众号 | `MpMessageServiceImpl.receiveMessage`(L76) | 方法体内含最长 3×5=15 秒的 `ThreadUtil.sleep` 轮询、微信 `userInfo` API 调用、`downloadMessageMedia` 媒体下载。包事务会让单条消息占用一个数据库连接 15 秒以上，消息高峰期迅速耗尽连接池拖垮整个应用——**长事务的危害远大于此处的数据不一致**。语义上两处写入也相互独立（补建粉丝档案本身有价值，不应因消息落库失败而回滚）。正确加固方向是拆成两个独立短事务，已在代码注释中记录 |
| AI-RAG | `AiragDocumentServiceImpl.deleteDocument`(L55) | 跨存储（向量库 + MySQL）删除，`@Transactional` 管不到向量库，加上只会造成「已加事务」的错觉。改为用**删除顺序 + 失败即中止**保证收敛，详见下条 |

**同时发现并修复一个独立的跨存储一致性缺陷（原报告未覆盖）**：

| 模块 | 问题描述 | 严重级别 | 影响范围 | 处置 |
|---|---|---|---|---|
| **AI-RAG** | `AiragDocumentServiceImpl.deleteDocument` 与 `AiragRagServiceImpl.deleteDocument` **两层都用 `try/catch` + `log.warn` 吞掉向量库删除异常**（外层 catch 实为死代码），随后照常删除文档 DB 记录。结果：文档从后台消失，向量却永久残留在检索库中——**用户以为已删除的文档仍会被 RAG 召回**。更严重的是清理向量所需的 `chunkCount` 随 DB 记录一起消失，此后再无任何入口可回收这批孤儿向量。这既是数据残留也是合规风险（用户要求删除的内容仍在被 AI 输出） | **高** | 所有启用向量库的 RAG 知识库 | ✅ 内层改为抛 `RAG_VECTOR_DELETE_FAIL`(1-041-002-004)，外层移除死 catch。删除顺序保持「先向量后 DB」不可颠倒（DB 记录是定位向量的唯一线索）。失败则保留 DB 记录、用户可重试；chunkId 由 documentId 确定性生成，重复删除幂等，路径最终收敛 |

**防回归**：`scan_tx.py` 已强化并作为 `Transaction Atomicity Gate` 接入 `.github/workflows/maven.yml`——支持 `@DSTransactional`/`@GlobalTransactional` 识别、跨行注解回溯、`// @tx-ignore <理由>` 显式豁免（豁免理由必须写在代码里、可被 review，而非藏在脚本白名单），发现缺口即以非 0 退出码阻断构建。当前全仓扫描结果：**0 处缺口**。

**尚未解决的架构级一致性问题（非本次修复范围）**：

| 层面 | 问题描述 | 严重级别 | 影响范围 | 修复建议 |
|---|---|---|---|---|
| 跨模块 | 无 Saga / 对账补偿机制保证跨模块最终一致性（如 ERP 记账↔WMS 出库↔CRM 回款）。单机 `@Transactional` 只能保证模块内原子性，跨模块调用链断裂后无自愈 | 高 | 数据对错账无自愈 | 引入事务消息/Saga；关键链路加定期对账任务 |
| 单体架构 | MES 高频写入与 ERP/CRM 共用同一 DB 与连接池，缺容量隔离 | 中 | 峰值互相拖垮 | 读写分离/模块库拆分（见可扩展性） |

### 1.4 功能闭环（对标缺口，详见第 2 节）

| 模块 | 问题描述 | 严重级别 | 影响范围 | 修复建议 |
|---|---|---|---|---|
| ERP | "业务单据→凭证"自动生成缺失（未检索到 `generateVoucher`）；利润表独立性、成本核算深度待补 | 中-高 | 财务闭环不完整 | 补凭证自动生成引擎；对齐 ERPNext 会计基线 |
| WMS | 无波次拣选/上架策略引擎/PDA 无纸化/批次效期 FEFO 闭环 | 高 | 仓储效率不如 Excel+人工 | 自研策略引擎 + PDA 端点 |
| MES | 无设备实时接入（OPC-UA/Modbus/MQTT）、无 OEE/Andon/APS | 高 | "电子台账"≠MES，价值归零 | 集成边缘网关 + 自研 OEE |
| QMS | 无 SPC 控制图/Cpk、无 NCR→MRB→8D/CAPA 闭环、无 FMEA/APQP | 高 | 体系认证核心可审计项缺失 | 自研闭环 + 集成统计库 |
| TMS | 无 VRP 路径优化、承运商比价、运费自动对账 | 高 | 运输成本不可控 | 集成 VRP 求解器 |
| OA/HR | 无薪酬引擎（个税累计预扣+专项附加）、无考勤规则引擎、无 HR 业务域 | 高 | OA/HR 仅为示例 | 自研薪酬/考勤 |
| BPM | 相对最强；缺运行中实例版本迁移、DMN、SLA 达成率统计 | 中 | 生产改流程踩坑 | 补实例迁移 + DMN |

### 1.5 可观测 / 部署 / 稳定性

| 模块 | 问题描述 | 严重级别 | 影响范围 | 修复建议 |
|---|---|---|---|---|
| 全平台 | 无 SLO/错误预算/告警分级/on-call；监控停留在"能看" | 中-高 | 故障被动发现 | 引 OTel + Prometheus + Grafana + 告警 |
| 全平台 | 无压测容量基线；单体无容灾 RTO/RPO 定义 | 中 | 峰值/故障无预案 | 压测定基线；定义 RTO/RPO；考虑多活 |
| 部署 | Helm/ArgoCD/Canary 已具备（正向） | 正向 | — | 维持；补蓝绿/回滚演练 |

---

## 2. 对标差距表（功能模块 ｜ 行业常见做法 ｜ 本项目现状 ｜ 差距与优先级）

> 行业做法引用见各模块来源（Salesforce/HubSpot、ERPNext/Odoo/用友/金蝶、Manhattan/富勒、Siemens Opcenter/黑湖、ETQ/盖勒普、RAGAS/NeMo Guardrails、BPMN/DMN、泛微/北森 等）。
> **本项目现状已按本 fork 实测修正**（ERP/AI-RAG 远超上游 zhicloud 基线）。

| 功能模块 | 行业常见做法（成熟产品） | 本项目现状（实测） | 差距与优先级 |
|---|---|---|---|
| **CRM** | 线索评分(MQL/SQL)、去重合并、Territory、加权预测、CPQ、Case+SLA、客户健康分、营销自动化、字段级权限 | 线索→客户→商机+公海；业绩目标/排行榜；**无评分/去重合并/Territory/预测/CPQ/工单SLA/营销自动化**；**无字段级权限** | 高(CPQ/工单SLA/字段权限)、中(其余) |
| **ERP** | 总账 GL+三大报表、成本核算、应收应付+发票税务、库存多计价+FEFO、MRP、P2P 三单匹配、固定资产+预算 | **已具备**：总账/凭证/账套、资产负债表、现金流量表、合并引擎；缺**利润表独立性、业务单据→凭证自动生成、成本核算深度、发票税务、MRP、三单匹配** | 中-高（自动凭证/税务/MRP 为高） |
| **WMS** | 波次拣选+路径优化、智能上架、越库、Slotting/ABC、循环盘点闭环、PDA 无纸化、批次/序列号/效期+FEFO、ASN、LMS、3PL 多货主/计费 | 出入库/调拨/盘点基础单据；**缺波次/上架策略/PDA/效期 FEFO 闭环/ASN/计费** | 高 |
| **MES** | OPC-UA/Modbus/MQTT 设备接入、OEE、Andon、APS 有限产能、eSOP 过站防错、谱系追溯(ISA-95)、eBR(21CFR11)、CMMS | 基本为工单电子台账；**无设备接入/OEE/Andon/APS/谱系/eBR**；未按 ISA-95 分层 | 高（设备接入 P0） |
| **QMS** | SPC(Cpk≥1.67)+OCAP、NCR→MRB→8D/CAPA 闭环、FMEA/APQP/PPAP、MSA、供应商质量(SCAR/PPM)、IQC~OQC 抽样(GB2828)、DCC 文控 | 基础质检单据；**无 SPC/CAPA 闭环/FMEA/PPAP/MSA/供应商质量/抽样引擎/文控** | 高 |
| **TMS** | VRP 路径优化、承运商比价/Rate Shopping、运费引擎+自动对账、在途 ETA、司机端 POD、多式联运、KPI | 基础运单状态流转；**无 VRP/比价/自动对账/ETA/POD** | 高 |
| **BPM** | 完整 BPMN2.0+边界事件、实例版本迁移、DMN、多实例动态集合、表单深度、流程挖掘/SLA | **相对最强**：双设计器+会签/或签/加签减签/驳回/转办齐全、节点级字段权限；缺实例迁移/DMN/SLA 达成率 | 中（深度，非有无） |
| **OA/HR** | 薪酬引擎(个税+专项附加)、考勤规则、组织人事、绩效、招聘 ATS、公文/印章/门户、ESS/MSS | OA 通知公告+站内信(请假为示例)；HR 仅 RBAC 三张表；**无薪酬/考勤/HR 域/公文印章** | 高（薪酬/考勤） |
| **AI-RAG** | 评估体系(RAGAS)、三层 Guardrails、引用溯源、权限感知检索、混合检索+rerank、语义分块、摄入工程、可观测/成本 | **已具备**：向量层 `tenant_id` 强制过滤、RAGAS 四指标评估、控制器 `@PreAuthorize`；缺**混合检索+cross-encoder rerank、语义分块、引用溯源粒度、摄入增量/删除传播、可观测成本** | 高(混合检索/rerank ROI 最高)、中(其余) |

### 生产就绪度六维评估

| 维度 | 行业做法 | 本项目现状 | 评级 |
|---|---|---|---|
| 功能完整性 | 核心域必须闭环（ERP 有总账、QMS 有 CAPA、MES 有设备数据、WMS 有 PDA） | BPM/权限/基础设施接近可用；ERP/CRM 半成品（有单据缺内核/自动凭证）；WMS/MES/QMS/TMS/OA-HR 深度不足；AI-RAG Demo→可用 | **部分达标** |
| 稳定性 | 压测基线、灰度/蓝绿、容灾 RTO/RPO、跨模块 Saga 对账 | 有 Redis 锁/幂等/限流/SBA；缺压测/容灾/一致性机制；单体争抢 DB | **部分达标** |
| 安全性 | SAML/OIDC+MFA、字段/记录级权限、三权分立、审计防篡改留存≥6月、国密、KMS | 有 RBAC/字段加密/OAuth2/租户感知 RAG；缺 SSO/MFA/字段权限/审计防篡改/国密/等保 | **未达标（等保场景 P0）** |
| 可观测性 | OTel 统一埋点 + Prometheus/Grafana + SLO/错误预算 + on-call；异步/MQ 覆盖 | SkyWalking+SBA+监控；缺 OTel/SLO/告警/on-call/MQ 链路 | **部分达标** |
| 可扩展性 | 插件/扩展点、API 版本化/开放平台、多租户三档隔离、Webhook | 多模块 Maven/代码生成器/SaaS 多租户/Swagger/信创库；缺**扩展点插件机制**(定制污染主干)、API 版本化、Schema/独立库隔离、Webhook | **部分达标** |
| 合规性 | 等保2.0 三级、个保法/GDPR(同意/被遗忘权)、行业标准(IATF/21CFR11/GMP/GB2828) | 基本未成体系；无合规矩阵/数据分级/国密/等保准备；AI-RAG 引入"删文档残留向量"新型风险 | **未达标** |

---

## 3. 上线结论

### 3.1 是否可上线

**分场景判定（不可一刀切）：**

- ✅ **可上线（MVP / 内部系统 / 非 regulated 中小客户 / 作品集演示）**：核心域 CRUD 闭环、RBAC、基础 ERP(总账+报表)/CRM/WMS 作业、BPM 工作流、AI-RAG 知识问答均已具备且含质量门禁。**前置条件**：补齐零测试模块、加 SLO 监控、做一轮压测、CRM 门禁 CI 复跑通过。
- ⛔ **不可上线（政企/金融/集团多租户规模化/regulated 行业）**：缺等保 2.0 三级（身份双因素、审计留存、国密）、SSO/MFA、跨模块一致性、可观测 SLO、容灾 RTO/RPO。其中等保与一致性为**一票否决项**。

### 3.2 必须先解决的阻塞项（P0）

1. **等保 2.0 合规准备**（若目标客户含政企/金融）：SSO+MFA、审计日志防篡改+留存、字段级权限、国密算法、三权分立。
2. **跨模块最终一致性**：Saga / 事务消息 + 定期对账（ERP↔WMS↔CRM 资金/库存/回款对账）。
3. **CI 全绿固化**：CRM 门禁已修，需推送复跑确认；零测试模块加冒烟测试门禁；`@PreAuthorize` 门禁扩展到全 admin。
4. **安全加固**：AI-RAG 跨租户检索回归测试固化；敏感字段脱敏；日志防篡改。

### 3.3 建议迭代优先级排序

| 优先级 | 项 | 性质 | 动作 |
|---|---|---|---|
| **P0** | 等保 2.0 合规（SSO/MFA/审计/字段权限/国密） | 合规一票否决 | 集成 Keycloak + 自研字段权限 + 审计 WORM |
| **P0** | 跨模块一致性（Saga/对账） | 数据正确 | 事务消息 + 对账任务 |
| **P0** | CI 全绿 + 零测试模块冒烟 + 全 admin `@PreAuthorize` 门禁 | 质量基线 | 扩展现有 CI 门禁 |
| **P1** | ERP 业务单据→凭证自动生成 + 利润表 + 成本核算 | 财务闭环 | 自研（对齐 ERPNext） |
| **P1** | WMS 波次/上架策略/PDA 无纸化/FEFO | 仓储效率 | 自研策略引擎 + PDA |
| **P1** | MES 设备接入(OPC-UA)+OEE+Andon | MES 价值 | 边缘网关 + 自研 |
| **P1** | QMS SPC+CAPA 闭环 / TMS VRP+对账 / OA-HR 薪酬考勤 | 业务域闭环 | 自研 + 集成求解器 |
| **P1** | 可观测性 SLO/错误预算/on-call（OTel+Prom+Grafana） | 稳定性 | 集成（高 ROI） |
| **P2** | AI-RAG 混合检索+rerank+语义分块+摄入工程 | 精度/合规 | 集成（单项 ROI 最高） |
| **P2** | 插件扩展点机制 + API 版本化 + Webhook | 可维护性/可扩展 | 自研（决定 3 年后是否可维护） |
| **P2** | BPM 实例迁移/DMN/SLA 统计；CRM CPQ/工单 SLA/字段权限 | 深度 | 自研 |

> **动作分类**：APS/VRP/SPC/MSA/RAGAS 评估/身份认证/可观测——**优先集成成熟方案**（自研性价比极低）；ERP 总账成本/WMS 策略/QMS 闭环/权限模型/插件扩展点——**必须自研**（业务耦合深）；3PL 计费/3D 装载/CMMN/GraphRAG/eBR——**可暂缓**（除非进入制药/汽车链）。

---

## 4. 附：对标推断 vs 本 fork 实测修正记录

1. **ERP「无总账」不成立**：实测存在 `ErpGlVoucherController`/`ErpGlAccountController`/`ErpAccountBookController`/`ErpFinancialStatementController`/`ErpBalanceSheetRespVO`/`ErpCashFlowStatementRespVO`/`ErpConsolidationEngineController`——总账、凭证、账套、资产负债表、现金流量表、合并引擎**均已具备**。缺口收窄为：利润表独立性、业务单据→凭证自动生成、成本核算深度、发票税务。
2. **AI-RAG「无评估/无租户过滤」不成立**：实测 `RagAdvisor.java:256` 显式 `builder.eq("tenant_id", tenantId)` 在向量查询层强制过滤（含 P0-2 安全修复注释）；`RagEvaluationResult` 含 faithfulness/answerRelevancy/contextPrecision/contextRecall/overallScore，`RagEvaluationServiceImpl` 实现四项 RAGAS 指标；AI-RAG 控制器普遍有 `@PreAuthorize`。缺口收窄为：混合检索+rerank、语义分块、引用溯源粒度、摄入工程(增量/删除传播)、可观测成本。
3. **MyBatis `${}` 注入风险不成立**：全仓 `${}` 均为枚举常量 OGNL 引用 `${@XxxEnum@CONSTANT}`，无用户可控输入进入 SQL，注入风险低。

> 说明：上述修正表明本 fork 对上游 zhicloud 做了**超出预期的纵深扩展**，对标表"本项目现状"列已据此调整；未逐项改写的行（WMS/MES/QMS/TMS/OA-HR/BPM）仍以行业基线对照本 fork 实测代码判断，建议上线前由专项审计逐模块代码级确认。
