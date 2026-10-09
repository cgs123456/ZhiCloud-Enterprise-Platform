# 单机多副本 + Nginx TLS 生产形态 E2E 证据

- 生成时间：2026-10-09（Asia/Singapore）
- 责任人：项目所有者（GitHub `cgs123456`）
- 保留期：随 `master` 版本保留；删除、重写或公开披露需所有者再次裁决
- 结论性质：**单机多副本生产形态验证**；不是生产等价环境验证
- 相关交接：`docs/enterprise-platform/handoff/uncommitted-review-groups-20260928.md` §12.164

## 1. 拓扑

- 入口：`nginx:1.27-alpine`，监听 `https://127.0.0.1:8443`，TLSv1.2/1.3，自签 `localhost` 证书。
- 上游：`zc-live-server:48080` 与 `zc-live-server-2:48080`，Nginx `upstream` 故障转移参数 `max_fails=2 fail_timeout=5s`。
- 数据层：隔离 `zc-live-mysql`（MySQL 8.4.11，55101）、`zc-live-redis`（16380）、`zc-live-rabbit`（56721）。
- Webhook 接收端：本机 HTTP 端点 `/webhook`，接收 `POST /erp/inventory-reconcile/alert`。

配置原文见 `nginx.conf`；证书信息见 `tls-cert.txt`。私钥未复制入本证据目录。

## 2. 本轮可复验读数

本轮通过干净 Windows VM 内的同一后端源码、同一 `zhicloud_platform` 库复验了登录与对账链路，原始 JSON 见 `vm-e2e-results.json`、`vm-reconcile-results.json`。

| 检查 | 结果 |
|---|---|
| 登录 | `code=0`，`userId=225` |
| 权限信息 | `code=0`，`user=zcadmin`，`roles=1`，`permissions=1232` |
| 刷新令牌 | `code=0`，`userId=225`，新 access token 长度 32 |
| 库存对账（清理后） | `code=0`，`consistent=true`，`mismatchCount=0` |

说明：`refresh-token` 的令牌通过 query 参数传递，不是请求体；这是当前接口契约。

## 3. 差异与告警链路

在隔离库注入 `erp_stock` product `990001` / warehouse `990001` 数量 7，并写入两条 `mdm_cross_ref` 后：

- `GET /erp/inventory-reconcile/run`：`consistent=false`，`mismatchCount=1`。
- `POST /erp/inventory-reconcile/alert`：人工触发后 `alertCode=0`、`pushed=true`。
- Webhook 实际收到 payload（`webhook.log`，SHA-256 `4D826EF0A915F5ABDA2436B3EF437074774E6B867BF5DCACE6938850E5654237`），其中 `source=inventory-reconcile`、`consistent=false`、`mismatches=1`。
- 测试差异已清理；复跑恢复 `consistent=true`、`mismatchCount=0`。

## 4. 故障转移观测

上一轮同拓扑下停止 `zc-live-server` 后，经 Nginx TLS 入口继续取得 `loginCode=0` 与 `runCode=0`，说明 Nginx 能把请求切到第二个副本；`nginx.conf` 的上游和超时参数可复核。该观测是单机双副本切换，不是多节点、真实网络分区或 HA 验证。

## 5. 边界（不得外推）

- 单机 Docker 多副本 + 自签 TLS ≠ 多节点 K8s、真实 CA、生产网络、生产规模或真实故障注入。
- 本轮可复验的登录/对账读数是 VM 直连 48080；Nginx TLS 多副本读数是上一轮同拓扑观测，相关配置与 webhook 原始载荷已保留。
- 不改变 `production_ready=false`、`gate3_passed=false`、`independent_review=false`、`Go/No-Go=No-Go`。
- 生产等价环境验证仍需外部预发/生产等量基础设施，保持 OPEN。

## 6. 工件

见同目录 `SHA256SUMS.txt`。
