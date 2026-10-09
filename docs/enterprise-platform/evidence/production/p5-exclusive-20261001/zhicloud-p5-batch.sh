#!/bin/bash
# P5 批量驱动：连续两次独占完整形态样本（中继 + 容器内 JVM），每次自带回收。
# 与单次驱动相同的约束：
#  - 每轮各自 start/stop，失败不中断下一轮，但一定回收本轮的沙箱；
#  - 口令只经环境变量继承，不写进命令行与日志；
#  - 单驱动：PID 文件活着就拒绝第二个驱动。
set +e
PW=/c/tools/powershell-7.6.6/pwsh.exe
REPO=/c/Users/Administrator/Desktop/zhicloud/zhicloud-audit-src2
TMPW='C:\Users\Administrator\AppData\Local\Temp'
PIDF=/c/Users/Administrator/AppData/Local/Temp/zhicloud-p5-driver.pid

if [ -f "$PIDF" ]; then
  OLD=$(cat "$PIDF" 2>/dev/null)
  if [ -n "$OLD" ] && ps -p "$OLD" >/dev/null 2>&1; then
    echo "REFUSE_SECOND_DRIVER pid=$OLD"; exit 9
  fi
fi
echo $$ > "$PIDF"
cd "$REPO" || { echo "CD_FAIL"; exit 1; }

CUR_STATE=""
cleanup() {
  if [ -n "$CUR_STATE" ] && [ -f "$CUR_STATE" ]; then
    echo "CLEANUP_BEGIN iter=$ITER $(date -Is)"
    "$PW" -NoProfile -File scripts/production/mysql-sandbox.ps1 stop -StateFile "$CUR_STATE" 2>&1 | tail -2
    echo "CLEANUP_STOP_EXIT=${PIPESTATUS[0]}"
  fi
  echo "P5_BATCH_END $(date -Is)"
}
trap cleanup EXIT

MC='C:\tools\mysql8\bin\mysql.exe'
if [ -f "/c/Users/Administrator/mysql84-sandbox/mysql-8.4.9-winx64/bin/mysql.exe" ]; then
  MC='C:\Users\Administrator\mysql84-sandbox\mysql-8.4.9-winx64\bin\mysql.exe'
fi

for ITER in 1 2; do
  echo "=== ITER $ITER BEGIN $(date -Is) ==="
  CUR_STATE="$TMPW\\zhicloud-p5b$ITER-state.json"
  RM_STATE=$(python -c "import re;print(re.sub(r'\\\\\\\\','/',r'$CUR_STATE'))" 2>/dev/null)
  "$PW" -NoProfile -File scripts/production/mysql-sandbox.ps1 start -RelayLoopback \
    -StateFile "$CUR_STATE" -EvidenceFile "$TMPW\\zhicloud-p5b$ITER-sandbox.json" > "/tmp/p5b$ITER-start.log" 2>&1
  echo "START_EXIT=$?"
  tail -2 "/tmp/p5b$ITER-start.log"
  NAME=$(python -c "
import json
try:
    d=json.load(open(r'$CUR_STATE',encoding='utf-8-sig'))
    print(d['container_name'] + '|' + str(d['relay_port']))
except Exception as e:
    print('|' + type(e).__name__)
")
  P5_CONTAINER=${NAME%%|*}
  P5_RELAY=${NAME##*|}
  echo "ITER$ITER container=$P5_CONTAINER relay=$P5_RELAY"
  if [ -z "$P5_CONTAINER" ] || [ -z "$P5_RELAY" ] || [ "$P5_RELAY" = "KeyError" ]; then
    echo "ITER$ITER GATE_FAIL skip_run"
    continue
  fi
  export ZHICLOUD_P5_ADMIN_PW
  ZHICLOUD_P5_ADMIN_PW=$(python -c "
import json
print(json.load(open(r'$CUR_STATE',encoding='utf-8-sig'))['root_password'], end='')
")
  "$PW" -NoProfile -File scripts/production/verify_mysql_runner_sandbox.ps1 \
    -JavaInSandboxNamespace -SandboxContainerName "$P5_CONTAINER" -DatabasePort "$P5_RELAY" \
    -MysqlClient "$MC" -AdminPasswordEnv ZHICLOUD_P5_ADMIN_PW \
    -Reviewer agent-p5-exclusive-20261001 > "/tmp/p5b$ITER-harness.log" 2>&1
  echo "ITER$ITER HARNESS_EXIT=$?"
  unset ZHICLOUD_P5_ADMIN_PW
  tail -6 "/tmp/p5b$ITER-harness.log"
  "$PW" -NoProfile -File scripts/production/mysql-sandbox.ps1 stop -StateFile "$CUR_STATE" 2>&1 | tail -2
  echo "ITER$ITER STOP_EXIT=${PIPESTATUS[0]}"
  CUR_STATE=""
  echo "=== ITER $ITER END $(date -Is) ==="
done

python -c "
import json, glob, hashlib, os
fs = sorted(glob.glob(r'C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\docs\enterprise-platform\evidence\production\mysql-runner-sandbox-2026100*_21*.json') +
            glob.glob(r'C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2\docs\enterprise-platform\evidence\production\mysql-runner-sandbox-2026100*_2[12]*.json'))
seen = {}
for p in fs:
    if not os.path.isfile(p): continue
    try: d = json.load(open(p, encoding='utf-8-sig'))
    except Exception: continue
    jr = d.get('java_runtime') or {}
    if jr.get('mode') != 'container_namespace': continue
    tr = d.get('transport') or {}
    seen[os.path.basename(p)] = (d.get('status'), len(d.get('failures') or []), tr.get('probe_attempts_total'), hashlib.sha256(open(p,'rb').read()).hexdigest()[:16])
for k in sorted(seen): print('NS_ROW', k, seen[k])
"
echo "P5_BATCH_SUMMARY_DONE"
