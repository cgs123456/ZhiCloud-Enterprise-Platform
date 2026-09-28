# -*- coding: utf-8 -*-
# check_commit_readiness.py 回归测试 (入仓前置守卫)。
#
# 契约目标:
# 1. CI 直接调用的脚本/策略文件若未被 Git 跟踪必须失败 —— 否则 CI 会以"文件不存在"失败,
#    与"门禁按设计阻断"混为一谈, 团队会习惯性忽略红灯, 门禁形同虚设。
# 2. 证据日志被 .gitignore 的 *.log 静默排除必须被报出来 (文档引用会变成悬空链接)。
# 3. 不在 Git 工作树中必须 exit=2, 不能被误判为"通过"。
# 4. 本脚本只读, 不得执行任何 add / commit。

import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
REPO_ROOT = PRODUCTION_DIR.parent.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import check_commit_readiness as cr  # noqa: E402


def git(repo: Path, *args: str) -> subprocess.CompletedProcess:
    return subprocess.run(["git", *args], cwd=repo, capture_output=True, text=True,
                          encoding="utf-8", errors="replace")


class TempRepoTestCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self.tmp.name)
        self.assertEqual(git(self.repo, "init", "-q").returncode, 0)
        git(self.repo, "config", "user.email", "test@example.com")
        git(self.repo, "config", "user.name", "test")

    def tearDown(self):
        self.tmp.cleanup()

    def write(self, rel: str, content: str = "x") -> Path:
        path = self.repo / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        return path


class RequiredTrackingTests(TempRepoTestCase):
    def test_tracked_file_passes(self):
        self.write("scripts/gate.py")
        self.assertEqual(git(self.repo, "add", "scripts/gate.py").returncode, 0)
        findings = cr.check_required(self.repo, ["scripts/gate.py"])
        self.assertEqual(findings[0]["status"], "tracked")
        self.assertEqual(cr.build_report(self.repo, ["scripts/gate.py"])["decision"], "pass")

    def test_untracked_file_blocks(self):
        self.write("scripts/gate.py")
        findings = cr.check_required(self.repo, ["scripts/gate.py"])
        self.assertEqual(findings[0]["status"], "untracked")

    def test_ignored_file_blocks(self):
        self.write(".gitignore", "*.log\n")
        self.write("run.log")
        findings = cr.check_required(self.repo, ["run.log"])
        self.assertEqual(findings[0]["status"], "ignored")

    def test_missing_file_blocks(self):
        findings = cr.check_required(self.repo, ["scripts/nope.py"])
        self.assertEqual(findings[0]["status"], "missing")


class EvidenceLogTests(TempRepoTestCase):
    def _evidence(self, name: str) -> Path:
        return self.write(f"docs/enterprise-platform/evidence/production/{name}")

    def test_ignored_evidence_log_reported(self):
        self.write(".gitignore", "*.log\n")
        self._evidence("python-full-20260928_0146.log")
        findings = cr.check_evidence(self.repo)
        self.assertEqual(len(findings), 1)
        self.assertEqual(findings[0]["status"], "ignored_evidence")

    def test_gitignore_negation_clears_evidence_log(self):
        self.write(".gitignore", "*.log\n!docs/enterprise-platform/evidence/production/**/*.log\n")
        self._evidence("python-full-20260928_0146.log")
        self.assertEqual(cr.check_evidence(self.repo), [])
        report = cr.build_report(self.repo, [])
        self.assertEqual(report["decision"], "pass")
        self.assertEqual(report["counts"]["ignored_evidence_logs"], 0)


class CliTests(TempRepoTestCase):
    def _run(self, args):
        return subprocess.run(
            [sys.executable, str(PRODUCTION_DIR / "check_commit_readiness.py"), *args],
            capture_output=True, text=True, encoding="utf-8", errors="replace")

    def test_exit_zero_when_clean(self):
        self.write("scripts/gate.py")
        git(self.repo, "add", "scripts/gate.py")
        out = self.repo / "out.json"
        proc = self._run(["--repo", str(self.repo), "--output", str(out),
                          "--required", "scripts/gate.py"])
        self.assertEqual(proc.returncode, 0, proc.stdout + proc.stderr)
        self.assertEqual(json.loads(out.read_text(encoding="utf-8"))["decision"], "pass")

    def test_exit_one_when_untracked(self):
        self.write("scripts/gate.py")
        proc = self._run(["--repo", str(self.repo), "--required", "scripts/gate.py", "--quiet"])
        self.assertEqual(proc.returncode, 1, proc.stdout + proc.stderr)
        self.assertIn("untracked", proc.stdout)

    def test_exit_one_when_ignored(self):
        self.write(".gitignore", "*.log\n")
        self.write("run.log")
        proc = self._run(["--repo", str(self.repo), "--required", "run.log", "--quiet"])
        self.assertEqual(proc.returncode, 1, proc.stdout + proc.stderr)

    def test_exit_two_outside_repo(self):
        outside = Path(tempfile.gettempdir()) / "zhicloud-not-a-repo-check"
        outside.mkdir(parents=True, exist_ok=True)
        proc = self._run(["--repo", str(outside)])
        self.assertEqual(proc.returncode, 2, proc.stdout + proc.stderr)

    def test_flags_remain_open(self):
        self.write("scripts/gate.py")
        git(self.repo, "add", "scripts/gate.py")
        report = cr.build_report(self.repo, ["scripts/gate.py"])
        self.assertFalse(report["flags"]["production_ready"])
        self.assertFalse(report["flags"]["gate3_passed"])
        self.assertEqual(report["flags"]["t107_status"], "OPEN")


