#!/bin/bash
##==================================================================
## yudao-server 部署前安全检查脚本
## 检查项：
##   1. SQL 中是否存在 BCrypt strength=4 的弱哈希（$2a$04$）
##   2. SQL 中是否存在默认账号（admin/yudao/yuanma/test）使用默认密码
##   3. application.yaml 是否存在硬编码敏感默认值
##   4. application-prod.yaml 是否覆盖了所有敏感配置
##   5. Dockerfile 是否使用 nonroot 用户
##   6. K8s yaml 是否包含 preStop / securityContext
## 用法：
##   ./security-check.sh [--sql-dir=sql/mysql] [--fail-on-warn]
## 退出码：
##   0 = 通过（可能存在 warning）
##   1 = 失败（存在 critical 风险）
##==================================================================

set -uo pipefail

# ===== 默认配置 =====
PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SQL_DIR="${PROJECT_ROOT}/sql/mysql"
APP_YAML="${PROJECT_ROOT}/yudao-server/src/main/resources/application.yaml"
APP_PROD_YAML="${PROJECT_ROOT}/yudao-server/src/main/resources/application-prod.yaml"
DOCKERFILE="${PROJECT_ROOT}/yudao-server/Dockerfile"
K8S_YAML="${PROJECT_ROOT}/k8s/yudao-server.yaml"
FAIL_ON_WARN=false
WARN_COUNT=0
CRITICAL_COUNT=0

# ===== 解析参数 =====
for arg in "$@"; do
    case $arg in
        --sql-dir=*)
            SQL_DIR="${arg#*=}"
            ;;
        --fail-on-warn)
            FAIL_ON_WARN=true
            ;;
        *)
            echo "未知参数: $arg"
            echo "用法: $0 [--sql-dir=sql/mysql] [--fail-on-warn]"
            exit 1
            ;;
    esac
done

# ===== 颜色定义 =====
RED='\033[0;31m'
YELLOW='\033[0;33m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
    WARN_COUNT=$((WARN_COUNT + 1))
}

critical() {
    echo -e "${RED}[CRITICAL]${NC} $1"
    CRITICAL_COUNT=$((CRITICAL_COUNT + 1))
}

info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

# ===== 1. 检查 BCrypt 弱哈希 =====
echo ""
echo "===== 1. 检查 BCrypt 密码哈希强度 ====="
if [ -d "${SQL_DIR}" ]; then
    WEAK_HASH_COUNT=$(grep -rE '\$2a\$04\$' "${SQL_DIR}" 2>/dev/null | wc -l || echo 0)
    if [ "${WEAK_HASH_COUNT}" -gt 0 ]; then
        warn "发现 ${WEAK_HASH_COUNT} 处 BCrypt strength=4 弱哈希（\$2a\$04\$），存在暴力破解风险"
        echo "  受影响文件："
        grep -rlE '\$2a\$04\$' "${SQL_DIR}" 2>/dev/null | sed 's/^/    - /'
        echo "  修复建议："
        echo "    部署后立即通过 admin 后台重置所有默认账号密码（admin/yudao/yuanma/test 等）"
        echo "    或执行 SQL: UPDATE system_users SET password = '<新$2a\$10\$哈希>' WHERE username IN ('admin','yudao','yuanma','test');"
    else
        info "未发现 BCrypt strength=4 弱哈希"
    fi
else
    warn "SQL 目录不存在: ${SQL_DIR}"
fi

# ===== 2. 检查默认账号 =====
echo ""
echo "===== 2. 检查默认账号风险 ====="
if [ -d "${SQL_DIR}" ]; then
    DEFAULT_USER_COUNT=$(grep -rE "INSERT INTO \`system_users\`.*'admin'|INSERT INTO \`system_users\`.*'yudao'|INSERT INTO \`system_users\`.*'yuanma'|INSERT INTO \`system_users\`.*'test'" "${SQL_DIR}" 2>/dev/null | wc -l || echo 0)
    if [ "${DEFAULT_USER_COUNT}" -gt 0 ]; then
        info "发现 ${DEFAULT_USER_COUNT} 个默认账号插入语句（admin/yudao/yuanma/test），生产部署后必须修改默认密码"
        echo "  修复建议："
        echo "    1. 部署完成后立即登录 admin 后台重置密码"
        echo "    2. 禁用或删除不必要的内置账号"
        echo "    3. 创建新的管理员账号后，将默认 admin 账号禁用"
    fi
fi

# ===== 3. 检查 application.yaml 硬编码敏感默认值 =====
echo ""
echo "===== 3. 检查 application.yaml 硬编码敏感默认值 ====="
if [ -f "${APP_YAML}" ]; then
    HARDCODED_SECRETS=$(grep -E '(password|secret|key|token):.*[a-zA-Z0-9]{16,}' "${APP_YAML}" 2>/dev/null | grep -vE '^\s*#|\$\{|example|test|demo' | wc -l || echo 0)
    if [ "${HARDCODED_SECRETS}" -gt 0 ]; then
        warn "application.yaml 中发现 ${HARDCODED_SECRETS} 处可能硬编码的敏感值（建议改为 \${ENV_VAR:default}）"
    else
        info "application.yaml 中未发现明显的硬编码敏感值"
    fi
