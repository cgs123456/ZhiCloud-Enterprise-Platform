#!/bin/sh
set -e

# ============================================================
# Alertmanager 启动入口脚本
#
# 背景：Alertmanager 配置文件不支持 ${VAR} 形式的环境变量插值，
#       直接在 alertmanager.yml 中写 ${DINGTALK_WEBHOOK_URL} 会导致
#       URL 非法，Alertmanager 启动失败（P0 阻断问题）。
#
# 本脚本在启动前将模板中的 __DINGTALK_WEBHOOK_URL__ 占位符
# 替换为环境变量 DINGTALK_WEBHOOK_URL 的值；
# 未设置时回退到本地默认地址，保证 Alertmanager 可正常启动。
# ============================================================

WEBHOOK_URL="${DINGTALK_WEBHOOK_URL:-http://127.0.0.1:9093/alertmanager}"
RENDERED_CONFIG="/tmp/alertmanager-rendered.yml"

# 转义 URL 中的 & 与 |，避免 sed 替换串将其解释为反向引用或分隔符
ESCAPED_URL=$(printf '%s\n' "${WEBHOOK_URL}" | sed 's/[&|]/\\&/g')

# 渲染配置到可写临时文件（模板以只读方式挂载，不能原地修改）
sed "s|__DINGTALK_WEBHOOK_URL__|${ESCAPED_URL}|g" \
    /etc/alertmanager/config.tpl.yml > "${RENDERED_CONFIG}"

echo "[alertmanager-entrypoint] webhook url = ${WEBHOOK_URL}"

# 启动 Alertmanager，使用渲染后的配置
exec /bin/alertmanager \
    --config.file="${RENDERED_CONFIG}" \
    --storage.path=/alertmanager \
    --web.listen-address=:9093
