#!/usr/bin/env bash
# ======================================================================
# 下载 BGE-base-zh-v1.5 ONNX 模型到 yudao-server 资源目录
#
# 用途：为 yudao-module-ai-rag 模块准备本地化 Embedding 模型
#
# 使用方法：
#   bash scripts/download-bge-model.sh
#
# 运行环境：
#   - Linux / macOS / Git Bash / WSL
#   - 需要 curl 或 wget（任一即可）
#
# 模型说明：
#   - 模型：BAAI/bge-base-zh-v1.5（中文，768 维，~390MB）
#   - 用途：yudao-module-ai-rag 的本地 Embedding（ONNX Runtime 嵌入应用）
#   - 维度：768（需与 AiragConfiguration.DEFAULT_VECTOR_DIMENSION 对齐）
#
# 启用 RAG 模块步骤：
#   1. 部署 PostgreSQL 16 + pgvector（参考 docker-compose.yml）
#   2. 执行 sql/postgresql/airag_pgvector.sql 建表
#   3. 运行本脚本下载模型
#   4. 在 AiragConfiguration 中新增 airagEmbeddingModel Bean（参考 EMBEDDING_DECISION.md）
#   5. 配置 yudao.airag.enabled=true
#
# 参考链接：
#   - 模型主页：https://huggingface.co/BAAI/bge-base-zh-v1.5
#   - 选型决策：yudao-module-ai-rag/EMBEDDING_DECISION.md
# ======================================================================
set -e

# ---------------- 配置 ----------------
MODEL_REPO="BAAI/bge-base-zh-v1.5"
BASE_URL="https://huggingface.co/${MODEL_REPO}/resolve/main"
# 目标目录：yudao-server/src/main/resources/airag/bge-base-zh/
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TARGET_DIR="${PROJECT_ROOT}/yudao-server/src/main/resources/airag/bge-base-zh"

# 需要下载的文件列表
FILES=(
    "model.onnx"
    "tokenizer.json"
    "tokenizer_config.json"
    "config.json"
    "vocab.txt"
)

# ---------------- 工具函数 ----------------
log() {
    echo "[download-bge-model] $1"
}

err() {
    echo "[download-bge-model][ERROR] $1" >&2
    exit 1
}

# 检测可用的下载工具：优先 curl，否则 wget
detect_downloader() {
    if command -v curl >/dev/null 2>&1; then
        DOWNLOADER="curl"
    elif command -v wget >/dev/null 2>&1; then
        DOWNLOADER="wget"
    else
        err "未找到 curl 或 wget，请先安装其一后再运行本脚本"
    fi
    log "使用下载工具：${DOWNLOADER}"
}

# 下载单个文件（支持断点续传）
# 用法：download_file <remote_filename>
download_file() {
    local file="$1"
    local url="${BASE_URL}/${file}"
    local target="${TARGET_DIR}/${file}"

    if [ -f "${target}" ]; then
        local size
        size=$(wc -c < "${target}" 2>/dev/null || echo 0)
        if [ "${size}" -gt 0 ]; then
            log "已存在，跳过（断点续传将补全）：${file} (${size} bytes)"
        fi
    fi

    log "开始下载：${file}"
    log "  URL: ${url}"

    if [ "${DOWNLOADER}" = "curl" ]; then
        # -C - : 启用断点续传（自动检测已下载部分）
        # -L   : 跟随重定向（HuggingFace 会重定向到 CDN）
        # --fail : HTTP 错误时返回非零退出码
        # -#   : 显示进度条
        curl -L --fail -C - -# -o "${target}" "${url}" || err "下载失败：${file}"
    else
        # -c : 启用断点续传
        # -q : 显示进度
        wget -c -q --show-progress -O "${target}" "${url}" || err "下载失败：${file}"
    fi

    local size
    size=$(wc -c < "${target}" 2>/dev/null || echo 0)
    log "下载完成：${file} (${size} bytes)"
}

# 显示最终目录结构
show_directory() {
    log "目标目录结构："
    if command -v tree >/dev/null 2>&1; then
        tree -h "${TARGET_DIR}"
    else
        ls -lh "${TARGET_DIR}"
    fi
}

# ---------------- 主流程 ----------------
main() {
    log "BGE-base-zh-v1.5 模型下载脚本"
    log "项目根目录：${PROJECT_ROOT}"
    log "目标目录：${TARGET_DIR}"
    log "模型仓库：https://huggingface.co/${MODEL_REPO}"
    echo ""

    # 1. 检测下载工具
    detect_downloader

    # 2. 创建目标目录
    mkdir -p "${TARGET_DIR}"
    log "已创建目标目录：${TARGET_DIR}"
    echo ""

    # 3. 逐个下载文件
    log "开始下载 ${#FILES[@]} 个文件..."
    for file in "${FILES[@]}"; do
        download_file "${file}"
        echo ""
    done

    # 4. 校验关键文件
    log "校验关键文件..."
    local missing=()
    for file in "${FILES[@]}"; do
        if [ ! -f "${TARGET_DIR}/${file}" ]; then
            missing+=("${file}")
        fi
    done
    if [ ${#missing[@]} -gt 0 ]; then
        err "以下文件缺失：${missing[*]}"
    fi
    log "所有文件校验通过 ✓"
    echo ""

    # 5. 显示目录结构
    show_directory
    echo ""

    # 6. 使用提示
    log "下载完成！"
    log "下一步操作："
    log "  1. 在 AiragConfiguration 中新增 airagEmbeddingModel Bean（参考 EMBEDDING_DECISION.md §4.2 方式 A）"
    log "  2. 补充 spring-ai-transformers 依赖（如未在 yudao-module-ai 中引入）"
    log "  3. 配置 yudao.airag.enabled=true 启用 RAG 模块"
    log "  4. 执行 sql/postgresql/airag_pgvector.sql 建表（如未执行）"
}

main "$@"

# ----------------------------------------------------------------------
# 使用提示：
#   1. 赋予执行权限（仅首次执行需要）：
#          chmod +x scripts/download-bge-model.sh
#   2. 直接运行：
#          ./scripts/download-bge-model.sh
#      或：
#          bash scripts/download-bge-model.sh
# ----------------------------------------------------------------------

