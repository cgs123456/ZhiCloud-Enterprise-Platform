#!/bin/bash
##==================================================================
## yudao 容灾演练脚本
## 功能：
##   1. 模拟主库故障，从备份恢复到备用库（Docker 临时 MySQL 实例）
##   2. 验证恢复后的数据完整性（表数量、行数）
##   3. 启动 yudao-server 连接备用库（spring profile=drill）
##   4. 测试应用能否连接备用库
##   5. 测试关键 API 是否可用
##   6. 输出演练报告
##
## 用法：
##   ./disaster-recovery-drill.sh
##   ./disaster-recovery-drill.sh --restore-from /data/backup/yudao/2026-07-29/mysql_ruoyi-vue-pro_20260729_020001.sql.gz
##   ./disaster-recovery-drill.sh --dry-run
##   ./disaster-recovery-drill.sh --skip-server    # 跳过启动 yudao-server
##
## 退出码：
##   0 - 演练成功
##   1 - 演练失败
##==================================================================

set -euo pipefail

# ===== 默认配置 =====
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONFIG_FILE="${SCRIPT_DIR}/backup-config.sh"
DRY_RUN=false
SKIP_SERVER=false
RESTORE_FROM=""
START_TIME=$(date +%s)
DRILL_DATE=$(date +%Y%m%d_%H%M%S)
REPORT_TITLE="yudao 容灾演练报告 ${DRILL_DATE}"

# ===== 解析命令行参数 =====
for arg in "$@"; do
    case $arg in
        --dry-run)
            DRY_RUN=true
            ;;
        --skip-server)
            SKIP_SERVER=true
            ;;
        --restore-from=*)
            RESTORE_FROM="${arg#*=}"
            ;;
        --config=*)
            CONFIG_FILE="${arg#*=}"
            ;;
        -h|--help)
            echo "用法: $0 [--dry-run] [--skip-server] [--restore-from=FILE] [--config=PATH]"
            echo "  --dry-run          仅打印不执行"
            echo "  --skip-server      跳过启动 yudao-server"
            echo "  --restore-from=FILE 指定备份文件恢复"
            echo "  --config=PATH      指定配置文件路径"
            exit 0
            ;;
        *)
            echo "[error] 未知参数: $arg"
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

# ===== 准备日志目录 =====
LOCAL_BACKUP_DIR="${LOCAL_BACKUP_DIR:-/data/backup/yudao}"
DRILL_REPORT_DIR="${DRILL_REPORT_DIR:-${SCRIPT_DIR}/../../logs/drill}"
mkdir -p "${DRILL_REPORT_DIR}"
LOG_FILE="${DRILL_REPORT_DIR}/drill-${DRILL_DATE}.log"
REPORT_FILE="${DRILL_REPORT_DIR}/drill-report-${DRILL_DATE}.md"

# 容器/端口默认值
DRILL_MYSQL_CONTAINER="${DRILL_MYSQL_CONTAINER:-yudao-drill-mysql}"
DRILL_MYSQL_PORT="${DRILL_MYSQL_PORT:-3399}"
DRILL_MYSQL_ROOT_PASSWORD="${DRILL_MYSQL_ROOT_PASSWORD:-drill_password_changeme}"
DRILL_MYSQL_DATABASE="${DRILL_MYSQL_DATABASE:-ruoyi-vue-pro}"
DRILL_SERVER_CONTAINER="${DRILL_SERVER_CONTAINER:-yudao-drill-server}"
DRILL_SERVER_PORT="${DRILL_SERVER_PORT:-48180}"
DRILL_SERVER_ACTUATOR_PORT="${DRILL_SERVER_ACTUATOR_PORT:-48190}"
DRILL_SERVER_JAR="${DRILL_SERVER_JAR:-/data/yudao/yudao-server.jar}"
DRILL_HEALTH_TIMEOUT="${DRILL_HEALTH_TIMEOUT:-180}"

# 演练结果收集
declare -a STEP_RESULTS=()
declare -a METRIC_RESULTS=()
OVERALL_STATUS="PASS"

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
log_step()  { log "STEP"  "$@"; }

