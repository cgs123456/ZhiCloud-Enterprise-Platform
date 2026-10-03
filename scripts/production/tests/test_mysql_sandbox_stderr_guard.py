import re
import unittest
from pathlib import Path

PRODUCTION_DIR = Path(__file__).resolve().parent.parent

TARGET = PRODUCTION_DIR / "mysql-sandbox.ps1"

# 历史（本文件第一版守的东西）：脚本级 $ErrorActionPreference = 'Stop' 会把原生命令的 stderr
# 升级为终止性异常，使 Wait-SandboxReady 的重试在第一轮"正常失败"时整体中止。实测这个缺陷让
# mysql-sandbox.ps1 start 只有在"首次探测恰好已就绪"时才可能成功，从而留下过一批无人认领的沙箱残骸。
# 当时的手法是"每个 `2>&1` 混流点临时降级为 Continue"，本文件当时就是数这些降级区。
#
# 现在探测改走共享执行器 SandboxNative.ps1：两条流分别捕获、不再混流，降级区随之没有存在的理由。
# 所以这里改守**结果**而不是守**手法**——手法换了还按旧手法数，就会得到一个恒绿的守卫：
#   1) 不允许再出现任何 `& <原生命令> ... 2>&1` 混流点（回归即红）；
#   2) 就绪探测的两个判据必须都还在：TCP + 带认证的真实查询，少一个就会过早宣布"就绪"；
#   3) 单条探测必须有自己的超时，否则一次挂住的 docker exec 会让整循环的 deadline 形同虚设；
#   4) 探测失败必须在循环内被当作"尚未就绪"继续重试，而不是抛出终止性异常。
# 真正的行为验证仍需带 Docker 的沙箱实跑（见 05 门禁清单与 §12 台账）。

MERGE_SITE = re.compile(r"&\s+\$?\w[\w-]*.*2>&1")


def native_stderr_merge_sites(text):
    """返回所有 `& <命令> ... 2>&1` 调用点的行号（1-based）。"""
    sites = []
    for i, line in enumerate(text.splitlines(), start=1):
        stripped = line.strip()
        if stripped.startswith("#"):
            continue
        if MERGE_SITE.search(line):
            sites.append((i, stripped))
    return sites


def function_body(text, name):
    """取某个顶层 function 的函数体（这些脚本的 function 一律顶格定义）。"""
    match = re.search(r"^function\s+%s\b" % re.escape(name), text, re.MULTILINE)
    if not match:
        return None
    nxt = re.search(r"^function\s+\w", text[match.end():], re.MULTILINE)
    end = match.end() + nxt.start() if nxt else len(text)
    return text[match.start():end]


def executable_lines(body):
    """去掉整行注释：注释里解释"为什么不用 mysqladmin"，不能被当成"在用 mysqladmin"。"""
    return "\n".join(line for line in body.splitlines() if not line.strip().startswith("#"))


class SandboxReadinessProbeTests(unittest.TestCase):
    def setUp(self):
        self.assertTrue(TARGET.is_file(), f"缺少被守卫的脚本: {TARGET}")
        self.text = TARGET.read_text(encoding="utf-8-sig")
        self.probe = function_body(self.text, "Wait-SandboxReady")
        self.assertIsNotNone(self.probe, "Wait-SandboxReady 已不存在：本守卫的保护对象消失了")
        self.probe_code = executable_lines(self.probe)

    def test_script_still_sets_stop_at_scope(self):
        """前提：脚本确实是 Stop 语义，否则第 4 条守的是一个不存在的问题。"""
        self.assertRegex(self.text, r"\$ErrorActionPreference\s*=\s*'Stop'")

    def test_script_still_reaches_out_to_the_shared_executor(self):
        self.assertIn("SandboxNative.ps1", self.text)
        self.assertGreaterEqual(len(re.findall(r"Invoke-BoundedNative\b", self.text)), 3,
                                "原生调用不再走执行器，硬超时与 stderr 分流同时失效")

    def test_no_merged_stderr_native_call_remains(self):
        self.assertEqual([], native_stderr_merge_sites(self.text),
                         "混流会把 stderr 升级成终止性异常并污染查询结果，请改走 Invoke-BoundedNative")

    def test_readiness_probe_keeps_both_criteria(self):
        """TCP 与带认证查询缺一不可：socket 探测在镜像初始化阶段就会过早成功，
        mysqladmin ping 在 Access denied 时依然返回 0。只看可执行行，
        否则"注释里解释为什么不用 mysqladmin"会被当成违规。"""
        self.assertIn("--protocol=TCP", self.probe_code)
        self.assertIn("SELECT 1", self.probe_code)
        self.assertNotIn("mysqladmin", self.probe_code)
        self.assertRegex(self.probe_code,
                         r"\$probe\.ExitCode\s+-eq\s+0\s+-and\s+\$probe\.StdOut\s+-eq\s+'1'")

    def test_readiness_probe_bounds_each_attempt(self):
        self.assertRegex(self.probe_code, r"\$deadline\s*=\s*\(Get-Date\)\.AddSeconds\(\$ReadyTimeoutSeconds\)")
        self.assertRegex(self.probe_code, r"while\s*\(\(Get-Date\)\s*-lt\s*\$deadline\)")
        self.assertRegex(self.probe_code, r"\[Math\]::Min\(30,\s*\$ReadyTimeoutSeconds\)")
        self.assertRegex(self.probe_code, r"-TimeoutSeconds\s+\$probeTimeout")

    def test_failed_probe_retries_instead_of_aborting(self):
        """降级区消失后，"探测失败不得中止循环"这条不变量必须换个方式继续守住：
        从执行器调用点到下一次 Start-Sleep 之间不允许出现 throw。"""
        call_at = self.probe_code.index("Invoke-BoundedNative")
        window = self.probe_code[call_at:]
        sleep_at = window.index("Start-Sleep")
        self.assertNotIn("throw", window[:sleep_at],
                         "重试循环内抛出会中止重试，等于退回旧缺陷：只有首次探测即成功才可能通过")


if __name__ == "__main__":
    unittest.main()
