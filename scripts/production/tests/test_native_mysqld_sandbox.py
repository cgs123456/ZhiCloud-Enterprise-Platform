# -*- coding: utf-8 -*-
"""native_mysqld_sandbox.ps1（R-040 方案 B 的仓库内路径）的守卫测试。

两条纪律：
1. 不用 skip —— pwsh 缺失就是 error，"环境没配所以自动跳过"的守卫永远绿。
2. 这些用例只验**拒绝**，一个都不真起 mysqld：每个用例都在守卫处返回，
   因此失败模式是"该拒的没拒"，而不是"留下了没人认领的进程"。
   真起实例的生命周期由 §12.63 的人工留痕证据覆盖，不在这里重复。
"""
from __future__ import annotations

import hashlib
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from typing import NamedTuple

REPO = Path(__file__).resolve().parents[3]
MODULE = REPO / "scripts" / "production" / "native_mysqld_sandbox.ps1"
KNOWN_PWSH = Path(r"C:\tools\powershell-7.6.6\pwsh.exe")
MARKER = ".zhicloud-native-sandbox"
ANSI = re.compile(r"\x1b\[[0-9;]*m")

# 子进程：绑一个回环端口、打印端口号、然后一直 accept 到被 terminate。
LISTENER_SNIPPET = (
    "import socket, time\n"
    "s = socket.socket()\n"
    "s.bind(('127.0.0.1', 0))\n"
    "s.listen(8)\n"
    "print(s.getsockname()[1], flush=True)\n"
    "time.sleep(120)\n"
)


class _Result(NamedTuple):
    returncode: int
    stdout: str
    stderr: str


def _decode(raw: bytes) -> str:
    text = ANSI.sub("", raw.decode("utf-8", "replace")) if raw else ""
    if "\ufffd" in text and raw:
        # 出现替换字符说明不是 UTF-8；改按本机 ANSI 代码页解（cp936 等），仍容错。
        text = ANSI.sub("", raw.decode("gbk", "replace"))
    return text.replace("\x00", "")


def find_pwsh() -> str:
    found = shutil.which("pwsh")
    if found:
        return found
    if KNOWN_PWSH.is_file():
        return str(KNOWN_PWSH)
    raise AssertionError(f"pwsh 不可用（PATH 与 {KNOWN_PWSH} 都没有）：本测试不允许跳过")


def _code_only(text: str) -> str:
    """剥掉行注释与 <# #> 文档块，只留真正会执行的行。

    这两条断言一开始就是红的，原因不是脚本有问题：注释里写着"SET PASSWORD 不生效"
    "本机 wmic.exe 已不存在"。对整份文件做 assertNotIn，等于把"解释过为什么不能用"
    当成"正在用"来判 —— 只会逼人删注释。
    """
    lines, in_block = [], False
    for line in text.splitlines():
        stripped = line.strip()
        if in_block:
            if stripped.startswith("#>"):
                in_block = False
            continue
        if stripped.startswith("<#"):
            in_block = True
            continue
        if stripped.startswith("#"):
            continue
        lines.append(line)
    return "\n".join(lines)


