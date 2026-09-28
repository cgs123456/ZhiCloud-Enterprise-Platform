# 智云企业数字化中台升级方案

> 版本：v1.1（方案基线）
> 日期：2026-09-27
> 适用仓库：`ZhiCloud-Enterprise-Platform`
> 当前分支：`upgrade/production-readiness`
> 当前 HEAD：`d7994f60e12ef1b93baede2ad811b15cc3f533f1`
> 交付状态：00-14 方案基线已形成；V115-V117 已在 MySQL 8.4.9 clean 沙箱全新库端到端执行并通过验证与重跑幂等；V116/V117 的并发重复拒绝、软删除语义和 100,000 行合成 DDL 成本已通过 MySQL 沙箱证据；MySQL bootstrap runner 已通过沙箱创建/noop/fail-closed/rotate 与只读审计；存量生产库全链、非 MySQL、生产规模 MDL/复制影响、BPM 回滚、TLS/K8s/灾备仍未闭环，生产就绪结论不变

## 0. 一句话结论

本项目已经具备企业数字化中台的**广度基础**，但不能直接作为生产环境上线。当前应先把安全基线、数据一致性、核心业务闭环和部署证据补齐，再扩展 SRM/EAM/PLM/BI 等新域。

## 1. 目标范围

平台统一承载以下业务域：

| 业务域 | 定位 |
|---|---|
| ERP | 财务、采购、销售、库存、成本、资产、税务、预算、合并 |
| MES | 工单、排产、报工、物料、设备、质量、能源、TPM |
| WMS | 收货、上架、拣货、复核、包装、发运、盘点、SN、FEFO、3PL |
| QMS | IQC/IPQC/OQC、NCR、CAPA、8D、SPC、MSA、FMEA、电子签名 |
| CRM | 客户、线索、商机、合同、回款、服务、CPQ |
| HR | 组织、员工、考勤、薪酬、绩效、招聘、合同、社保 |
| OA | 审批、公文、知识库、会议、日程、公告 |
| TMS | 承运商、车辆、司机、运单、GPS、运费、对账、车队经营 |

建议后续新增：

- SRM：供应商准入、询报价、采购协同、绩效。
- EAM：设备台账、点检、维修、备件、工单。
- PLM：产品、BOM、ECN、工艺、图纸。
- BI：指标平台、数据仓库、自助分析、驾驶舱。
- 可选：CDP、IoT、ESG、合规审计。

## 2. 硬约束

1. 本文档以企业数字化中台为目标，不扩展与八大业务域无关的方向。
2. 当前状态必须保持：
   - `production_ready=false`
   - `gate3_passed=false`
3. 不得用假 Secret、假证书、假客户端凭据或放宽扫描规则消除 P0。
4. 模拟数据只能进入 `demo-data/`、演示库、测试库或沙箱，禁止导入生产。
5. 任何“已通过”的结论必须能指向当前提交对应的证据文件。
6. 真实 TLS 握手、证书链、主机名验证、K8s 服务端 dry-run、真实生产部署未完成前，不得宣称可生产部署。

## 3. 当前已核实事实

### 3.1 代码与模块

| 指标 | 当前核验值 |
|---|---:|
| ERP Controller | 70 |
| MES Controller | 148 |
| WMS Controller | 38 |
| QMS Controller | 28 |
| CRM Controller | 32 |
| HR Controller | 13 |
| OA Controller | 12 |
| TMS Controller | 9 |
| Java 测试文件总数 | 385（当前工作区统计） |
| 最近迁移版本 | V117（已在 MySQL 8.4.9 clean 沙箱执行；存量库全链尚未通过） |

模块测试分布严重不均，QMS、CRM、HR、OA、TMS 的测试文件明显不足。详见 `01-平台现状核验.md`。

### 3.2 当前门禁

| 门禁 | 当前结果 | 说明 |
|---|---|---|
| Maven CRM 定向测试（V117） | 16 tests passed | 回款并发、幂等和余额保护定向用例通过；不等于全量模块或生产部署通过 |
| Maven CRM 模块测试（V117） | 42 tests passed | 包含上述 16 个定向用例，不能与定向结果相加；不等于生产验证 |
| Maven 迁移 runner（V117） | 7 tests passed | runner 单元/组件测试通过，另有 clean 沙箱端到端执行；均不等于生产迁移通过 |
| Python 全量生产门禁 | 303 tests OK（skipped=3） | 3 个 skip 均为 Docker daemon 不可用；当前证据 `python-full-20260927_215221.log`；另有 81 个专项测试 OK |
| Helm 3/4 本地渲染与离线校验 | `status=passed` | 6 个正向、8 个负向 fail-closed、12 个 kubeconform 用例通过；server-side dry-run=not_run，不证明外部 Secret 或真实集群部署 |
| MySQL runner + bootstrap 只读审计（沙箱） | `status=passed` | 迁移、ensure/noop/fail-closed/rotate、账号/租户/角色/岗位/BCrypt 形态和唯一索引一致；仅证明该沙箱库状态，`independent_review=false`，不证明生产目标库、事务、并发或其余 7 套方言 |
| V116/V117 约束语义（MySQL 沙箱） | 5/5 scenarios passed | 并发重复拒绝为 MySQL 1062、软删除语义、100,000 行合成 DDL；V116 2.401s、V117 0.599s；仅隔离沙箱单次观测，不证明生产规模 MDL/复制、非 MySQL 或 BPM 回滚 |
| 迁移契约 | `status=passed` | tracked=28、scanned=124、findings=0、exit=0；仅锁迁移字节，实库证据另见当前 MySQL runner 证据 |
| 租户隔离 | `status=passed`（默认阈值） | 仍有 360 条 P1（T5=258、T4=102），不能写成零风险 |
| 安全基线扫描 | `status=failed` | 463 文件、248 发现、P0=32、P1=216、exit=1 |
| 生产就绪标记 | 未通过 | `production_ready=false` |
| Gate3 | 未通过 | `gate3_passed=false` |

