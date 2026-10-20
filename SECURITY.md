# 安全策略

## 报告漏洞

请**不要**开公开 issue。使用仓库 Security 标签页的「Report a vulnerability」（GitHub Private vulnerability reporting）私下提交，附影响范围、复现步骤和版本信息；该入口不可用时，联系你的交付对接人。收到后会评估影响，并在修复版本的变更记录中说明。

## 支持范围

安全修复只进入最新主线版本，不为历史快照单独出补丁。

## 部署方须知

- 首个管理员由 `bootstrap-admin`（单机 compose）或 `init_bootstrap_admin.py` / `BootstrapAdminRunner`（K8s PreSync Job）创建，口令必须 **12–16 位**；`ensure` 模式不会覆盖已存在账号的口令。
- V115 迁移会软禁用全部公开种子账号（`admin` / `admin123` 等），**不要**重新启用或导入演示种子以获取登录能力。
- 生产必须替换 `.env` 里的全部占位口令。示例 compose 为内网试点默认关闭了 MySQL 证书校验（`verifyServerCertificate=false`），对外暴露前须换成真实证书并开启校验。
- Redis / PostgreSQL / MinIO / Nacos 均设口令，不要裸奔在公网；反向代理终止 TLS，并限制 `/actuator/**` 仅内网访问。
- 不要提交 `.env`、证书、私钥或真实客户数据；`.env.example` 只放占位符。
- 第三方依赖许可证门禁尚未通过，对外分发制品前需先完成法务处置，详见 [license-disposition-proposal-20261003.md](docs/enterprise-platform/license-disposition-proposal-20261003.md)。
