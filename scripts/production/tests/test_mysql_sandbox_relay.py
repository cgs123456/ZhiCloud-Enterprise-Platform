"""R-033 方向3（容器内 loopback 中继）接缝守卫。

这些测试只跑 DryRun —— 不接触 Docker daemon —— 但断言的是"宿主侧看到的端口集合"和
"守卫是否真的咬人"，不是注释文本。默认（不加 -RelayLoopback）必须与改动前逐参数一致，
否则这个改造就把所有既有沙箱调用点都改了形态，而那不是一次裁决授权的事。
"""

import json
import shutil
import subprocess
import unittest
from pathlib import Path

# tests/ -> production/ -> scripts/ -> 仓库根：parents[2] 会算到 scripts，
# 于是 SCRIPT 变成 …/scripts/scripts/… 并且整套守卫静默 skip（skip 不等于验过）。
REPO = Path(__file__).resolve().parents[3]
SCRIPT = REPO / 'scripts' / 'production' / 'mysql-sandbox.ps1'


def hosts():
    out = []
    for exe in (shutil.which('pwsh'), shutil.which('powershell')):
        if exe and Path(exe).is_file():
            out.append(exe)
    return out


HOSTS = hosts()
RENDER_CACHE = {}


def dry_run(extra):
    key = tuple(extra)
    if key in RENDER_CACHE:
        return RENDER_CACHE[key]
    if not HOSTS:
        raise unittest.SkipTest('无可用 PowerShell 宿主；中继守卫未经任何验证')
    args = [HOSTS[0], '-NoProfile', '-ExecutionPolicy', 'Bypass',
            '-File', str(SCRIPT), 'start', '-DryRun'] + list(extra)
    done = subprocess.run(args, capture_output=True, text=True,
                          encoding='utf-8', errors='replace', timeout=180, check=False)
    payload = None
    for line in reversed((done.stdout or '').splitlines()):
        line = line.strip()
        if line.startswith('{'):
            payload = json.loads(line)
            break
    result = (done.returncode, payload, (done.stdout or '') + (done.stderr or ''))
    RENDER_CACHE[key] = result
    return result


