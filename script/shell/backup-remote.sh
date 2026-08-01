#!/bin/bash
##==================================================================
## yudao 异地备份脚本
## 功能：
##   1. mysqldump 导出 MySQL（gzip 压缩）
##   2. pg_dump 导出 PostgreSQL（gzip 压缩）
##   3. redis-cli --rdb 导出 Redis RDB
##   4. 上传到 rsync over SSH 或 S3 兼容存储
##   5. 本地保留 7 天，远程保留 30 天
##   6. 日志记录到 logs/backup/backup-yyyy-MM-dd.log
##
## 用法：
##   ./backup-remote.sh                # 执行备份
##   ./backup-remote.sh --dry-run      # 仅打印不执行
##   ./backup-remote.sh --config=/path # 指定配置文件
##
## 退出码：
##   0 - 成功
##   1 - 失败
##==================================================================

set -euo pipefail

# ===== 默认配置 =====
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/backup-config.sh"
DRY_RUN=false
START_TIME=$(date +%s)
TODAY=$(date +%Y-%m-%d)
BACKUP_DATETIME=$(date +%Y%m%d_%H%M%S)

# ===== 解析命令行参数 =====
for arg in "$@"; do
    case $arg in
        --dry-run)
            DRY_RUN=true
            ;;
        --config=*)
            CONFIG_FILE="${arg#*=}"
            ;;
        -h|--help)
            echo "用法: $0 [--dry-run] [--config=/path/to/backup-config.sh]"
            echo "  --dry-run       仅打印操作，不实际执行"
            echo "  --config=PATH   指定配置文件路径"
            exit 0
            ;;
        *)
            echo "[error] 未知参数: $arg"
            echo "用法: $0 [--dry-run] [--config=/path/to/backup-config.sh]"
            exit 1
            ;;
    esac
done

# ===== 加载配置 =====
if [ ! -f "${CONFIG_FILE}" ]; then
    echo "[error] 配置文件不存在: ${CONFIG_FILE}"
    exit 1
fi
# shellcheck disable=SC1090
source "${CONFIG_FILE}"

# ===== 准备目录 =====
LOCAL_BACKUP_DIR="${LOCAL_BACKUP_DIR:-/data/backup/yudao}"
BACKUP_DAY_DIR="${LOCAL_BACKUP_DIR}/${TODAY}"
LOG_DIR="${LOG_DIR:-${SCRIPT_DIR}/../../logs/backup}"
LOG_FILE="${LOG_DIR}/backup-${TODAY}.log"

mkdir -p "${BACKUP_DAY_DIR}" "${LOG_DIR}"

# ===== 日志函数 =====
log() {
    local LEVEL=$1
    shift
    local MSG="$*"
    local TS
    TS=$(date '+%Y-%m-%d %H:%M:%S')
    echo "[${TS}] [${LEVEL}] ${MSG}" | tee -a "${LOG_FILE}"
}

log_info()  { log "INFO"  "$@"; }
log_warn()  { log "WARN"  "$@"; }
log_error() { log "ERROR" "$@"; }

# ===== 通知函数 =====
send_notify() {
    local TITLE=$1
    local CONTENT=$2
    if [ -z "${NOTIFY_WEBHOOK:-}" ]; then
        return 0
    fi
    local PAYLOAD
    PAYLOAD=$(cat <<EOF
{
  "msgtype": "markdown",
  "markdown": {
    "title": "${TITLE}",
    "text": "## ${TITLE}\n\n${CONTENT}\n\n> 时间: $(date '+%Y-%m-%d %H:%M:%S')"
  }
}
EOF
)
    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] 发送通知: ${TITLE}"
        return 0
    fi
    curl -s -o /dev/null -w "%{http_code}" \
        -H "Content-Type: application/json" \
        -d "${PAYLOAD}" \
        "${NOTIFY_WEBHOOK}" 2>/dev/null || log_warn "通知发送失败"
}

# ===== 执行命令封装（支持 dry-run）====
run_cmd() {
    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] $*"
    else
        log_info "[exec] $*"
        eval "$@" 2>&1 | tee -a "${LOG_FILE}" || return 1
    fi
}

# ===== 备份结果统计 =====
BACKUP_RESULT_STATUS="SUCCESS"
BACKUP_RESULT_FILES=()
BACKUP_RESULT_DETAIL=""

