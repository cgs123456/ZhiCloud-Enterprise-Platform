# SkillCatalogValidator 启动期租户上下文修复证据

- 生成时间：2026-10-09（Asia/Singapore）
- 结论性质：**单元测试 + 隔离环境运行时日志**；不是生产等价验证
- 相关交接：`docs/enterprise-platform/handoff/uncommitted-review-groups-20260928.md` §12.163.7
- 相关代码：`zhicloud-module-ai-multiagent/src/main/java/cn/zhicloud/module/aimultiagent/service/skill/SkillCatalogValidator.java`
- 相关测试：`zhicloud-module-ai-multiagent/src/test/java/cn/zhicloud/module/aimultiagent/service/skill/SkillCatalogValidatorTest.java`

## 1. 缺陷

`SkillCatalogValidator` 是 `ApplicationRunner`，启动时比对 Worker 声明的工具与 `aimultiagent_skill`
技能目录的绑定关系。它在**启动线程**执行，此时 `TenantContextHolder` 为空；而
`aimultiagent_skill` / `aimultiagent_skill_group` 是租户表（`BaseDO` + 租户拦截器），
查询被拦截器抛 `NullPointerException: TenantContextHolder 不存在租户编号`，
又被方法内的 `catch (Exception e) { log.warn("校验异常，跳过", e); }` 吞掉。

结果：**该校验从未真正生效**，每次都走"异常 → 跳过"分支。这与本项目已登记的
「`@ElectronicSignature` 切面实现完整但使用次数为 0，运行时管控点根本不存在」同型
（见 `docs/enterprise-platform/00-README.md`）：实现正确 ≠ 已经接线。

## 2. 目录归属核实（决定修法）

`V87__aimultiagent_skill.sql` 的种子行 `tenant_id` 全部为 **0**，实库复核一致：

```
mysql> SELECT tenant_id, COUNT(*) FROM aimultiagent_skill GROUP BY tenant_id;
0	8
mysql> SELECT tenant_id, COUNT(*) FROM aimultiagent_skill_group GROUP BY tenant_id;
0	6
```

即技能目录是**平台全局（租户 0）目录**，不按业务租户切分。

## 3. 修法

在 `run()` 内用 `TenantUtils.execute(0L, this::validateCatalog)` 显式切到租户 0 执行校验，
`catch` 仍保留（数据库尚未迁移等场景不阻断启动）。这与 `ErpAccountingEventServiceImpl`
读取平台科目（`TenantUtils.execute(0L, () -> glAccountMapper.selectByCode(...))`）是同一写法。
未改动任何租户拦截器、未放宽任何 `@TenantIgnore`。

## 4. 单元测试

```
mvn -B -pl zhicloud-module-ai-multiagent -am test -Dtest=SkillCatalogValidatorTest \
    -Dsurefire.failIfNoSpecifiedTests=false
→ Tests run: 3, Failures: 0, Errors: 0, Skipped: 0 … BUILD SUCCESS
```

断言：

1. `getSkillByToolName` 实际被调用，且调用时 `TenantContextHolder.getTenantId() == 0`（证明校验真跑，而非被 catch 吞掉）；
2. 执行后租户上下文被还原（原为 null 则仍为 null；原为 1 则仍为 1）；
3. 目录缺失的工具仍会被查询并计数，不抛异常。

## 5. 运行时日志对照（隔离容器，MySQL 8.4.11）

修复前（同一容器前两次启动）：

```
2026-10-09 07:24:48.334 [main] [] [] WARN  c.z.m.a.service.skill.SkillCatalogValidator:67 - [SkillCatalogValidator][技能目录校验异常，跳过]
2026-10-09 07:47:13.011 [main] [] [] WARN  c.z.m.a.service.skill.SkillCatalogValidator:67 - [SkillCatalogValidator][技能目录校验异常，跳过]
```

修复后（重建 jar + `docker restart zc-live-server`）：

```
2026-10-09 08:05:41.655 [main] [] [] INFO  c.z.m.a.service.skill.SkillCatalogValidator:84 - [SkillCatalogValidator][技能目录校验完成，Worker 数=4，缺失目录的工具数=0]
```

同一启动窗口内 `不存在租户编号` 命中数 = 0。

## 6. 回归确认（同一容器）

| 步骤 | 结果 |
|---|---|
| `POST /admin-api/system/auth/login`（tenant-id:1，zcadmin） | `code=0`，`userId=225` |
| `GET /admin-api/system/auth/get-permission-info` | `code=0`，`user=zcadmin`，`roles=1`，`permissions=1232` |
| `POST /admin-api/system/auth/refresh-token` | `code=0`，`userId=225` |

即 V151 登录修复未因本次改动回归。

## 7. 边界（不得外推）

- 校验结果「Worker 数=4，缺失目录的工具数=0」是**该隔离库**的读数，不代表其他租户/其他部署的目录完整。
- 本次只修「校验根本没执行」；校验本身的覆盖面（是否应逐租户校验、是否应纳入 CI）未变。
- 仍不改 `production_ready=false`、`gate3_passed=false`、`independent_review=false`、`Go/No-Go=No-Go`。