# ===== 步骤记录 =====
record_step() {
    local NAME=$1
    local STATUS=$2
    local DETAIL=$3
    STEP_RESULTS+=("| ${NAME} | ${STATUS} | ${DETAIL} |")
    if [ "${STATUS}" != "PASS" ] && [ "${STATUS}" != "SKIP" ]; then
        OVERALL_STATUS="FAIL"
    fi
}

record_metric() {
    local NAME=$1
    local VALUE=$2
    local EXPECTED=$3
    METRIC_RESULTS+=("| ${NAME} | ${VALUE} | ${EXPECTED} |")
}

# ===== 1. 选择最新备份文件 =====
select_backup() {
    log_step "STEP 1: 选择最新备份文件"
    if [ -n "${RESTORE_FROM}" ]; then
        if [ ! -f "${RESTORE_FROM}" ]; then
            log_error "指定备份文件不存在: ${RESTORE_FROM}"
            record_step "选择备份" "FAIL" "文件不存在: ${RESTORE_FROM}"
            return 1
        fi
        BACKUP_FILE="${RESTORE_FROM}"
        log_info "使用指定备份文件: ${BACKUP_FILE}"
        record_step "选择备份" "PASS" "使用指定: ${BACKUP_FILE}"
        return 0
    fi

    # 查找最新一天的备份目录
    local LATEST_DAY_DIR
    LATEST_DAY_DIR=$(ls -td "${LOCAL_BACKUP_DIR}"/20[0-9][0-9]-[0-9][0-9]-[0-9][0-9] 2>/dev/null | head -1)
    if [ -z "${LATEST_DAY_DIR}" ]; then
        log_error "未找到任何备份目录: ${LOCAL_BACKUP_DIR}"
        record_step "选择备份" "FAIL" "未找到备份目录"
        return 1
    fi
    log_info "最新备份目录: ${LATEST_DAY_DIR}"

    # 选择最新的 MySQL 备份文件
    BACKUP_FILE=$(ls -t "${LATEST_DAY_DIR}"/mysql_*.sql.gz 2>/dev/null | head -1)
    if [ -z "${BACKUP_FILE}" ]; then
        log_error "未找到 MySQL 备份文件"
        record_step "选择备份" "FAIL" "未找到 MySQL 备份"
        return 1
    fi
    log_info "选定备份文件: ${BACKUP_FILE}"
    local FILE_SIZE
    FILE_SIZE=$(du -h "${BACKUP_FILE}" | cut -f1)
    record_step "选择备份" "PASS" "文件=${BACKUP_FILE} 大小=${FILE_SIZE}"
    return 0
}

# ===== 2. 创建临时 MySQL 实例（Docker）====
create_drill_mysql() {
    log_step "STEP 2: 创建临时 MySQL 实例（Docker）"
    if ! command -v docker >/dev/null 2>&1; then
        log_error "docker 命令不可用"
        record_step "创建 MySQL" "FAIL" "docker 不可用"
        return 1
    fi

    # 清理可能存在的旧容器
    if docker ps -a --format '{{.Names}}' | grep -q "^${DRILL_MYSQL_CONTAINER}$"; then
        log_warn "发现旧容器 ${DRILL_MYSQL_CONTAINER}，清理中..."
        if [ "${DRY_RUN}" = false ]; then
            docker rm -f "${DRILL_MYSQL_CONTAINER}" >/dev/null 2>&1 || true
        fi
    fi

    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] docker run -d --name ${DRILL_MYSQL_CONTAINER} -p ${DRILL_MYSQL_PORT}:3306 ..."
        record_step "创建 MySQL" "PASS" "dry-run"
        return 0
    fi

    if ! docker run -d \
            --name "${DRILL_MYSQL_CONTAINER}" \
            -p "${DRILL_MYSQL_PORT}:3306" \
            -e MYSQL_ROOT_PASSWORD="${DRILL_MYSQL_ROOT_PASSWORD}" \
            -e MYSQL_DATABASE="${DRILL_MYSQL_DATABASE}" \
            -e TZ=Asia/Shanghai \
            mysql:8.0 \
            --character-set-server=utf8mb4 \
            --collation-server=utf8mb4_unicode_ci \
            --default-time-zone=+08:00 \
            >/dev/null 2>&1; then
        log_error "创建临时 MySQL 实例失败"
        record_step "创建 MySQL" "FAIL" "docker run 失败"
        return 1
    fi

    log_info "等待 MySQL 启动..."
    local WAIT=0
    while [ ${WAIT} -lt 60 ]; do
        if docker exec "${DRILL_MYSQL_CONTAINER}" \
                mysqladmin ping -uroot -p"${DRILL_MYSQL_ROOT_PASSWORD}" \
                --silent 2>/dev/null; then
            log_info "MySQL 已就绪（${WAIT}s）"
            record_step "创建 MySQL" "PASS" "容器=${DRILL_MYSQL_CONTAINER} 端口=${DRILL_MYSQL_PORT}"
            return 0
        fi
        sleep 2
        WAIT=$((WAIT + 2))
    done
    log_error "MySQL 启动超时"
    record_step "创建 MySQL" "FAIL" "启动超时"
    return 1
}

