# 真实干净 Windows 机 `.bat` 全安装证据

- 生成时间：2026-10-09（Asia/Singapore）
- 责任人：项目所有者（GitHub `cgs123456`）
- 保留期：随 `master` 版本保留；删除、重写或公开披露需所有者再次裁决
- 结论性质：**干净 Windows Server 2025 Evaluation VM 上的真实全安装与启动验证**
- 相关交接：`docs/enterprise-platform/handoff/uncommitted-review-groups-20260928.md` §12.164

## 1. 干净机初始状态

- 主机名 `ZC-CLEAN`，OS `Microsoft Windows Server 2025 Datacenter Evaluation`，版本 `10.0.26100.0`。
- 初始状态：无 Java、无 Maven、无 Node、无 pnpm；联网正常（`example.com` HTTP 200）。
- 原始记录：`vm-inventory.out.txt`（Java/Maven/Node/Pnpm 全空）、`vm-precheck.out.txt`（安装后的版本）。

工具链版本（由 zip 安装到 `C:\tools`）：

| 工具 | 版本 |
|---|---|
| JDK | Eclipse Temurin `21.0.12.1+1` |
| Maven | `3.9.11` |
| Node.js | `22.22.2` |
| pnpm | `11.25.0` |

## 2. 后端 `start-backend.bat` 全安装

- 在空 Maven 本地仓库下执行 `C:\zhicloud-src\start-backend.bat`。
- 第一次在 2 GB 动态内存 VM 上到 CRM 打包阶段失败：`maven-jar-plugin` 报 `Java heap space`，原始错误见 `build-fail.txt`。这是 VM 资源限制，不是源码/依赖错误。
- 处置：将 Maven 堆显式设为 `-Xmx2048m`，记录见 `restart-backend.out.txt`；VM 动态内存上限为 4 GB。
- 重跑同一 `.bat`：`BUILD SUCCESS`，`Total time: 07:41 min`，生成 `zhicloud-server.jar` 并启动。
- 启动证据（`startup-lines.txt`）：
  - `Tomcat started on port 48080 (http) with context path '/'`
  - `Tomcat started on port 48090 (http) with context path '/'`
  - `Started ZhiCloudServerApplication in 215.8 seconds (process running for 217.754)`
  - `http://127.0.0.1:48080/` → `HTTP 200`（`server2-status.txt`）

## 3. 干净 VM 上的登录与业务读路径

原始 JSON：`vm-e2e-results.json`、`vm-reconcile-results.json`。

| 检查 | 结果 |
|---|---|
| 登录 | `code=0`，`userId=225` |
| 权限信息 | `code=0`，`user=zcadmin`，`roles=1`，`permissions=1232` |
| 刷新令牌 | `code=0`，`userId=225` |
| 库存对账 | `code=0`，`consistent=true`，`mismatchCount=0` |

说明：VM 通过宿主端口转发连接隔离 MySQL 8.4.11 / Redis / RabbitMQ；令牌仅记录长度，未落盘明文。

## 4. 前端 `start-frontend.bat` 冷安装

- 在空 `node_modules`、空 pnpm store 下执行 `C:\zhicloud-src\start-frontend.bat`。
- `pnpm install` 下载 1147 个包，`Done in 1m 8.6s using pnpm v11.25.0`。
- `Vite v8.1.4` 冷启动 `ready in 17985 ms`，监听 `http://localhost:80/`。
- 首页 `http://127.0.0.1/` → `HTTP 200`，响应长度 150471。
- 原始记录：`frontend-wait-result.txt`。

## 5. 边界（不得外推）

- 这是 **Windows Server 2025 Datacenter Evaluation VM**，不是 Windows 10/11 桌面物理机；桌面版差异未验证。
- 2 GB 内存下默认 Maven 堆不足，必须先设置 `MAVEN_OPTS=-Xmx2048m`；这是本次唯一环境前置，已保留失败与成功两份记录。
- 数据层仍是宿主上的隔离 MySQL/Redis/RabbitMQ，不是生产部署；不代表生产规模、TLS、K8s、HA、备份或独立复核。
- 不改变 `production_ready=false`、`gate3_passed=false`、`independent_review=false`、`Go/No-Go=No-Go`。

## 6. 工件

见同目录 `SHA256SUMS.txt`。
