import re
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parent.parent.parent
PRODUCTION_DIR = SCRIPTS_DIR / "production"
MODULE = PRODUCTION_DIR / "SandboxNative.ps1"

# 背景：R-G4-2 / R-G3.5-2 —— PowerShell 的 `&` 原生命令调用没有原生超时，
# 所以这些脚本声明的 $TimeoutSeconds / $ReadyTimeoutSeconds 此前从未到达子进程：
# 一次挂起的 docker / mysql / maven / helm 调用就能让门禁既不红也不绿，只是永远不返回。
# 修法是把执行器收敛成一份实现（SandboxNative.ps1），各脚本 dot-source 后在咽喉函数里调用。
#
# 本文件守的是"收敛没有被绕过、而且枚举没漏"：
#   * 枚举是**动态**扫出来的，不是抄一份文件清单——抄来的清单会在下一个脚本加入时静默失真，
#     第一版就是这样：它只列了 5 个脚本，于是 backup-restore-drill / build-container /
#     verify_mysql_history_lock / verify_helm_render 四个自造实现或无超时调用全部漏网。
#   * 行为级验证在 test_sandbox_native_behavior.py；这里只做结构判定，不假装启动了 Docker。

TOOL_INVOCATION = re.compile(
    r"(?<![\w.\-])&\s+(?:docker|mvn|mysql(?:\.exe)?|java(?:\.exe)?"
    r"|helm(?:\.exe)?|kubectl(?:\.exe)?|kubeconform(?:\.exe)?"
    r"|\$(?:Docker|Maven|MysqlClient|JavaPath|PythonPath|Helm\d*|Kubeconform|ComSpec)[\w]*)\b",
    re.IGNORECASE)
GIT_INVOCATION = re.compile(r"(?<![\w.\-])&\s+(?:git|\$git(?:\.Source)?|\$Git[\w.]*)\b", re.IGNORECASE)

# 允许不经过执行器的调用，逐条给理由；新增一条就是本文件该变红的时候。
ALLOWED_UNBOUNDED = (
    # 守卫必须在建立执行器**之前**判断 daemon 归属：先有 context 才谈得上敢不敢建容器。
    "docker context show",
)

# 只有这一处允许自己持有进程：并发锁验证需要"起一个会话、不阻塞、稍后再收"。
PROCESS_LAUNCH_EXCEPTIONS = {"verify_mysql_history_lock.ps1"}

# 第二类例外：常驻守护进程。执行器的契约是"等它退出并带硬超时"，
# 而 mysqld 永远不退出 —— 把它塞进执行器只会得到一个必然超时的调用（假红）。
# 容器形态里对应的位置是 `docker run -d`：CLI 立刻返回、进程由容器持有。
# 代价是这里必须由**另一套**纪律代替硬超时：起后限时探活、停前核对 PID 归属、
# 失败路径自己收尸。下面每条都真断言，所以这不是放宽，而是换个形状继续收紧。
DAEMON_LAUNCH_EXCEPTIONS = {"native_mysqld_sandbox.ps1": "宿主原生 mysqld 守护进程（R-040 方案 B）"}

SELF_LAUNCH = re.compile(r"New-Object\s+System\.Diagnostics\.Process"
                         r"|\[[A-Za-z.]*Process(?:StartInfo)?::new\(\)"
                         r"|=\s*Start-Process\b"
                         r"|\$\w*[Pp]rocess\w*\.Start\(")
MERGE_SITE = re.compile(r"(?<![\w.\-])&\s+\$?\w[\w\.\-]*.*2>&1")
DOWNGRADE = re.compile(r"\$ErrorActionPreference\s*=\s*'Continue'")


def ps1_files():
    return sorted(SCRIPTS_DIR.rglob("*.ps1"))


def code_lines(text):
    """(行号, 文本)，跳过整行注释：注释里的文件名/标记不算数（第一版被这样骗过三次）。"""
    return [(i, line) for i, line in enumerate(text.splitlines(), start=1)
            if not line.strip().startswith("#")]