# ===== 3. 恢复备份到临时实例 =====
restore_backup() {
    log_step "STEP 3: 恢复备份到临时实例"
    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] zcat ${BACKUP_FILE} | docker exec -i ${DRILL_MYSQL_CONTAINER} mysql -uroot -p*** ${DRILL_MYSQL_DATABASE}"
        record_step "恢复备份" "PASS" "dry-run"
        return 0
    fi

    # 校验 checksum（如有）
    local BACKUP_DIR
    BACKUP_DIR=$(dirname "${BACKUP_FILE}")
    local CHECKSUM_FILE="${BACKUP_DIR}/checksum.sha256"
    if [ -f "${CHECKSUM_FILE}" ]; then
        log_info "校验备份文件完整性..."
        if (cd "${BACKUP_DIR}" && sha256sum -c "${CHECKSUM_FILE}" --quiet 2>/dev/null); then
            log_info "校验文件完整性 OK"
        else
            log_warn "校验文件完整性失败，继续恢复（可能是部分文件）"
        fi
    fi

    local RESTORE_START=$(date +%s)
    if ! zcat "${BACKUP_FILE}" | docker exec -i "${DRILL_MYSQL_CONTAINER}" \
            mysql -uroot -p"${DRILL_MYSQL_ROOT_PASSWORD}" \
            "${DRILL_MYSQL_DATABASE}" 2>>"${LOG_FILE}"; then
        log_error "恢复失败"
        record_step "恢复备份" "FAIL" "mysql 恢复失败"
        return 1
    fi
    local RESTORE_END=$(date +%s)
    local RESTORE_DURATION=$((RESTORE_END - RESTORE_START))
    log_info "恢复成功，耗时 ${RESTORE_DURATION}s"
    record_step "恢复备份" "PASS" "耗时=${RESTORE_DURATION}s"
    return 0
}

# ===== 4. 验证数据完整性 =====
verify_data() {
    log_step "STEP 4: 验证数据完整性"
    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] 跳过验证"
        record_step "数据验证" "PASS" "dry-run"
        record_metric "表数量" "N/A" ">0"
        return 0
    fi

    # 表数量
    local TABLE_COUNT
    TABLE_COUNT=$(docker exec "${DRILL_MYSQL_CONTAINER}" \
        mysql -uroot -p"${DRILL_MYSQL_ROOT_PASSWORD}" "${DRILL_MYSQL_DATABASE}" \
        -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DRILL_MYSQL_DATABASE}';" 2>/dev/null)
    log_info "表数量: ${TABLE_COUNT}"
    record_metric "表数量" "${TABLE_COUNT}" ">0"

    if [ -z "${TABLE_COUNT}" ] || [ "${TABLE_COUNT}" -le 0 ]; then
        log_error "表数量异常: ${TABLE_COUNT}"
        record_step "数据验证" "FAIL" "表数量=${TABLE_COUNT}"
        return 1
    fi

    # 关键表行数抽查
    local KEY_TABLES=("system_users" "system_role" "system_menu" "system_dept" "system_tenant")
    for TBL in "${KEY_TABLES[@]}"; do
        local ROW_COUNT
        ROW_COUNT=$(docker exec "${DRILL_MYSQL_CONTAINER}" \
            mysql -uroot -p"${DRILL_MYSQL_ROOT_PASSWORD}" "${DRILL_MYSQL_DATABASE}" \
            -N -e "SELECT COUNT(*) FROM \`${TBL}\`;" 2>/dev/null || echo "ERR")
        log_info "  - ${TBL}: ${ROW_COUNT} 行"
        record_metric "${TBL} 行数" "${ROW_COUNT}" ">=0"
    done

    record_step "数据验证" "PASS" "表数量=${TABLE_COUNT}"
    return 0
}