class StaticShapeTest(unittest.TestCase):
    def setUp(self):
        self.raw = MODULE.read_bytes()
        self.text = self.raw.decode("utf-8-sig")
        self.code = _code_only(self.text)

    def test_module_is_bom_plus_lf(self):
        self.assertTrue(self.raw.startswith(b"\xef\xbb\xbf"), "5.1 需要 BOM 才能读非 ASCII 的 .ps1")
        self.assertNotIn(b"\r\n", self.raw, "必须是 LF")

    def test_every_refusal_rule_is_worded_as_a_refusal(self):
        # 每条守卫都要能自证"拒绝了谁"，光 throw 一句泛化文案不算守卫。
        for needle in (
            "拒绝 3306",
            "越界：只允许 1024-65535",
            "拒绝把原生实例目录放在仓库工作树",
            "可能属于别人，拒绝初始化、拒绝复用、更拒绝删除",
            "不是本脚本创建的实例，拒绝操作",
            "不是 mysqld.exe：拒绝停止别人的进程",
            "读不到 PID",
            "purge 会删除 datadir 与凭据文件",
            "实例仍在运行：先 stop 再 purge",
        ):
            self.assertIn(needle, self.code, f"缺少拒绝文案：{needle}")

    def test_password_bootstrap_is_alter_user_not_set_password(self):
        # 实测缺陷：SET PASSWORD 在刚 --initialize-insecure 的实例上不生效，
        # 之后所有连接 1045，而现场看起来"脚本跑成功了"。
        self.assertIn("ALTER USER 'root'@'localhost' IDENTIFIED BY", self.code)
        self.assertNotIn("SET PASSWORD", self.code)

    def test_credentials_never_enter_argv(self):
        self.assertIn("$pwFile", self.code)
        self.assertIn("$env:MYSQL_PWD = $pw", self.code)
        self.assertIn("[Environment]::SetEnvironmentVariable('MYSQL_PWD', $null, 'Process')", self.code)
        # 口令只能来自文件；出现 -p<pw> / --password= / --env KEY=VALUE 就是把秘密交给 argv
        self.assertNotIn("--password=", self.code)
        self.assertIsNone(re.search(r"['\"]-p['\"]\s*\+|\-p' \+ \$pw", self.code))
        self.assertNotIn("--env", self.code)

    def test_loopback_only_and_no_xprotocol(self):
        self.assertIn("'--bind-address=127.0.0.1'", self.code)
        self.assertIn("'--mysqlx=OFF'", self.code)
        # 不提供把 bind 改成 0.0.0.0 的口子
        self.assertNotIn("0.0.0.0", self.code)

    def test_isolation_control_flattens_separators_instead_of_deleting_them(self):
        # 服务端 @@datadir 以双反斜杠回显；早期两版归一化都错：
        # 单反斜杠直接比较 → 把自己的合法实例判成外来实例；
        # .NET String.Replace 不接受空 oldValue → 运行期抛异常。
        self.assertIn("-split '[^A-Za-z0-9:.]'", self.code)
        self.assertIn("[System.StringComparison]::OrdinalIgnoreCase", self.code)
        self.assertNotIn(".Replace('\\', '')", self.code)
        self.assertIn("amz_schemas=0", self.code)

    def test_failure_path_reaps_the_instance_it_just_started(self):
        # 隔离对照失败时不能只报错、留一个没人认领的 mysqld（§12.40 的孤儿侧车教训）
        self.assertIn("isolated_instance_reaped_pid=", self.code)
        self.assertLess(self.code.index("isolated_instance_reaped_pid="),
                        self.code.index("隔离对照不通过：服务端 @@datadir"))

    def test_stop_branch_deletes_nothing(self):
        stop_branch = self.code.split("'stop' {", 1)[1].split("'status' {", 1)[0]
        self.assertNotIn("Remove-Item", stop_branch, "stop 删 .pw 会让整个 datadir 变成砖（§12.63）")
        purge_branch = self.code.split("'purge' {", 1)[1]
        self.assertEqual(1, self.code.count("Remove-Item -LiteralPath $pwFile"),
                         "只有 purge 可以删凭据文件")
        self.assertIn("Remove-Item -LiteralPath $pwFile", purge_branch)

    def test_ownership_uses_cim_not_wmic(self):
        self.assertIn("Get-CimInstance -ClassName Win32_Process", self.code)
        self.assertNotIn("wmic", self.code.lower(), "本机 wmic.exe 已不存在，命中即假绿")