def code_only(text):
    return "\n".join(line for _no, line in code_lines(text))


def read(path):
    return path.read_text(encoding="utf-8-sig")


def dot_source_lines(text):
    return [line for _no, line in code_lines(text)
            if re.match(r"^\s*\.\s*\(Join-Path.*SandboxNative\.ps1", line)]


def executor_users():
    """所有在代码里真的调用执行器的脚本（动态枚举，不含执行器本身）。"""
    users = []
    for path in ps1_files():
        if path.name == MODULE.name:
            continue
        if "Invoke-BoundedNative" in code_only(read(path)) or "Stop-NativeProcessTree" in code_only(read(path)):
            users.append(path)
    return users


class SandboxNativeSeamTests(unittest.TestCase):
    def test_module_exists_and_is_dot_sourced_by_every_executor_user(self):
        self.assertTrue(MODULE.is_file(), f"缺少共享执行器: {MODULE}")
        users = executor_users()
        # 非空判定：如果执行器调用被整体改写，枚举会变空，此时"逐条断言"会全部恒真。
        self.assertGreaterEqual(len(users), 8,
                                f"只有 {len(users)} 个脚本使用执行器，枚举或执行器本身已失真")
        for path in users:
            with self.subTest(script=path.name):
                self.assertEqual(1, len(dot_source_lines(read(path))),
                                 "使用执行器却没有 dot-source 它")

    def test_bounded_executor_is_defined_exactly_once(self):
        definitions = [path.name for path in ps1_files()
                       if re.search(r"^\s*function\s+Invoke-BoundedNative\b", read(path), re.MULTILINE)]
        self.assertEqual(["SandboxNative.ps1"], definitions,
                         "出现第二份 Invoke-BoundedNative 实现：同一规则的两份实现会漂移")

    def test_no_script_reimplements_process_launching(self):
        """副本不一定以 Invoke-BoundedNative 命名，所以按"自己起进程"的特征再查一遍。"""
        offenders = []
        for path in ps1_files():
            if path.name == MODULE.name:
                continue
            for line_no, line in code_lines(read(path)):
                if SELF_LAUNCH.search(line):
                    offenders.append((path.name, line_no, line.strip()[:90]))
        unexpected = [o for o in offenders
                      if o[0] not in PROCESS_LAUNCH_EXCEPTIONS | set(DAEMON_LAUNCH_EXCEPTIONS)]
        self.assertEqual([], unexpected,
                         "沙箱脚本自己启动进程而不经过共享执行器，等于绕过硬超时"
                         f"（登记的例外仅 {sorted(PROCESS_LAUNCH_EXCEPTIONS | set(DAEMON_LAUNCH_EXCEPTIONS))}）")

    def test_daemon_launch_exception_carries_its_own_bounded_discipline(self):
        """守护进程例外的对价：没有硬超时，就必须有"限时探活 + 归属核对 + 失败收尸"。

        每条都是真断言，且这个例外必须**仍然命中**一次自造启动 ——
        否则它就成了"为让门禁变绿而留的空条目"（同 test_allowed_unbounded_sites_still_exist 的道理）。
        """
        self.assertEqual({"native_mysqld_sandbox.ps1"}, set(DAEMON_LAUNCH_EXCEPTIONS),
                         "守护进程例外被悄悄扩大：每加一个都要重谈对价")
        path = PRODUCTION_DIR / "native_mysqld_sandbox.ps1"
        self.assertTrue(path.is_file(), "例外指向的脚本已不存在，条目应一并删除")
        text = read(path)
        body = code_only(text)

        launched = [(no, line) for no, line in code_lines(text) if SELF_LAUNCH.search(line)]
        self.assertEqual(1, len(launched), f"守护进程启动点应有且只有 1 处，实得 {len(launched)}")
        launch_no = launched[0][0]
        # 这一条在原脚本里就换行了，所以取窗口而不是单行 —— 单行断言会因为排版而假红。
        window = "\n".join(text.splitlines()[launch_no - 1:launch_no + 2])
        self.assertIn("$mysqldBin", window, "唯一允许的自启动必须是 mysqld 本身")
        self.assertIn("-PassThru", window, "拿不到 PID 就无法在停之前核对归属")

        # ① 起后限时探活：必须有上限循环 + 明确的"没起来就报错"
        self.assertRegex(body, r"for \(\$i = 0; \$i -lt \d+;")
        self.assertIn("未监听", body, "探活失败必须报错，不能带着没起来的实例继续跑")
        # ② 停之前核对 PID 归属：定义在哪不算数，要看**带 -ProcId 的调用**至少出现两次
        #    （stop 分支一次、失败收尸一次）。只查"名字在 Stop-Process 之前出现过"会被函数定义行糊过去。
        self.assertGreaterEqual(body.count("Assert-PidIsOurMysqld -ProcId"), 2,
                                "归属核对调用少于 2 处：stop 或失败收尸有一条已经不看 PID")
        # ③ 失败路径自己收尸，不留无人认领的进程
        self.assertIn("isolated_instance_reaped_pid=", body)
        # ④ 客户端调用仍在执行器内（守护进程例外只覆盖 mysqld 那一次启动）
        self.assertGreaterEqual(body.count("Invoke-BoundedNative"), 4,
                                "netstat/初始化/隔离对照/shutdown 至少要 4 次有界调用")
        self.assertNotRegex(body, r"(?<![\w.\-])&\s+\$(?:mysqlBin|adminBin|mysqldBin)\b",
                            "客户端二进制必须走执行器，否则又回到无超时挂死")
        self.assertEqual(1, len(dot_source_lines(text)), "使用执行器却没有 dot-source 它")

    def test_concurrent_session_launcher_shares_the_killer(self):
        """唯一的例外必须仍然复用同一个进程树终止器，否则会退回裸 Kill() 只杀直接子进程。"""
        for name in PROCESS_LAUNCH_EXCEPTIONS:
            path = PRODUCTION_DIR / name
            if not path.is_file():
                self.skipTest(f"{name} 已不存在：例外条目需要一并删除")
            body = code_only(read(path))
            self.assertIn("Stop-NativeProcessTree", body,
                          "并发会话超时后不再走共享终止器")
            self.assertRegex(body, r"\$Session\.process\.WaitForExit\(\$TimeoutMs\)")
            self.assertNotRegex(body, r"\$Session\.process\.Kill\(\)\s*\}\s*catch")

    def test_sandbox_tool_invocations_all_go_through_the_executor(self):
        offenders = []
        for path in ps1_files():
            if path.name == MODULE.name:
                continue
            for line_no, line in code_lines(read(path)):
                if not TOOL_INVOCATION.search(line):
                    continue
                if any(allowed in line for allowed in ALLOWED_UNBOUNDED):
                    continue
                offenders.append((path.name, line_no, line.strip()[:110]))
        self.assertEqual([], offenders,
                         "以下原生调用绕过执行器，会无超时挂住整个门禁")

    def test_allowed_unbounded_sites_still_exist(self):
        """允许清单也要防"为让门禁变绿而加的空条目"：每条都必须真的命中一次。"""
        hits = {allowed: 0 for allowed in ALLOWED_UNBOUNDED}
        for path in ps1_files():
            for _no, line in code_lines(read(path)):
                for allowed in hits:
                    if allowed in line and TOOL_INVOCATION.search(line):
                        hits[allowed] += 1
        for allowed, count in hits.items():
            with self.subTest(site=allowed):
                self.assertGreaterEqual(count, 1, "允许清单里的这条已经不命中，应一并删除")
                self.assertLessEqual(count, 4, f"{allowed} 命中 {count} 次，超出已知守卫数")

    def test_git_metadata_calls_are_enumerated_and_local(self):
        """git 元数据读取(HEAD/branch/status)不入执行器：本地、不联网、不能长挂。
        这条把"未量到"变成"量过并记录"，一旦有人加 & git clone 之类就会突破上限。"""
        hits = []
        for path in ps1_files():
            for line_no, line in code_lines(read(path)):
                if GIT_INVOCATION.search(line) and not line.strip().startswith('#'):
                    hits.append((path.name, line_no, line.strip()[:90]))
        self.assertGreaterEqual(len(hits), 5, "git 枚举可能失真")
        self.assertLessEqual(len(hits), 16, f"git 原生调用增至 {len(hits)} 处，需逐条判定是否会阻塞")
        for name, line_no, line in hits:
            self.assertNotRegex(line, r"\bclone\b|\bfetch\b|\bpull\b|\bpush\b",
                                f"{name}:{line_no} 出现了会联网的 git 调用，必须入执行器")

    def test_stderr_merge_sites_are_downgraded_or_inside_executor(self):
        """回归的不变量：任何把 stderr 并入管道的原生调用都必须处在临时降级区里，
        否则脚本级 Stop 会把一行无害告警升级成终止性异常。"""
        offenders = []
        for path in ps1_files():
            if path.name == MODULE.name:
                continue
            lines = read(path).splitlines()
            for line_no, line in code_lines(read(path)):
                if not MERGE_SITE.search(line):
                    continue
                window = lines[max(0, line_no - 14):line_no]
                if not any(DOWNGRADE.search(ln) for ln in window):
                    offenders.append((path.name, line_no, line.strip()[:90]))
        self.assertEqual([], offenders,
                         "以下混流点会把 stderr 升级成终止性异常（执行器内部除外）")

    def test_downgrade_regions_restore_in_finally(self):
        for path in ps1_files():
            text = read(path)
            lines = text.splitlines()
            for i, line in enumerate(lines):
                if not DOWNGRADE.search(line):
                    continue
                context = "\n".join(lines[i:i + 16])
                self.assertIn("finally", context,
                              f"{path.name}:{i + 1} 降级后没有 finally 还原，等于整段丢掉 Stop")

    def test_no_dotnet_core_only_argumentlist_on_windows_powershell(self):
        """`ProcessStartInfo.ArgumentList` 在 .NET Framework（Windows PowerShell 5.1）上不存在。
        本仓库曾有脚本用了它却只声明 `#requires -Version 5.1`，于是"能解析、不能跑"——
        解析级门禁看不见这类缺陷，这里补上。"""
        offenders = []
        for path in ps1_files():
            text = read(path)
            if ".ArgumentList" not in code_only(text):
                continue
            if "#requires -PSEdition Core" in text:
                continue
            offenders.append(path.name)
        self.assertEqual([], offenders,
                         "使用了 5.1 不存在的 ArgumentList，且未声明 #requires -PSEdition Core")

    def test_module_bounds_wait_and_kills_process_tree(self):
        text = code_only(read(MODULE))
        self.assertRegex(text, r"\$process\.WaitForExit\(\$TimeoutSeconds \* 1000\)")
        self.assertRegex(text, r"taskkill\.exe\s+/PID[\s\S]{0,40}/T\s+/F")
        timeout_block = text[text.index("if (-not $process.WaitForExit"):]
        self.assertIn("throw", timeout_block[:timeout_block.index("$graceMilliseconds")])

    def test_module_uses_async_split_stream_capture(self):
        text = code_only(read(MODULE))
        self.assertEqual(2, len(re.findall(r"ReadToEndAsync\(\)", text)))
        self.assertRegex(text, r"RedirectStandardError\s*=\s*\$true")

    def test_module_writes_stdin_as_utf8_and_closes_it(self):
        """stdin 必须①显式 UTF-8 ②写完关闭 ③子进程先退出时不掩盖真实失败。

        只钉 stdout/stderr 是不够的：GBK 默认码页曾把含中文注释的迁移 SQL 写成乱码，
        真实门禁（物理复制沙箱）因此报 1064。也不能用 StandardInputEncoding——
        它与 ArgumentList 同族，在 .NET Framework（PS 5.1）上不存在。
        """
        text = code_only(read(MODULE))
        self.assertIn('StreamWriter($process.StandardInput.BaseStream', text)
        self.assertNotIn('StandardInputEncoding', text.replace(
            '# 注意这里不能用 StandardInputEncoding', ''))
        write_at = text.index('$stdinWriter.Write(')
        close_at = text.index('$stdinWriter.Close()', write_at)
        self.assertLess(write_at, close_at)
        self.assertIn('Flush()', text[write_at:close_at], '关闭前必须 Flush，否则可能丢字节')
        self.assertIn("catch [System.IO.IOException]", text[write_at:close_at + 500],
                      "子进程提前退出时写 stdin 会抛异常，必须吞掉它让退出码说明真实失败")

    def test_file_landing_paths_use_raw_streams(self):
        """落盘的证据必须用未 Trim 的原始流。
        执行器返回的 StdOut/StdErr 是 Trim 过的：直接写文件会让每份证据少一个结尾换行，
        实测 76 份里有 37 份与改动前不等价——证据字节变化必须是有意的，不能是副作用。"""
        for name in ("verify_helm_render.ps1",):
            body = code_only(read(PRODUCTION_DIR / name))
            self.assertIn("RawStdOut", body, f"{name} 又把 Trim 过的流写进证据文件了")
            self.assertNotIn("WriteAllText($StdoutPath, $run.StdOut", body)
            self.assertNotIn("WriteAllText($StderrPath, $run.StdErr", body)
        module = code_only(read(MODULE))
        module_lines = [line.strip() for line in code_only(read(MODULE)).splitlines()]
        raw = [line for line in module_lines if line.startswith('RawStdOut')]
        self.assertEqual(1, len(raw), '执行器必须只暴露一个 RawStdOut 字段')
        self.assertTrue(raw[0].endswith('= $stdOut'), raw[0])

    def test_docker_run_scripts_clean_up_after_a_hard_stop(self):
        """硬超时杀的是 docker **客户端**；`--rm` 只有客户端活着陪到容器退出才保证回收。
        所以每个用 `docker run` 的门禁脚本都必须在自己失败的路径上按精确容器名强制移除，
        否则超时会留下占着缓存卷与 CPU 的孤儿容器。"""
        text = read(PRODUCTION_DIR / "build-container.ps1")
        body = code_only(text)
        self.assertRegex(body, r"function\s+Remove-BuildContainer")
        self.assertRegex(body, r"'rm',\s*'-f',\s*\$ContainerName")
        # catch 分支与"退出码非 0"分支都要清理，漏一个就是半条路径没兜住
        self.assertEqual(2, len(re.findall(r"Remove-BuildContainer -ContainerName \$containerName", body)),
                         "docker run 的失败路径有一处没做容器回收")
        # 回显必须早于失败抛出，否则失败的构建只剩一句退出码
        self.assertLess(body.index('Write-Output $run.StdOut'),
                        body.index('throw "Containerized Maven build failed'))

    def test_enumeration_is_not_vacuous(self):
        """守卫最怕"扫了 0 个文件还报 OK"：这里钉住被扫面本身。"""
        files = ps1_files()
        self.assertGreaterEqual(len(files), 10, f"只扫到 {len(files)} 个 .ps1，扫描根可能已经变了")
        self.assertTrue(any(p.name == "backup-restore-drill.ps1" for p in files),
                        "扫描根不再覆盖 scripts/production，全部接缝断言都会静默失效")


if __name__ == "__main__":
    unittest.main()