# ===== 5. 启动 yudao-server 连接临时实例 =====
start_drill_server() {
    if [ "${SKIP_SERVER}" = true ]; then
        log_step "STEP 5: 跳过启动 yudao-server（--skip-server）"
        record_step "启动服务" "SKIP" "用户跳过"
        return 0
    fi

    log_step "STEP 5: 启动 yudao-server 连接临时实例"

    if [ ! -f "${DRILL_SERVER_JAR}" ]; then
        log_warn "yudao-server.jar 不存在: ${DRILL_SERVER_JAR}，跳过服务测试"
        record_step "启动服务" "SKIP" "jar 不存在"
        return 0
    fi

    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] docker run -d --name ${DRILL_SERVER_CONTAINER} -p ${DRILL_SERVER_PORT}:48080 ..."
        record_step "启动服务" "PASS" "dry-run"
        return 0
    fi

    # 清理旧容器
    if docker ps -a --format '{{.Names}}' | grep -q "^${DRILL_SERVER_CONTAINER}$"; then
        docker rm -f "${DRILL_SERVER_CONTAINER}" >/dev/null 2>&1 || true
    fi

    if ! docker run -d \
            --name "${DRILL_SERVER_CONTAINER}" \
            -p "${DRILL_SERVER_PORT}:48080" \
            -p "${DRILL_SERVER_ACTUATOR_PORT}:48090" \
            -v "${DRILL_SERVER_JAR}:/app/app.jar:ro" \
            -e JAVA_OPTS="-Xms512m -Xmx512m" \
            -e SPRING_PROFILES_ACTIVE=drill \
            java:8-jdk-alpine \
            java -jar /app/app.jar \
                --spring.profiles.active=drill \
                --spring.datasource.dynamic.datasource.master.url="jdbc:mysql://${DRILL_MYSQL_CONTAINER}:3306/${DRILL_MYSQL_DATABASE}?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true" \
                --spring.datasource.dynamic.datasource.master.username=root \
                --spring.datasource.dynamic.datasource.master.password="${DRILL_MYSQL_ROOT_PASSWORD}" \
                >/dev/null 2>&1; then
        log_error "启动 yudao-server 失败"
        record_step "启动服务" "FAIL" "docker run 失败"
        return 1
    fi

    # 注意：上面使用了容器名连接，需要确保在同一个 Docker 网络
    docker network connect yudao-system_yudao-net "${DRILL_SERVER_CONTAINER}" 2>/dev/null || \
        log_warn "连接到 yudao-net 网络失败（可能是孤立网络）"

    record_step "启动服务" "PASS" "容器=${DRILL_SERVER_CONTAINER} 端口=${DRILL_SERVER_PORT}"
    return 0
}