class NativeSandboxBehaviorTest(unittest.TestCase):
    """真调用：每条都必须以拒绝收场，并且拒绝必须发生在任何写/起进程之前。"""

    def setUp(self):
        self.base = Path(tempfile.mkdtemp(prefix="zc-native-guard-"))
        self.pw_runs = []

    def tearDown(self):
        shutil.rmtree(self.base, ignore_errors=True)

    def _run(self, *args, timeout=240):
        # 不写 encoding='utf-8'：pwsh 把 throw 的错误记录按**本机 ANSI 代码页**（这台是 cp936）
        # 写到 stderr，强行按 UTF-8 解会得到乱码，于是"守卫确实拒了"却断言失败（实测踩过）。
        proc = subprocess.run(
            [find_pwsh(), "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(MODULE), *args],
            capture_output=True, timeout=timeout)
        out = _decode(proc.stdout)
        err = _decode(proc.stderr)
        self.pw_runs.append((args, proc.returncode, out, err))
        return _Result(proc.returncode, out, err)

    @staticmethod
    def _combined(proc) -> str:
        # pwsh 的错误记录会按控制台宽度折行，长文案可能被从中间切断；
        # 断言只关心"有没有这句拒绝"，所以把空白全部压掉再比。
        return re.sub(r"\s+", "", proc.stdout + "\n" + proc.stderr)

    def _free_port(self) -> int:
        with socket.socket() as s:
            s.bind(("127.0.0.1", 0))
            return s.getsockname()[1]

    def test_default_port_3306_is_refused(self):
        target = self.base / "attempt-3306"
        proc = self._run("-Command", "start", "-Port", "3306", "-BaseDir", str(target))
        self.assertNotEqual(0, proc.returncode, "3306 属于别人的 mysqld，绝不能起：" + self._combined(proc))
        self.assertIn("拒绝3306", self._combined(proc))
        self.assertFalse(target.exists(), "端口守卫之后不得留下任何目录")

    def test_out_of_range_port_is_refused(self):
        proc = self._run("-Command", "status", "-Port", "80", "-BaseDir", str(self.base / "p80"))
        self.assertNotEqual(0, proc.returncode, self._combined(proc))
        self.assertIn("越界", self._combined(proc))

    def test_repo_internal_basedir_is_refused(self):
        # R-042 同方向：数据与凭据不得落进工作树，哪怕只是"看一眼"的命令。
        inside = REPO / f"zc-native-refusal-{os.urandom(4).hex()}"
        proc = self._run("-Command", "status", "-BaseDir", str(inside))
        self.assertNotEqual(0, proc.returncode, self._combined(proc))
        self.assertIn("拒绝把原生实例目录放在仓库工作树", self._combined(proc))
        self.assertFalse(inside.exists(), "守卫之后工作树里必须一个文件都没有")

    def test_non_empty_unmarked_datadir_is_refused_and_stays_unmarked(self):
        # §12.60：C:\tools\mysql8\data 是别人在跑的库（71 个 amz_*）。
        # 这条用例把那种目录的形状复制到临时位置，验证脚本既不初始化、不复用、也不认领它。
        data = self.base / "someone-elses" / "data"
        data.mkdir(parents=True)
        victim = data / "ibdata1"
        victim.write_bytes(b"pretend-existing-innodb-data-not-for-mei" * 8)
        before = hashlib.sha256(victim.read_bytes()).hexdigest()

        proc = self._run("-Command", "start", "-Port", str(self._free_port()),
                         "-BaseDir", str(self.base / "someone-elses"))
        combined = self._combined(proc)
        self.assertNotEqual(0, proc.returncode, "非空且无标记的 datadir 必须拒绝：" + combined)
        self.assertIn("可能属于别人，拒绝初始化、拒绝复用、更拒绝删除", combined)
        self.assertFalse((self.base / "someone-elses" / MARKER).exists(),
                         "拒绝之前绝不能写标记：写了就等于把别人的目录认领成我们的")
        self.assertFalse((self.base / "someone-elses" / ".pw").exists())
        self.assertEqual(before, hashlib.sha256(victim.read_bytes()).hexdigest(), "拒绝不能顺手改数据")
        self.assertNotIn("started_pid=", combined)

    def test_stop_refuses_a_port_held_by_a_non_mysqld_process(self):
        # 归属核对必须面对一个"活的、名字对不上"的真实进程：起一个子进程持有监听端口，
        # 再用它自己的 PID 存活与否做判据。
        # 早先我用**测试进程自己**的套接字并去 connect 它 —— 那条断言是自我指涉的：
        # 脚本若真把进程杀了，测试进程也一起没了，永远只能报"测试异常"而不是"守卫失效"。
        child = subprocess.Popen(
            [sys.executable, "-c", LISTENER_SNIPPET],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, encoding="utf-8")
        try:
            port_line = child.stdout.readline().strip()
            self.assertTrue(port_line.isdigit(), f"子监听进程没给出端口：{port_line}")
            port = int(port_line)
            box = self.base / "held"
            box.mkdir()
            (box / MARKER).write_text("created=fake-by-test\n", encoding="ascii")

            proc = self._run("-Command", "stop", "-Port", str(port), "-BaseDir", str(box))
            combined = self._combined(proc)
            self.assertNotEqual(0, proc.returncode,
                                f"端口 {port} 的持有者不是 mysqld.exe，必须拒绝：" + combined)
            self.assertIn("拒绝停止别人的进程", combined)
            self.assertNotIn("shutdown_exit=", combined, "拒绝之后不能已经发了 shutdown")
            self.assertIsNone(child.poll(), "PID 归属核对失效：脚本把别人的进程杀了")
        finally:
            child.terminate()
            try:
                child.wait(timeout=20)
            except subprocess.TimeoutExpired:
                child.kill()
                child.wait(timeout=20)
            for pipe in (child.stdout, child.stderr):
                if pipe is not None:
                    pipe.close()

    def test_stop_and_purge_without_marker_are_both_refused(self):
        box = self.base / "unmarked"
        (box / "data").mkdir(parents=True)
        for cmd in ("stop", "purge"):
            proc = self._run("-Command", cmd, "-Port", str(self._free_port()), "-BaseDir", str(box))
            combined = self._combined(proc)
            self.assertNotEqual(0, proc.returncode, f"{cmd} 无标记必须拒绝：" + combined)
            self.assertIn("不是本脚本创建的实例，拒绝操作", combined)
        self.assertTrue((box / "data").exists(), "拒绝不能变成变相删除")

    def test_stop_when_not_running_keeps_the_password_file(self):
        # §12.63 自坑：手工删掉 .pw 后 datadir 立刻不可用。stop 因此永远不碰凭据文件。
        box = self.base / "idle"
        box.mkdir()
        (box / MARKER).write_text("created=fake-by-test\n", encoding="ascii")
        pw = box / ".pw"
        pw.write_text("not-a-real-secret-value\n", encoding="ascii")
        proc = self._run("-Command", "stop", "-Port", str(self._free_port()), "-BaseDir", str(box))
        self.assertEqual(0, proc.returncode, self._combined(proc))
        self.assertIn("not_running=true", proc.stdout)
        self.assertTrue(pw.exists(), "stop 删了凭据文件 = 把数据目录变成砖")

    def test_purge_without_force_deletes_nothing(self):
        box = self.base / "purgeable"
        (box / "data").mkdir(parents=True)
        (box / "data" / "ibdata1").write_bytes(b"x" * 32)
        (box / MARKER).write_text("created=fake-by-test\n", encoding="ascii")
        (box / ".pw").write_text("not-a-real-secret-value\n", encoding="ascii")

        proc = self._run("-Command", "purge", "-Port", str(self._free_port()), "-BaseDir", str(box))
        combined = self._combined(proc)
        self.assertNotEqual(0, proc.returncode, "purge 不带 -Force 必须拒绝：" + combined)
        self.assertIn("purge会删除datadir与凭据文件", combined)
        self.assertTrue((box / "data" / "ibdata1").exists())
        self.assertTrue((box / ".pw").exists())
        self.assertTrue((box / MARKER).exists())


if __name__ == "__main__":
    unittest.main()
