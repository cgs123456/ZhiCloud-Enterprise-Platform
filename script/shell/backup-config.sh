#!/bin/bash
##==================================================================
## yudao 备份配置文件
## 使用方式：source ./backup-config.sh
## 安全说明：
##   - 密码类变量使用 ${VAR:?required} 形式，未设置时立即报错退出
##   - 实际部署时建议通过环境变量注入，而非硬编码
##==================================================================

# ===== MySQL 配置 =====
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
MYSQL_DATABASES="zhicloud_platform"
MYSQL_EXTRA_OPTS="--single-transaction --quick --routines --triggers --events"

# ===== PostgreSQL 配置 =====
PG_HOST=127.0.0.1
PG_PORT=5432
PG_USER=yudao
PG_PASSWORD="${PG_PASSWORD:?PG_PASSWORD is required}"
PG_DATABASES="yudao_rag"

# ===== Redis 配置 =====
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
REDIS_PASSWORD="${REDIS_PASSWORD:?REDIS_PASSWORD is required}"

# ===== 本地备份目录与保留策略 =====
LOCAL_BACKUP_DIR=/data/backup/yudao
LOCAL_RETENTION_DAYS=7

# ===== 远程存储（二选一）====

# ----- 模式 1：rsync over SSH -----
REMOTE_RSYNC_ENABLED=false
REMOTE_RSYNC_HOST=backup.example.com
REMOTE_RSYNC_USER=backup
REMOTE_RSYNC_PATH=/data/backup/yudao
REMOTE_RSYNC_PORT=22
REMOTE_RETENTION_DAYS=30

# ----- 模式 2：S3 兼容存储 -----
S3_ENABLED=false
S3_BUCKET=yudao-backup
S3_ENDPOINT=https://s3.amazonaws.com
S3_REGION=us-east-1
S3_PREFIX=backup/
S3_RETENTION_DAYS=30
# AWS CLI 访问密钥（建议通过 IAM Role / 环境变量注入）
AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-}"
AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-}"

# ===== 日志目录 =====
LOG_DIR=/var/log/yudao-backup
LOG_RETENTION_DAYS=30

# ===== 通知配置 =====
# 钉钉/企业微信 webhook，留空则不发送通知
NOTIFY_WEBHOOK="${NOTIFY_WEBHOOK:-}"

# ===== 容灾演练专用配置 =====
# 临时 MySQL 容器（用于恢复演练）
DRILL_MYSQL_CONTAINER=yudao-drill-mysql
DRILL_MYSQL_PORT=3399
DRILL_MYSQL_ROOT_PASSWORD=drill_password_changeme
DRILL_MYSQL_DATABASE=zhicloud_platform

# 临时 yudao-server 配置
DRILL_SERVER_CONTAINER=yudao-drill-server
DRILL_SERVER_PORT=48180
DRILL_SERVER_ACTUATOR_PORT=48190
DRILL_SERVER_JAR=/data/yudao/yudao-server.jar
DRILL_HEALTH_TIMEOUT=180

# 健康检查与 API 测试用账号（仅用于演练环境，请使用测试账号）
DRILL_LOGIN_USERNAME=admin
DRILL_LOGIN_TENANT=1

# 容灾演练结果输出目录
DRILL_REPORT_DIR=/var/log/yudao-drill
