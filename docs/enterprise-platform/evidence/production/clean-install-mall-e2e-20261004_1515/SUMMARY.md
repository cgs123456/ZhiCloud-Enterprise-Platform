# 全新安装端到端证据包（2026-10-04，run_id 20261004_1515）

- **命令/口径**：`mvn -B -pl zhicloud-server -am clean package -DskipTests` → 开包核对
  `BOOT-INF/classes/db/migration/V132…V135` → 停自己确认过的 PID → `CREATE DATABASE zhicloud_sandbox_clean`
  → 导入 `sql/mysql/zhicloud_platform.sql` + 剥 BOM 后的 `sql/mysql/quartz.sql`（59 表、无 flyway 历史）
  → `zc-clean-boot.sh` 启服务，由**应用自带 Flyway** 建历史 → 跑 `zc_clean_mall.py`。
- **环境**：MySQL 8.4.11 容器 `zc-live-mysql`（宿主 127.0.0.1:55101）、Redis `16380`、RabbitMQ `56721`，
  全部本轮自建隔离实例；未连接任何其它项目的容器或数据目录。
- **Flyway 读数**：`Successfully baselined schema with version: 1`、`Successfully validated 142 migrations`，
  `applied_total=142 / SUM(success=1)=142`；`flyway-v132-v135.txt` 与 `flyway_schema_history.sql` 为库内导出。
- **端到端读数**：`mall-e2e-run.log` 尾部 8 条断言全 PASS，`CLEAN_INSTALL_MALL verdict=PASS`，退出码 0。
  其中负向对照（渠道为种子默认停用态）返回 `1007001001 支付渠道已经禁用`。
  断言对象是**迁移产出**的 `pay_app`/`pay_channel`（`creator='1'`），日志里 `app_id=1 / channel_id=1` 即该迁移行。
- **本包不证明**：不证明生产拓扑、并发、真实支付渠道、多租户隔离；`mall-reference-seed.sql` 是本机临时
  参照数据（运营录入的等价物），不是仓库产物，也不构成对任何真实供应商/商品的事实陈述。
  `production_ready=false`、`gate3_passed=false`、`independent_review=false` 不变。
