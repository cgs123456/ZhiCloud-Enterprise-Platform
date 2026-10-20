# 贡献指南

> 本项目以「变更可验证」为前提：改动要能跑通构建、测试与门禁脚本，并同步受影响的文档。

## 开发环境

JDK 21、Maven 3.9+、Node ≥ 20.19、pnpm ≥ 8.6，以及 Docker（起 MySQL / Redis）。本地启动步骤见根 [README](README.md#从源码开发)。

## 提交前检查

```bash
mvn -B -DskipTests install -T 1C
cd zhicloud-server && mvn test && cd ..

python scripts/check_error_codes.py
python scripts/check_missing_preauthorize.py
python scripts/scan_tx.py
python scripts/check_electronic_signature.py
python scripts/production/check_migration_contract.py
```

`scripts/check_bare_throws.py` 用于统计裸抛异常，目前 82 处已超出豁免预算 81；新增代码不要带裸抛，需要时先看它的输出，别直接调大 `MAX_EXEMPTIONS`。

改前端时另跑：

```bash
cd zhicloud-ui/zhicloud-ui-admin-vue3 && pnpm ts:check && pnpm build:prod
```

## 提交信息

`type(scope): 说明`，type 取 `feat` / `fix` / `docs` / `test` / `refactor` / `chore` / `deploy`，scope 用模块名或目录（如 `crm`、`deploy`、`evidence`）。一个提交只做一件事。

## 合并请求

- 写清「改了什么、为什么、怎么验证」，附命令输出或证据文件路径。
- 涉及接口、权限、迁移时，同步更新 `docs/enterprise-platform/` 下的对应台账或证据。
- 改动安装步骤、环境变量或启动命令时，必须同时更新 `README.md` 和相关 handoff 文档——文档过期按缺陷处理。
- 新增第三方依赖需给出许可证结论，见 [SECURITY.md](SECURITY.md)。

## 不要做的事

- 不要修改已登记的历史 Flyway 迁移；修复一律新增版本（当前最高 V218）。
- 不要把 `demo-data/` 当作生产数据源或账号来源。
- 不要提交口令、证书、私钥、真实客户数据；只提交 `.env.example`。
- 不要为了过门禁而扩大豁免清单；确需豁免时在 PR 里写明理由和移除条件。