# ===== 6. 健康检查 =====
check_health() {
    if [ "${SKIP_SERVER}" = true ] || [ "${DRY_RUN}" = true ]; then
        record_step "健康检查" "SKIP" "skip/dry-run"
        return 0
    fi
    log_step "STEP 6: 健康检查 /actuator/health"
    local HEALTH_URL="http://127.0.0.1:${DRILL_SERVER_ACTUATOR_PORT}/actuator/health"
    local WAIT=0
    while [ ${WAIT} -lt ${DRILL_HEALTH_TIMEOUT} ]; do
        local STATUS
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" -m 10 "${HEALTH_URL}" 2>/dev/null || echo "000")
        if [ "${STATUS}" = "200" ]; then
            local BODY
            BODY=$(curl -s -m 10 "${HEALTH_URL}" 2>/dev/null)
            log_info "健康检查通过（${WAIT}s）: ${BODY}"
            record_step "健康检查" "PASS" "状态=200 耗时=${WAIT}s"
            return 0
        fi
        sleep 5
        WAIT=$((WAIT + 5))
        log_info "等待中... (${WAIT}s, status=${STATUS})"
    done
    log_error "健康检查超时"
    record_step "健康检查" "FAIL" "超时=${DRILL_HEALTH_TIMEOUT}s"
    return 1
}

# ===== 7. 测试关键 API =====
test_api() {
    if [ "${SKIP_SERVER}" = true ] || [ "${DRY_RUN}" = true ]; then
        record_step "API 测试" "SKIP" "skip/dry-run"
        return 0
    fi
    log_step "STEP 7: 测试关键 API"
    local BASE_URL="http://127.0.0.1:${DRILL_SERVER_PORT}"

    # 7.1 登录接口
    local LOGIN_BODY
    LOGIN_BODY=$(cat <<EOF
{
  "username": "${DRILL_LOGIN_USERNAME:-admin}",
  "password": "admin123",
  "tenantId": ${DRILL_LOGIN_TENANT:-1},
  "captchaVerification": ""
}
EOF
)
    local LOGIN_RESP
    LOGIN_RESP=$(curl -s -m 30 -X POST "${BASE_URL}/admin-api/system/auth/login" \
        -H "Content-Type: application/json" \
        -H "tenant-id: ${DRILL_LOGIN_TENANT:-1}" \
        -d "${LOGIN_BODY}" 2>/dev/null)
    log_info "登录响应: ${LOGIN_RESP:0:200}"

    if echo "${LOGIN_RESP}" | grep -q "accessToken\|code\":0"; then
        log_info "登录接口测试通过"
        record_step "API: 登录" "PASS" "返回 accessToken"
    else
        log_warn "登录接口测试异常（可能需要真实账号）"
        record_step "API: 登录" "WARN" "响应未包含 accessToken"
    fi

    # 7.2 验证码
    local CAPTCHA_STATUS
    CAPTCHA_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -m 10 \
        "${BASE_URL}/admin-api/system/captcha/get-enable" 2>/dev/null || echo "000")
    log_info "验证码接口状态: ${CAPTCHA_STATUS}"
    record_step "API: 验证码" "$([ "${CAPTCHA_STATUS}" = "200" ] && echo PASS || echo WARN)" "status=${CAPTCHA_STATUS}"

    # 7.3 系统租户
    local TENANT_STATUS
    TENANT_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -m 10 \
        "${BASE_URL}/admin-api/system/tenant/get-id-by-name?name=芋道源码" 2>/dev/null || echo "000")
    log_info "租户接口状态: ${TENANT_STATUS}"
    record_step "API: 租户" "$([ "${TENANT_STATUS}" = "200" ] && echo PASS || echo WARN)" "status=${TENANT_STATUS}"

    return 0
}

# ===== 8. 清理临时资源 =====
cleanup() {
    log_step "STEP 8: 清理临时资源"
    if [ "${DRY_RUN}" = true ]; then
        log_info "[dry-run] 跳过清理"
        record_step "资源清理" "PASS" "dry-run"
        return 0
    fi

    # 停止并删除 drill server 容器
    if [ "${SKIP_SERVER}" = false ]; then
        if docker ps -a --format '{{.Names}}' | grep -q "^${DRILL_SERVER_CONTAINER}$"; then
            log_info "停止 ${DRILL_SERVER_CONTAINER}"
            docker rm -f "${DRILL_SERVER_CONTAINER}" >/dev/null 2>&1 || true
        fi
    fi

    # 停止并删除 drill mysql 容器
    if docker ps -a --format '{{.Names}}' | grep -q "^${DRILL_MYSQL_CONTAINER}$"; then
        log_info "停止 ${DRILL_MYSQL_CONTAINER}"
        docker rm -f "${DRILL_MYSQL_CONTAINER}" >/dev/null 2>&1 || true
    fi

    record_step "资源清理" "PASS" "已清理容器"
    return 0
}