class RelayWiringTests(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        if not HOSTS:
            raise unittest.SkipTest('pwsh/powershell 均不可用')
        if not SCRIPT.is_file():
            raise unittest.SkipTest(f'missing {SCRIPT}')

    def published_values(self, args):
        rc, payload, text = dry_run(args)
        self.assertEqual(rc, 0, text[-400:])
        self.assertIsNotNone(payload, f'DryRun 未产出 JSON：{text[-400:]}')
        arr = payload['docker_run_args']
        return arr, [arr[i + 1] for i, v in enumerate(arr) if v == '--publish']

    def test_default_run_publishes_only_the_direct_port(self):
        arr, pubs = self.published_values([])
        self.assertEqual(pubs, ['127.0.0.1::3306'], f'默认形态被改了：{pubs}')
        self.assertNotIn('--network', [v for v in arr if v.startswith('container:')])

    def test_relay_mode_publishes_relay_port_before_image_and_mount(self):
        _, payload, _ = dry_run(['-RelayLoopback'])
        arr = payload['docker_run_args']
        pubs = [arr[i + 1] for i, v in enumerate(arr) if v == '--publish']
        self.assertIn('127.0.0.1::3306', pubs)
        relay = [p for p in pubs if p.endswith('::13306')]
        self.assertEqual(len(relay), 1, f'中继端口缺失或重复：{pubs}')
        mount = arr.index('--mount')
        image = arr.index(payload['image'])
        for i, v in enumerate(arr):
            if v == '--publish':
                self.assertLess(i, mount, '--publish 必须排在 --mount 之前')
                self.assertLess(i, image, '--publish 必须排在镜像参数之前')
        # 反向对照：默认形态里这条 13306 发布必须不存在（否则改动就不是可选的了）。
        _, default_payload, _ = dry_run([])
        self.assertNotIn('127.0.0.1::13306', default_payload['docker_run_args'])

    def test_relay_container_name_is_derived_and_pattern_bounded(self):
        _, payload, _ = dry_run(['-RelayLoopback'])
        name = payload['relay_container']
        self.assertRegex(name, r'^zhicloud-mysql-sandbox-relay-[0-9a-f]{8}$')
        self.assertEqual(payload['relay_container_port'], 13306)

    def test_unpinned_relay_image_is_refused(self):
        rc, payload, text = dry_run(['-RelayLoopback', '-RelayImage', 'busybox:1.36'])
        self.assertNotEqual(rc, 0, text[-400:])
        self.assertIsNone(payload)
        self.assertIn('digest-pinned', text)

    def test_latest_tagged_relay_image_is_refused(self):
        rc, payload, text = dry_run(
            ['-RelayLoopback', '-RelayImage',
             'busybox:latest@sha256:0000000000000000000000000000000000000000000000000000000000000000'])
        self.assertNotEqual(rc, 0, text[-400:])
        self.assertIn('mutable relay image tag', text)

    def test_relay_port_collision_and_out_of_range_are_refused(self):
        for bad, needle in (('3306', 'collide with the direct mysqld mapping'),
                            ('0', 'must be 1-65535')):
            rc, payload, text = dry_run(['-RelayLoopback', '-RelayListenPort', bad])
            self.assertNotEqual(rc, 0, f'-RelayListenPort {bad} 未被拒绝：{text[-300:]}')
            self.assertIn(needle, text)

    def test_a_different_digest_pinned_relay_image_is_accepted(self):
        # 反向对照：守卫必须只拒绝"未钉定"，不能顺手拒绝一切非默认镜像，
        # 否则它其实是把参数写死了而不是在校验。
        rc, payload, text = dry_run(
            ['-RelayLoopback', '-RelayImage',
             'alpine:3@sha256:0000000000000000000000000000000000000000000000000000000000000000'])
        self.assertEqual(rc, 0, text[-400:])
        self.assertEqual(payload['relay_image'].split('@')[0], 'alpine:3')

    def test_teardown_removes_the_relay_before_the_parent_container(self):
        # sidecar 以 --network container:<mysqld> 依赖父容器命名空间；顺序反了父容器删不掉，
        # 于是 stop 会"报告成功"却留下幽灵沙箱。这里锁源码顺序，因为 DryRun 不执行拆卸。
        text = SCRIPT.read_text(encoding='utf-8-sig')
        relay = text.find("$removeRelay = Invoke-Docker")
        parent = text.find("$removeContainer = Invoke-Docker")
        self.assertGreater(relay, -1, '拆卸段找不到中继容器删除调用')
        self.assertGreater(parent, -1, '拆卸段找不到父容器删除调用')
        self.assertLess(relay, parent, '必须先删中继再删父容器')

    def test_grant_wording_matches_actual_source_ip_after_r033_direction2(self):
        # R-033 方向 2（用户裁决 2026-10-19）：宿主 Java 进程经 Docker 发布端口连入时，
        # MySQL 在容器内看到的来源地址是 bridge 网关 172.17.0.1（不是 127.0.0.1），
        # 授权主机改为 % 以匹配实际来源。安全边界由 Assert-LoopbackSandboxHost 对 -DatabaseHost
        # （连接目标）的回环守卫保证——脚本仍只允许连 127.0.0.1/::1/localhost，不会被指向远程实例。
        verifier = (REPO / 'scripts' / 'production' / 'verify_mysql_runner_sandbox.ps1').read_text(
            encoding='utf-8-sig')
        self.assertIn("'$DbUser'@'%'", verifier, "授权主机应为 %（实际来源 = Docker bridge 网关）")
        # loopback 守卫仍严格：DatabaseHost 只允许回环
        self.assertIn("SandboxLoopbackHosts = @('127.0.0.1', '::1', 'localhost')", verifier)
        # 授权主机不再是 127.0.0.1（来源地址不匹配，方向 1 已证伪）
        self.assertNotIn("'$DbUser'@'127.0.0.1'", verifier,
                         "授权主机不应仍为 127.0.0.1（来源地址不匹配）")

    def test_transport_preflight_retries_only_the_probe(self):
        # R-034 的结构锁：允许对无副作用的 `SELECT 1` 做有界重试，
        # 但带副作用的 java（迁移/引导）调用必须始终只有一次。
        # 这是静态断言；行为证据是同一次 harness 跑（见 handoff §12.23）。
        text = (REPO / 'scripts' / 'production' / 'verify_mysql_runner_sandbox.ps1').read_text(
            encoding='utf-8-sig')
        self.assertIn('function Wait-TransportReady', text)
        self.assertIn("Invoke-MysqlScalar -Sql 'SELECT 1;'", text)
        self.assertEqual(1, text.count('-FilePath $JavaPath'),
                         'java 调用点必须唯一，否则迁移可能被重试')
        self.assertGreaterEqual(text.count('$null = Wait-TransportReady'), 1)
        # 预检必须在拉起 java 之前，否则挡不住首连抖动落在 java 内部
        preflight = text.index('$null = Wait-TransportReady')
        java_launch = text.index('-FilePath $JavaPath')
        self.assertLess(text.index('function Invoke-JavaMain'), preflight)
        self.assertLess(preflight, java_launch)
        # 重试次数与延时必须写进证据的复现命令，否则"同一条命令"不可复现
        self.assertIn('-TransportProbes $TransportProbes', text)

    def test_namespace_mode_moves_admin_sql_off_the_host_path(self):
        # (b) 的收尾：容器命名空间形态下，管理侧 SQL 也必须走 docker exec，
        # 否则"宿主 → 发布端口"这段仍在，§12.28 的抖动就还有落脚处。
        text = (REPO / 'scripts' / 'production' / 'verify_mysql_runner_sandbox.ps1').read_text(
            encoding='utf-8-sig')
        self.assertIn("Stage 'mysql-query-namespace'", text)
        self.assertIn("'exec', '-i',", text)
        # R-041：口令只经进程环境继承给 `-e MYSQL_PWD`（不带值）；docker 命令行里出现值
        # 就等于任何进程监视器都能读到 root 口令。旧断言曾经**反向钉住**这个缺陷形态。
        self.assertIn("'-e', 'MYSQL_PWD',", text)
        self.assertIn("SetEnvironmentVariable('MYSQL_PWD', $AdminPassword, 'Process')", text)
        self.assertNotIn('"MYSQL_PWD=$AdminPassword"', text)
        self.assertIn('docker_exec_in_namespace', text)
        # 默认（非该形态）路径必须原样保留，否则改动波及既有调用点
        self.assertIn('--host=$DatabaseHost', text)
        self.assertIn('--port=$DatabasePort', text)

    def test_sandbox_script_keeps_the_root_password_out_of_argv(self):
        text = (REPO / 'scripts' / 'production' / 'mysql-sandbox.ps1').read_text(encoding='utf-8-sig')
        self.assertIn('function Set-SandboxMysqlPasswordEnv', text)
        self.assertIn("'exec', '-i', '-e', 'MYSQL_PWD', $ContainerName,", text)
        self.assertIn("'exec', '-e', 'MYSQL_PWD', $ContainerName,", text)
        self.assertNotIn('"MYSQL_PWD=$RootPassword"', text)
        # 两个调用点（健康 SQL 与就绪探测）都必须自己声明环境，否则漏一处就是漏一处缺陷
        self.assertEqual(text.count('Set-SandboxMysqlPasswordEnv -RootPassword $RootPassword'), 2)

    def test_stale_env_file_sweep_is_wired_into_the_namespace_path(self):
        # R-043：--env-file 把口令移出 argv，但也让它短暂落到 %TEMP%；
        # Remove-Item 只在正常退出时执行，所以必须有"下次运行清扫孤儿"的补偿控制，
        # 且清扫结果必须进证据 —— 否则"已经清过了"只是我的一句话。
        text = (REPO / 'scripts' / 'production' / 'verify_mysql_runner_sandbox.ps1').read_text(
            encoding='utf-8-sig')
        self.assertIn(". (Join-Path $PSScriptRoot 'SandboxSecretSweep.ps1')", text)
        self.assertIn('[int]$StaleEnvMinAgeHours = 24', text)
        dot_source = text.index("'SandboxSecretSweep.ps1'")
        sweep_call = text.index('Clear-StaleRunnerEnvFiles -DirectoryPath $env:TEMP')
        env_write = text.index('[System.IO.File]::WriteAllText($envFile')
        self.assertLess(dot_source, sweep_call, '必须先 dot-source 再调用')
        self.assertLess(sweep_call, env_write, '清扫必须早于本运行写出自己的 env 文件')
        self.assertIn('stale_env_sweep = $script:StaleEnvSweep', text)
        # 记录点必须在写证据之前重新赋值：只在 [ordered] 字面量里取一次，取到的是"还没扫过"的 null
        # （§12.50：真跑里 canary 被删了，字段却是 null —— "字段存在"伪装成了"记录有效"）。
        write_back = text.index("$result.secret_handling['stale_env_sweep'] = $script:StaleEnvSweep")
        serialize = text.index('ConvertTo-Json -Depth 12')
        self.assertLess(write_back, serialize, '必须在序列化之前回填')
        self.assertIn("namespace_env_file = 'transient_disk_file_removed_after_each_call'", text)
        # 不做文件级 assertNotIn(旧 note 文案)：那句现在只剩一处，位置是我自己写的解释性注释
        # （引用旧说法以说明为什么改），不是对当前行为的断言。文件级禁令会把"引用旧文案"误判成缺陷。
        self.assertIn('note = \'数据库口令与 bootstrap 明文口令不写进 evidence。', text)

    def test_state_file_is_refused_inside_the_repo(self):
        # R-042：state JSON 内含明文 root_password。默认在每用户 TEMP，
        # 但把 -StateFile 指进仓库工作树会让一份含口令的文件跟源码一起被复制/提交。
        text = (REPO / 'scripts' / 'production' / 'mysql-sandbox.ps1').read_text(encoding='utf-8-sig')
        self.assertIn('$repoPrefix = (Get-RepoRoot).TrimEnd', text)
        self.assertIn('OrdinalIgnoreCase', text)
        self.assertIn('Refusing -StateFile', text)
        self.assertNotIn('.StartsWith($repoPrefix', text,
                         'Path.StartsWith 在 .NET Framework/5.1 上不存在，必须用 Substring+Equals')
        # 证据仍允许落在仓库内（约定如此，且证据不含口令）
        self.assertLess(text.index('Refusing -StateFile'), text.index('function Get-DefaultEvidenceFile') + len(text),
                        '守卫必须存在于脚本主体')

    def test_auditor_retry_is_limited_to_transport_errors(self):
        # 只读审计器可重试，但认证类错误必须立刻返回 —— 不能把真缺陷刷成绿灯。
        text = (REPO / 'scripts' / 'production' / 'verify_mysql_runner_sandbox.ps1').read_text(
            encoding='utf-8-sig')
        self.assertIn('ERROR 1045|Access denied', text)
        # 审计器自身的 60 秒客户端超时也会中断只读查询（§12.30 实测：重试特征表漏掉它，
        # 导致一次真实的传输停摆没有被兜住），故该签名必须在表内。
        self.assertIn('timed out after', text)
        self.assertIn('if ($auditText -match $deterministicPattern) { return }', text)
        self.assertIn('if ($auditText -notmatch $transientPattern) { return }', text)
        self.assertIn('if ([int]$script:lastAuditExitCode -eq 0) { return }', text)

    def test_namespace_mode_also_moves_the_auditor_off_the_host(self):
        # R-039 的闭合条件：该形态下审计器也必须进容器，宿主→数据库不得再有网络路径。
        verifier = (REPO / 'scripts' / 'production' / 'verify_mysql_runner_sandbox.ps1').read_text(
            encoding='utf-8-sig')
        self.assertIn("'--docker-container', $SandboxContainerName", verifier)
        auditor = (REPO / 'scripts' / 'production' / 'audit_bootstrap_admin.py').read_text(
            encoding='utf-8')
        self.assertIn('"docker", "exec", "-i"', auditor)
        self.assertIn('"-e", "MYSQL_PWD"', auditor)
        self.assertIn('docker_exec_in_namespace', auditor)
        # 反向：默认形态仍必须用宿主 mysql.exe 与传入的 host/port
        self.assertIn('args.mysql_client,', auditor)
        # 证据里"实际连到哪"与"请求连到哪"必须是两个字段，否则命名空间形态下
        # host/port 会记成宿主中继端口，与 transport 里的容器内 3306 自相矛盾。
        self.assertIn('"requested_host": args.host,', auditor)
        self.assertIn('"requested_port": args.port,', auditor)
        self.assertIn('"host": ("127.0.0.1" if getattr(args, "docker_container", None) else args.host)', auditor)


if __name__ == '__main__':
    unittest.main()