# ===== 1. MySQL 备份 =====
backup_mysql() {
    local DB
    for DB in ${MYSQL_DATABASES}; do
        local FILE="${BACKUP_DAY_DIR}/mysql_${DB}_${BACKUP_DATETIME}.sql.gz"
        log_info "开始备份 MySQL 数据库: ${DB} -> ${FILE}"
        if [ "${DRY_RUN}" = true ]; then
            log_info "[dry-run] mysqldump -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p*** ${DB} | gzip > ${FILE}"
            BACKUP_RESULT_FILES+=("${FILE}")
            continue
        fi
        if mysqldump -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" \
                -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" \
                ${MYSQL_EXTRA_OPTS:-} \
                "${DB}" 2>>"${LOG_FILE}" | gzip > "${FILE}"; then
            local SIZE
            SIZE=$(du -h "${FILE}" | cut -f1)
            log_info "MySQL 备份成功: ${FILE} (${SIZE})"
            BACKUP_RESULT_FILES+=("${FILE}")
        else
            log_error "MySQL 备份失败: ${DB}"
            BACKUP_RESULT_STATUS="FAILED"
            return 1
        fi
    done
}

# ===== 2. PostgreSQL 备份 =====
backup_postgres() {
    local DB
    for DB in ${PG_DATABASES}; do
        local FILE="${BACKUP_DAY_DIR}/postgres_${DB}_${BACKUP_DATETIME}.sql.gz"
        log_info "开始备份 PostgreSQL 数据库: ${DB} -> ${FILE}"
        if [ "${DRY_RUN}" = true ]; then
            log_info "[dry-run] pg_dump -h${PG_HOST} -p${PG_PORT} -U${PG_USER} ${DB} | gzip > ${FILE}"
            BACKUP_RESULT_FILES+=("${FILE}")
            continue
        fi
        if PGPASSWORD="${PG_PASSWORD}" pg_dump -h"${PG_HOST}" -p"${PG_PORT}" \
                -U"${PG_USER}" -d "${DB}" \
                --no-owner --no-privileges 2>>"${LOG_FILE}" | gzip > "${FILE}"; then
            local SIZE
            SIZE=$(du -h "${FILE}" | cut -f1)
            log_info "PostgreSQL 备份成功: ${FILE} (${SIZE})"
            BACKUP_RESULT_FILES+=("${FILE}")
        else
            log_error "PostgreSQL 备份失败: ${DB}"
            BACKUP_RESULT_STATUS="FAILED"
            return 1
        fi
    done
}

# ===== 3. Redis 备份 =====
backup_redis() {
    local FILE="${BACKUP_DAY_DIR}/redis_${BACKUP_DATETIME}.rdb"
    log_info "开始备份 Redis RDB -> ${FILE}"
    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] redis-cli -h${REDIS_HOST} -p${REDIS_PORT} -a*** --rdb ${FILE}"
        BACKUP_RESULT_FILES+=("${FILE}")
        return 0
    fi
    # 先触发 BGSAVE 再导出 RDB，避免不一致
    redis-cli -h"${REDIS_HOST}" -p"${REDIS_PORT}" -a"${REDIS_PASSWORD}" \
        --no-auth-warning BGSAVE 2>>"${LOG_FILE}" || log_warn "BGSAVE 触发失败，继续导出"
    if redis-cli -h"${REDIS_HOST}" -p"${REDIS_PORT}" -a"${REDIS_PASSWORD}" \
        --no-auth-warning --rdb "${FILE}" 2>>"${LOG_FILE}"; then
        local SIZE
        SIZE=$(du -h "${FILE}" | cut -f1)
        log_info "Redis 备份成功: ${FILE} (${SIZE})"
        BACKUP_RESULT_FILES+=("${FILE}")
    else
        log_error "Redis 备份失败"
        BACKUP_RESULT_STATUS="FAILED"
        return 1
    fi
}

