# 本机原生 MySQL 沙箱（不使用 Docker）

> 本文件是 `scripts/production/mysql_chain_run.py` 的运行前提说明。
> 背景：本机 Docker 不可用（`com.docker.service` 拒绝启动，WSL 只有已 Stopped 的 `docker-desktop`），
> 因此 V96–V102 的「真机执行」证据改由本机 ZIP 版 MySQL 8.4.9 产生。

## 1. 环境事实（实测）

| 项 | 值 |
|---|---|
| 版本 | MySQL **8.4.9**（ZIP 解压版，非容器镜像） |
| 根目录 | `C:\Users\Administrator\mysql84-sandbox\mysql-8.4.9-winx64` |
| 客户端 | `bin\mysql.exe` |
| 服务端 | `bin\mysqld.exe`，用 `Start-Process -WindowStyle Hidden` 后台常驻，**未注册 Windows 服务** |
| 端口 | **53306**（避开 3306，防止与既有实例冲突） |
| 绑定 | `127.0.0.1`（仅回环） |
| 账号 | `root` / **空密码**（仅限本机沙箱，禁止用于任何非沙箱环境） |
| 时区 | `default-time-zone=+08:00` |
| 字符集 / 排序规则 | `utf8mb4` / `utf8mb4_0900_ai_ci` |
| sql_mode | `ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION` |
| 引擎 / 复制 | InnoDB；`log-bin` 开启，`server-id=1` |
| 连接数 / 缓冲池 | `max_connections=200`；`innodb_buffer_pool_size=512M` |
| 配置文件 | 同根目录 `my.ini` |
| 日志 | 同根目录 `mysqld.out.log` / `mysqld.err.log` |

## 2. 初始化（一次性）

1. `bin\mysqld.exe --initialize-insecure`（生成空密码 root）。
2. `Start-Process bin\mysqld.exe -ArgumentList '--defaults-file=...\my.ini' -WindowStyle Hidden`。
3. 建库：`CREATE DATABASE zhicloud_sandbox_chain DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;`

## 3. 日常执行

```powershell
python scripts\production\mysql_chain_run.py `
  --mysql-client "C:\Users\Administrator\mysql84-sandbox\mysql-8.4.9-winx64\bin\mysql.exe" `
  --host 127.0.0.1 --port 53306 --user root `
  --database zhicloud_sandbox_chain `
  --verify V90 V91 V92 V93 V94 V95 V96 V97 V98 V99 V100 V101 V102 `
  --output ..\docs\enterprise-platform\evidence\production\mysql-chain-run.json
```

只跑校验、不重建库：追加 `--no-drop --from-version V9999`。

## 4. 安全边界

- 执行器**只允许**库名以 `zhicloud_sandbox` 开头的库，其余一律拒绝执行。
- 沙箱库可随时 `DROP DATABASE` 重建；**不得**把 `--host/--port` 指向生产或预生产实例。
- 空密码 root + 127.0.0.1 绑定是沙箱前提；一旦对外暴露必须改密码并限制来源。

## 5. 已知限制

- 与官方 `mysql:8.4` 镜像存在**小版本差异**（8.4.9 vs 8.4.11），优化器与排序规则细节可能不同；
  正式上线前需在目标数据库版本上复跑同一套迁移与校验。
- 单实例、无主从、无备份与 binlog 归档策略，**不具备生产高可用语义**，只用于迁移与门禁取证。