fi

# ===== 4. 检查 application-prod.yaml 配置完整性 =====
echo ""
echo "===== 4. 检查 application-prod.yaml 配置完整性 ====="
if [ -f "${APP_PROD_YAML}" ]; then
    REQUIRED_ENVS=("MASTER_DATASOURCE_URL" "MASTER_DATASOURCE_USERNAME" "MASTER_DATASOURCE_PASSWORD"
                   "REDIS_PASSWORD" "MYBATIS_ENCRYPTOR_PASSWORD" "DRUID_USERNAME" "DRUID_PASSWORD")
    MISSING_ENVS=()
    for env in "${REQUIRED_ENVS[@]}"; do
        if ! grep -q "\${${env}" "${APP_PROD_YAML}" 2>/dev/null; then
            MISSING_ENVS+=("${env}")
        fi
    done
    if [ ${#MISSING_ENVS[@]} -gt 0 ]; then
        critical "application-prod.yaml 缺少以下必需的环境变量配置：${MISSING_ENVS[*]}"
    else
        info "application-prod.yaml 环境变量配置完整"
    fi

    # 检查生产 profile 是否启用 XSS / 关闭 Swagger
    if grep -q "xss:" "${APP_PROD_YAML}" && grep -A1 "xss:" "${APP_PROD_YAML}" | grep -q "enable: true"; then
        info "生产环境 XSS 防护已启用"
    else
        critical "生产环境未启用 XSS 防护（yudao.xss.enable: true）"
    fi
    if grep -q "swagger-ui:" "${APP_PROD_YAML}" && grep -A1 "swagger-ui:" "${APP_PROD_YAML}" | grep -q "enabled: false"; then
        info "生产环境 Swagger UI 已关闭"
    else
        warn "生产环境未关闭 Swagger UI"
    fi
else
    critical "application-prod.yaml 不存在"
fi

# ===== 5. 检查 Dockerfile 安全性 =====
echo ""
echo "===== 5. 检查 Dockerfile 安全性 ====="
if [ -f "${DOCKERFILE}" ]; then
    if grep -qE "^USER\s+(nonroot|yudao|[0-9]+)" "${DOCKERFILE}"; then
        info "Dockerfile 已配置非 root 用户"
    else
        critical "Dockerfile 未配置非 root 用户（USER 指令）"
    fi
    if grep -q "HEALTHCHECK" "${DOCKERFILE}"; then
        info "Dockerfile 已配置 HEALTHCHECK"
    else
        warn "Dockerfile 未配置 HEALTHCHECK"
    fi
    if grep -qE "^FROM\s+\S+\s+AS\s+builder" "${DOCKERFILE}"; then
        info "Dockerfile 使用多阶段构建"
    else
        warn "Dockerfile 未使用多阶段构建"
    fi
else
    critical "Dockerfile 不存在"
fi

# ===== 6. 检查 K8s yaml 安全性 =====
echo ""
echo "===== 6. 检查 K8s yaml 安全性 ====="
if [ -f "${K8S_YAML}" ]; then
    if grep -q "preStop" "${K8S_YAML}"; then
        info "K8s 已配置 preStop hook（优雅停机）"
    else
        warn "K8s 未配置 preStop hook"
    fi
    if grep -q "runAsNonRoot: true" "${K8S_YAML}"; then
        info "K8s 已配置 runAsNonRoot"
    else
        critical "K8s 未配置 runAsNonRoot"
    fi
    if grep -q "readOnlyRootFilesystem" "${K8S_YAML}"; then
        info "K8s 已配置 readOnlyRootFilesystem"
    fi
    if grep -q "NetworkPolicy" "${K8S_YAML}"; then
        info "K8s 已配置 NetworkPolicy"
    else
        warn "K8s 未配置 NetworkPolicy"
    fi
    if grep -q "PodDisruptionBudget" "${K8S_YAML}"; then
        info "K8s 已配置 PodDisruptionBudget"
    fi
else
    warn "K8s yaml 不存在: ${K8S_YAML}"
fi

# ===== 汇总报告 =====
echo ""
echo "========== 安全检查汇总 =========="
echo "  CRITICAL: ${CRITICAL_COUNT}"
echo "  WARN:     ${WARN_COUNT}"
echo "=================================="

if [ "${CRITICAL_COUNT}" -gt 0 ]; then
    echo ""
    echo -e "${RED}部署被阻止：存在 ${CRITICAL_COUNT} 个 CRITICAL 风险，必须修复后才能部署${NC}"
    exit 1
fi

if [ "${WARN_COUNT}" -gt 0 ] && [ "${FAIL_ON_WARN}" = true ]; then
    echo ""
    echo -e "${YELLOW}存在 ${WARN_COUNT} 个 WARN 风险，--fail-on-warn 模式下视为失败${NC}"
    exit 1
fi

echo ""
info "安全检查通过（允许部署）"
exit 0