# ===== 4. 上传到远程（rsync over SSH）====
upload_rsync() {
    if [ "${REMOTE_RSYNC_ENABLED:-false}" != "true" ]; then
        log_info "rsync 远程上传未启用，跳过"
        return 0
    fi
    local REMOTE="${REMOTE_RSYNC_USER}@${REMOTE_RSYNC_HOST}"
    local REMOTE_FULL_PATH="${REMOTE_RSYNC_PATH}/${TODAY}"
    log_info "上传到远程（rsync over SSH）: ${REMOTE}:${REMOTE_FULL_PATH}"

    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] rsync -avz --progress -e 'ssh -p ${REMOTE_RSYNC_PORT}' ${BACKUP_DAY_DIR}/ ${REMOTE}:${REMOTE_FULL_PATH}/"
        return 0
    fi

    # 创建远程目录
    ssh -p "${REMOTE_RSYNC_PORT}" "${REMOTE}" "mkdir -p ${REMOTE_FULL_PATH}" 2>>"${LOG_FILE}"
    # 上传
    if rsync -avz --progress -e "ssh -p ${REMOTE_RSYNC_PORT}" \
            "${BACKUP_DAY_DIR}/" "${REMOTE}:${REMOTE_FULL_PATH}/" 2>>"${LOG_FILE}"; then
        log_info "rsync 上传成功"
    else
        log_error "rsync 上传失败"
        BACKUP_RESULT_STATUS="FAILED"
        return 1
    fi

    # 清理远程过期备份（保留 N 天）
    local REMOTE_ROOT="${REMOTE_RSYNC_PATH}"
    log_info "清理远程过期备份（保留 ${REMOTE_RETENTION_DAYS} 天）"
    ssh -p "${REMOTE_RSYNC_PORT}" "${REMOTE}" \
        "find ${REMOTE_ROOT} -maxdepth 1 -type d -name '20[0-9][0-9]-[0-9][0-9]-[0-9][0-9]' -mtime +${REMOTE_RETENTION_DAYS} -exec rm -rf {} \;" 2>>"${LOG_FILE}" || \
        log_warn "远程清理失败（不影响主流程）"
}

# ===== 5. 上传到 S3 =====
upload_s3() {
    if [ "${S3_ENABLED:-false}" != "true" ]; then
        log_info "S3 上传未启用，跳过"
        return 0
    fi
    local S3_TARGET="s3://${S3_BUCKET}/${S3_PREFIX}${TODAY}/"
    log_info "上传到 S3: ${S3_TARGET}"

    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] aws s3 cp ${BACKUP_DAY_DIR}/ ${S3_TARGET} --endpoint-url ${S3_ENDPOINT} --recursive"
        return 0
    fi

    # 设置 AWS 凭证（如提供）
    if [ -n "${AWS_ACCESS_KEY_ID:-}" ] && [ -n "${AWS_SECRET_ACCESS_KEY:-}" ]; then
        export AWS_ACCESS_KEY_ID AWS_SECRET_ACCESS_KEY
    fi

    if aws s3 cp "${BACKUP_DAY_DIR}/" "${S3_TARGET}" \
            --endpoint-url "${S3_ENDPOINT}" --region "${S3_REGION:-us-east-1}" \
            --recursive 2>>"${LOG_FILE}"; then
        log_info "S3 上传成功"
    else
        log_error "S3 上传失败"
        BACKUP_RESULT_STATUS="FAILED"
        return 1
    fi

    # 清理远程过期 S3 对象
    local CUTOFF_DATE
    CUTOFF_DATE=$(date -d "-${S3_RETENTION_DAYS} days" +%Y-%m-%d 2>/dev/null || date -v-${S3_RETENTION_DAYS}d +%Y-%m-%d)
    log_info "清理 S3 过期对象（早于 ${CUTOFF_DATE}）"
    aws s3 ls "s3://${S3_BUCKET}/${S3_PREFIX}" \
        --endpoint-url "${S3_ENDPOINT}" --region "${S3_REGION:-us-east-1}" 2>>"${LOG_FILE}" \
        | awk '{print $2}' \
        | while read -r PREFIX; do
            PREFIX_DATE=$(echo "${PREFIX}" | tr -d '/' | grep -E '^[0-9]{4}-[0-9]{2}-[0-9]{2}$' || true)
            if [ -n "${PREFIX_DATE}" ] && [ "${PREFIX_DATE}" \< "${CUTOFF_DATE}" ]; then
                log_info "删除 S3 过期对象: ${PREFIX}"
                aws s3 rm "s3://${S3_BUCKET}/${S3_PREFIX}${PREFIX}" \
                    --endpoint-url "${S3_ENDPOINT}" --region "${S3_REGION:-us-east-1}" \
                    --recursive 2>>"${LOG_FILE}" || log_warn "删除 S3 对象失败: ${PREFIX}"
            fi
        done
}