@unittest.skipUnless((REPO_ROOT / ".git").exists(), "需要真实仓库")
class RealRepoTests(unittest.TestCase):
    def test_real_repo_reports_untracked_license_scripts(self):
        report = cr.build_report(REPO_ROOT, cr.REQUIRED_TRACKED)
        statuses = {item["path"]: item["status"] for item in report["required"]}
        for rel in ("scripts/production/generate_license_inventory.py",
                    "scripts/production/check_license_policy.py",
                    "scripts/production/license-policy.json",
                    "scripts/production/build_license_disposition_register.py"):
            self.assertIn(statuses.get(rel), ("untracked", "tracked"), rel)
        tracked = [rel for rel in (".github/workflows/maven.yml", "scripts/check_error_codes.py",
                                   "scripts/production/check_migration_contract.py")]
        for rel in tracked:
            self.assertEqual(statuses.get(rel), "tracked", rel)


class WarningTests(TempRepoTestCase):
    """两类告警：整棵文档树未入仓、仓库根临时脚本残留。

    刻意设计成不阻断：入仓需要授权，守卫无权替人做决定；但必须被看见，
    否则文档引用的证据路径在提交后全部悬空。
    """

    def test_untracked_tree_warned_but_not_blocking(self):
        self.write("docs/enterprise-platform/01-x.md")
        report = cr.build_report(self.repo, [])
        self.assertEqual([w["status"] for w in report["warnings"]],
                         ["untracked_tree"])
        self.assertEqual(report["counts"]["untracked_trees"], 1)
        self.assertEqual(report["counts"]["warnings"], 1)
        self.assertEqual(report["decision"], "pass")

    def test_tracked_tree_not_warned(self):
        self.write("docs/enterprise-platform/01-x.md")
        git(self.repo, "add", "docs/enterprise-platform/01-x.md")
        report = cr.build_report(self.repo, [])
        self.assertEqual(report["warnings"], [])
        self.assertEqual(report["counts"]["warnings"], 0)

    def test_temp_residue_warned_but_not_blocking(self):
        self.write("_tmp_probe.py")
        report = cr.build_report(self.repo, [])
        self.assertEqual([w["status"] for w in report["warnings"]],
                         ["temp_residue"])
        self.assertEqual(report["decision"], "pass")

    def test_warning_does_not_mask_blocking(self):
        """告警不得把真阻断洗白：两者必须同时出现在同一份报告里。"""
        self.write("docs/enterprise-platform/01-x.md")
        self.write("scripts/gate.py")
        report = cr.build_report(self.repo, ["scripts/gate.py"])
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["counts"]["warnings"], 1)
        self.assertEqual(report["counts"]["required_untracked"], 1)

    def test_missing_tree_directory_is_not_warned(self):
        report = cr.build_report(self.repo, [])
        self.assertEqual(report["warnings"], [])

    def test_limits_state_warnings_are_advisory(self):
        report = cr.build_report(self.repo, [])
        self.assertIn("warnings", report)
        self.assertTrue(any("warnings 为提示项" in item for item in report["limits"]))

    def test_cli_prints_warn_line(self):
        self.write("docs/enterprise-platform/01-x.md")
        # 用一个已入仓的文件做必查项，确保退出码只反映告警而非必查缺失。
        self.write("scripts/gate.py")
        git(self.repo, "add", "scripts/gate.py")
        proc = subprocess.run(
            [sys.executable, str(PRODUCTION_DIR / "check_commit_readiness.py"),
             "--repo", str(self.repo), "--required", "scripts/gate.py"],
            capture_output=True, text=True, encoding="utf-8", errors="replace")
        self.assertEqual(proc.returncode, 0, proc.stdout + proc.stderr)
        self.assertIn("WARN", proc.stdout)
        self.assertIn("warnings=1", proc.stdout)


@unittest.skipUnless((REPO_ROOT / ".git").exists(), "需要真实仓库")
class RealRepoWarningTests(unittest.TestCase):
    def test_real_repo_warning_statuses_are_known(self):
        report = cr.build_report(REPO_ROOT, [])
        self.assertIn("warnings", report)
        for item in report["warnings"]:
            self.assertIn(item["status"], ("untracked_tree", "temp_residue"))
            self.assertTrue(item["action"])


if __name__ == "__main__":
    unittest.main()
