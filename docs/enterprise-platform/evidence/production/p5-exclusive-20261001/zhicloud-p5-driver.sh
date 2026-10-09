#!/bin/bash
# 一次性 P5 独占样本驱动（完整形态：中继 + 容器内 JVM）。
# 设计约束：
#  - 无论中途成功/失败/被杀，EXIT trap 都会按精确 state 文件回收沙箱；
#  - 口令只从容器 state 读出后经环境变量继承，绝不写进命令行或日志；
#  - 单一驱动：启动前检查 PID 文件，活着就拒绝第二个驱动（§12.36 的并发错误）。
set +e
PW=/c/tools/powershell-7.6.6/pwsh.exe
REPO=/c/Users/Administrator/Desktop/zhicloud/zhicloud-audit-src2
TMPW='C:\Users\Administrator\AppData\Local\Temp'
STATE="$TMPW\\zhicloud-p5-state.json"
SBEV="$TMPW\\zhicloud-p5-sandbox-evidence.json"
LOG=/c/Users/Administrator/AppData/Local/Temp/zhicloud-p5-run.log
PIDF=/c/Users/Administrator/AppData/Local/Temp/zhicloud-p5-driver.pid

if [ -f "$PIDF" ]; then
  OLD=$(cat "$PIDF" 2>/dev/null)
  if [ -n "$OLD" ] && ps -p "$OLD" >/dev/null 2>&1; then
    echo "REFUSE_SECOND_DRIVER pid=$OLD"
    exit 9
  fi
fi
echo $$ > "$PIDF"

cd "$REPO" || { echo "CD_FAIL"; exit 1; }

cleanup() {
  echo "CLEANUP_BEGIN $(date -Is)"
  "$PW" -NoProfile -File scripts/production/mysql-sandbox.ps1 stop -StateFile "$STATE" 2>&1 | tail -3
  echo "CLEANUP_STOP_EXIT=${PIPESTATUS[0]}"
  echo "P5_DRIVER_END $(date -Is)"
}
trap cleanup EXIT

echo "P5_DRIVER_BEGIN $(date -Is)"
"$PW" -NoProfile -File scripts/production/mysql-sandbox.ps1 start -RelayLoopback -StateFile "$STATE" -EvidenceFile "$SBEV" 2>&1 | tail -6
echo "START_EXIT=${PIPESTATUS[0]}"
if [ ! -f "$STATE" ]; then echo "NO_STATE_ABORT"; exit 2; fi

eval "$(python -c "
import json
d = json.load(open(r'$TMPW\\zhicloud-p5-state.json', encoding='utf-8-sig'))
print('P5_CONTAINER=' + str(d.get('container_name') or ''))
print('P5_RELAY=' + str(d.get('relay_port') or ''))
print('P5_DIRECT=' + str(d.get('host_port') or ''))
print('P5_ROOTPW_SET=' + ('1' if d.get('root_password') else '0'))
")"
echo "container=$P5_CONTAINER relay=$P5_RELAY direct=$P5_DIRECT pw_present=$P5_ROOTPW_SET"
if [ -z "$P5_CONTAINER" ] || [ -z "$P5_RELAY" ] || [ "$P5_ROOTPW_SET" != "1" ]; then
  echo "GATE_FAIL_MISSING_STATE_FIELDS"
  exit 3
fi

MC_DEFAULT='C:\Users\Administrator\mysql84-sandbox\mysql-8.4.9-winx64\bin\mysql.exe'
if [ -f "/c/Users/Administrator/mysql84-sandbox/mysql-8.4.9-winx64/bin/mysql.exe" ]; then
  MC="$MC_DEFAULT"
else
  MC='C:\tools\mysql8\bin\mysql.exe'
fi
echo "mysql_client_choice=$MC"

export ZHICLOUD_P5_ADMIN_PW
ZHICLOUD_P5_ADMIN_PW=$(python -c "
import json
print(json.load(open(r'$TMPW\\zhicloud-p5-state.json', encoding='utf-8-sig'))['root_password'], end='')
")

"$PW" -NoProfile -File scripts/production/verify_mysql_runner_sandbox.ps1 \
  -JavaInSandboxNamespace \
  -SandboxContainerName "$P5_CONTAINER" \
  -DatabasePort "$P5_RELAY" \
  -MysqlClient "$MC" \
  -AdminPasswordEnv ZHICLOUD_P5_ADMIN_PW \
  -Reviewer agent-p5-exclusive-20261001 2>&1 | tail -40
echo "HARNESS_EXIT=${PIPESTATUS[0]}"
unset ZHICLOUD_P5_ADMIN_PW

python -c "
import json, glob, os
fs = sorted(glob.glob(r'C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\docs\enterprise-platform\evidence\production\runner-sandbox-combined-*'))
print('P5_EVIDENCE_COUNT=' + str(len(fs)))
if fs:
    p = fs[-1]
    d = json.load(open(p, encoding='utf-8-sig'))
    print('P5_LATEST_DIR=' + os.path.basename(p))
    print('P5_STATUS=' + str(d.get('status')))
    tr = d.get('transport') or {}
    print('P5_TRANSPORT_PROBES=' + str(tr.get('probe_attempts_total')) + ' waits=' + str(tr.get('probe_waits')))
    jr = d.get('java_runtime') or {}
    print('P5_JAVA_MODE=' + str(jr.get('mode')) + ' endpoint=' + str(jr.get('jdbc_endpoint')) + ' transport=' + str(jr.get('mysql_transport')))
    print('P5_SCENARIOS=' + str(len(d.get('scenarios') or [])))
    print('P5_FAILURES=' + str(len(d.get('failures') or [])))
"
echo "HARNESS_DONE"