# ===== 6. 本地保留清理 =====
cleanup_local() {
    local RETENTION=${LOCAL_RETENTION_DAYS:-7}
    log_info "清理本地过期备份（保留 ${RETENTION} 天）"
    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] find ${LOCAL_BACKUP_DIR} -maxdepth 1 -type d -mtime +${RETENTION} -exec rm -rf {} \;"
        return 0
    fi
    find "${LOCAL_BACKUP_DIR}" -maxdepth 1 -type d -name '20[0-9][0-9]-[0-9][0-9]-[0-9][0-9]' \
        -mtime +"${RETENTION}" -exec rm -rf {} \; 2>>"${LOG_FILE}" || \
        log_warn "本地清理失败（不影响主流程）"

    # 清理过期日志
    find "${LOG_DIR}" -type f -name "backup-*.log" \
        -mtime +"${LOG_RETENTION_DAYS:-30}" -delete 2>>"${LOG_FILE}" || true
}

# ===== 生成校验文件 =====
generate_checksum() {
    local CHECKSUM_FILE="${BACKUP_DAY_DIR}/checksum.sha256"
    log_info "生成校验文件: ${CHECKSUM_FILE}"
    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] sha256sum ${BACKUP_DAY_DIR}/* > ${CHECKSUM_FILE}"
        return 0
    fi
    (cd "${BACKUP_DAY_DIR}" && sha256sum *.sql.gz *.rdb > "${CHECKSUM_FILE}" 2>/dev/null) \
        || log_warn "校验文件生成失败"
}

# ===== 主流程 =====
main() {
    log_info "================ 备份开始 ================"
    log_info "模式: $([ "${DRY_RUN}" = true ] && echo 'DRY-RUN' || echo 'EXECUTE')"
    log_info "本地目录: ${BACKUP_DAY_DIR}"
    log_info "MySQL: ${MYSQL_DATABASES}"
    log_info "PostgreSQL: ${PG_DATABASES}"
    log_info "Redis: ${REDIS_HOST}:${REDIS_PORT}"
    log_info "rsync 远程: ${REMOTE_RSYNC_ENABLED}"
    log_info "S3: ${S3_ENABLED}"

    # 执行备份
    backup_mysql       || log_error "MySQL 备份环节异常"
    backup_postgres    || log_error "PostgreSQL 备份环节异常"
    backup_redis       || log_error "Redis 备份环节异常"

    # 生成校验文件
    generate_checksum

    # 上传远程
    upload_rsync || log_error "rsync 上传环节异常"
    upload_s3    || log_error "S3 上传环节异常"

    # 清理过期备份
    cleanup_local

    # 统计
    local END_TIME=$(date +%s)
    local DURATION=$((END_TIME - START_TIME))
    local FILE_COUNT=${#BACKUP_RESULT_FILES[@]}
    local TOTAL_SIZE="0"
    if [ "${DRY_RUN}" = false ] && [ ${FILE_COUNT} -gt 0 ]; then
        TOTAL_SIZE=$(du -sh "${BACKUP_DAY_DIR}" 2>/dev/null | cut -f1)
    fi

    log_info "================ 备份结束 ================"
    log_info "状态: ${BACKUP_RESULT_STATUS}"
    log_info "文件数: ${FILE_COUNT}"
    log_info "总大小: ${TOTAL_SIZE}"
    log_info "耗时: ${DURATION}s"

    # 构建详情
    BACKUP_RESULT_DETAIL=$(cat <<EOF
- **状态**: ${BACKUP_RESULT_STATUS}
- **备份目录**: ${BACKUP_DAY_DIR}
- **文件数**: ${FILE_COUNT}
- **总大小**: ${TOTAL_SIZE}
- **耗时**: ${DURATION}s
- **MySQL**: ${MYSQL_DATABASES}
- **PostgreSQL**: ${PG_DATABASES}
- **Redis**: ${REDIS_HOST}:${REDIS_PORT}
- **远程(rsync)**: ${REMOTE_RSYNC_ENABLED}
- **S3**: ${S3_ENABLED}
EOF
)
    echo "${BACKUP_RESULT_DETAIL}" >> "${LOG_FILE}"

    # 通知
    if [ "${BACKUP_RESULT_STATUS}" = "SUCCESS" ]; then
        send_notify "✅ yudao 备份成功" "${BACKUP_RESULT_DETAIL}"
    else
        send_notify "❌ yudao 备份失败" "${BACKUP_RESULT_DETAIL}"
    fi

    # 退出码
    if [ "${BACKUP_RESULT_STATUS}" = "SUCCESS" ]; then
        exit 0
    else
        exit 1
    fi
}

main "$@"