### 3.3 安全扫描 P0

当前 P0 由两类组成：

- 20 条生产 Flyway 种子账号：`R7`
- 12 条 SQL 明文客户端凭据：`R10`；其中 8 套安装脚本的 96 条已移除

此前 3 条 `existingSecret` 空值命中已确认是扫描器假阳性并修复，R5 当前为 0，不得继续计入 P0。

具体位置、规则和修复边界见 `13-安全基线扫描证据.md`。V115-V117 已在 MySQL 8.4.9 clean 沙箱执行并通过验证与重跑幂等；V116/V117 的并发重复拒绝、软删除语义和 100,000 行合成 DDL 成本已通过隔离沙箱证据，但该证据不覆盖生产规模、MDL/复制影响、非 MySQL 方言或 BPM 回滚，存量生产库全链尚未闭环。非 MySQL 方言仍没有等价的活跃账号唯一约束或 CRM 回款幂等迁移。当前安全扫描为 463 文件、248 发现、P0=32、P1=216，安全门禁仍为失败。

管理员 SQL 模板资产覆盖 8 套方言并按 `tenant_id=1 + username`、`role_id=1`、`post_id=4` 做静态契约校验；生产 K8s 入口是 `BootstrapAdminRunner`。当前 MySQL 8.4.9 clean 沙箱 runner 和只读审计确认了 ensure 创建、相同密码 noop、不同密码 fail-closed 且哈希不变、rotate 成功、旧密码失效、新密码 noop，以及账号唯一性、租户、角色、岗位、BCrypt 形态和 `uk_system_users_tenant_active_username` 索引一致；`independent_review=false`。该 bootstrap 证据只覆盖 MySQL 沙箱库，不证明生产目标库、事务边界、并发或其余 7 套方言。V116/V117 的并发/软删除/合成 DDL 证据同样仅限隔离沙箱，不证明生产规模迁移、非 MySQL 并发唯一性、BPM 回滚或生产可用。其余 7 套尚未在目标实库执行。

## 4. 文档索引

| 文件 | 内容 |
|---|---|
| `01-平台现状核验.md` | 代码、模块、迁移、测试、部署现状 |
| `02-企业级能力差距分析.md` | 与企业级目标的差距和优先级 |
| `03-目标架构与升级方案.md` | 目标架构、核心机制、模块边界 |
| `04-业务模块升级路线.md` | 八大域和新域的升级路线 |
| `05-生产就绪门禁清单.md` | 上线门禁、P0、退出条件 |
| `06-数据迁移与发布方案.md` | 数据清理、迁移、双写、回滚 |
| `07-模拟数据与验收方案.md` | 无真实数据时的模拟数据和验收 |
| `08-测试与证据.md` | 测试金字塔、证据分类、缺口 |
| `09-实施排期与资源估算.md` | 阶段、人员、周期、依赖 |
| `10-最终Review与交付审计.md` | 最终审计口径和当前结论 |
| `11-外部对标与资料索引.md` | GitHub 对标、来源和限制 |
| `12-风险登记册.md` | 风险、触发条件、缓解措施 |
| `13-安全基线扫描证据.md` | 当前安全扫描唯一有效口径 |
| `14-实施任务分解与执行顺序.md` | 可直接执行的阶段任务清单 |

## 5. 当前结论

**可以进入生产化改造，不可以直接进入生产部署。**

下一步必须先完成：

1. 在 MySQL 沙箱证据基础上，进入预发/生产等量库验证 V116/V117 的 MDL、锁等待、复制延迟、磁盘空间、回滚和真实业务数据，并修复存量库的 bootstrap 顺序闭环。
2. 为非 MySQL 方言补齐活跃账号并发唯一约束和存量 MFA schema 升级路径。
3. 生产迁移中的客户端 Secret 轮换和外部 Secret 的实际注入、对象存在性、键名、权限和挂载验证。
4. 真实 TLS 握手、证书链、主机名验证，以及 K8s 服务端 dry-run。
5. 库存、质检、工单、回款等核心业务一致性问题修复。
6. 备份恢复、灾备、监控告警和 SLO 证据。