# ===== 9. 生成演练报告 =====
generate_report() {
    log_step "STEP 9: 生成演练报告"

    local END_TIME=$(date +%s)
    local DURATION=$((END_TIME - START_TIME))

    cat > "${REPORT_FILE}" <<EOF
# ${REPORT_TITLE}

## 演练概览

- **演练时间**: $(date '+%Y-%m-%d %H:%M:%S')
- **演练模式**: $([ "${DRY_RUN}" = true ] && echo 'DRY-RUN' || echo 'EXECUTE')
- **总耗时**: ${DURATION} 秒
- **总体结果**: **${OVERALL_STATUS}**

## 演练步骤

| 步骤 | 状态 | 详情 |
|------|------|------|
EOF

    for line in "${STEP_RESULTS[@]}"; do
        echo "${line}" >> "${REPORT_FILE}"
    done

    cat >> "${REPORT_FILE}" <<EOF

## 数据完整性指标

| 指标 | 实际值 | 期望值 |
|------|--------|--------|
EOF

    for line in "${METRIC_RESULTS[@]}"; do
        echo "${line}" >> "${REPORT_FILE}"
    done

    cat >> "${REPORT_FILE}" <<EOF

## 配置信息

- **备份文件**: \`${BACKUP_FILE:-N/A}\`
- **临时 MySQL 容器**: ${DRILL_MYSQL_CONTAINER}
- **临时 MySQL 端口**: ${DRILL_MYSQL_PORT}
- **临时 MySQL 数据库**: ${DRILL_MYSQL_DATABASE}
- **临时 Server 容器**: ${DRILL_SERVER_CONTAINER}
- **临时 Server 端口**: ${DRILL_SERVER_PORT}
- **健康检查超时**: ${DRILL_HEALTH_TIMEOUT}s

## 日志

完整日志见: \`${LOG_FILE}\`

## 结论

$([ "${OVERALL_STATUS}" = "PASS" ] && echo "✅ 容灾演练通过，备份可用，恢复流程正常。" || echo "❌ 容灾演练失败，请检查上述失败步骤并修复。")

---
_报告由 disaster-recovery-drill.sh 自动生成_
EOF

    log_info "报告已生成: ${REPORT_FILE}"
    cat "${REPORT_FILE}" | tee -a "${LOG_FILE}"
}

# ===== 主流程 =====
main() {
    log_info "================ 容灾演练开始 ================"
    log_info "模式: $([ "${DRY_RUN}" = true ] && echo 'DRY-RUN' || echo 'EXECUTE')"
    log_info "备份目录: ${LOCAL_BACKUP_DIR}"
    log_info "临时 MySQL: ${DRILL_MYSQL_CONTAINER}:${DRILL_MYSQL_PORT}"

    # 捕获异常，确保清理与报告生成
    trap 'cleanup; generate_report' EXIT

    # 执行步骤
    select_backup         || { log_error "选择备份失败，终止演练"; exit 1; }
    create_drill_mysql    || { log_error "创建临时 MySQL 失败，终止演练"; exit 1; }
    restore_backup        || { log_error "恢复备份失败，终止演练"; exit 1; }
    verify_data           || log_warn "数据验证异常，继续后续步骤"
    start_drill_server    || log_warn "启动服务失败，继续后续步骤"
    check_health          || log_warn "健康检查异常"
    test_api              || log_warn "API 测试异常"

    log_info "================ 演练步骤执行完毕 ================"

    # 退出码
    if [ "${OVERALL_STATUS}" = "PASS" ]; then
        exit 0
    else
        exit 1
    fi
}

main "$@"
