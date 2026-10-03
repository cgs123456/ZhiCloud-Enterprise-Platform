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
        # 契约基线：一个自身就自洽（存在且已跟踪）的清单。
        # 否则每个用例都会多出一条 contract_manifest_unreadable 告警，
        # 而"没有清单=无从证明"本来就该是告警，不是静默通过。
        self.seed_contract(tracked=True)

    def tearDown(self):
        self.tmp.cleanup()

    def write(self, rel: str, content: str = "x") -> Path:
        path = self.repo / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
        return path

    def seed_contract(self, *, migrations=None, tracked: bool = True) -> None:
        """写入契约清单；migrations 为 None 时使用一条自洽条目。"""
        if migrations is None:
            migrations = [{
                "version": 122,
                "file": "V900__baseline.sql",
                "sha256": "a",
                "verification_file": "V900_verify.sql",
                "verification_sha256": "b",
            }]
            for subdir, name in ((cr.MIGRATION_DIR, "V900__baseline.sql"),
                                 (cr.VERIFICATION_DIR, "V900_verify.sql")):
                self.write((subdir / name).as_posix())
        self.write(cr.MIGRATION_CONTRACT.as_posix(), json.dumps({
            "schema_version": 1, "contract_start_version": 90,
            "purpose": "fixture", "migrations": migrations,
        }))
        if tracked:
            for rel in (cr.MIGRATION_CONTRACT.as_posix(),
                        (cr.MIGRATION_DIR / "V900__baseline.sql").as_posix(),
                        (cr.VERIFICATION_DIR / "V900_verify.sql").as_posix()):
                if (self.repo / rel).is_file():
                    self.assertEqual(git(self.repo, "add", rel).returncode, 0)

    def drop_contract(self) -> None:
        (self.repo / cr.MIGRATION_CONTRACT).unlink(missing_ok=True)


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
            self.assertIn(item["status"], (
                "untracked_tree", "temp_residue",
                "contract_file_untracked", "contract_file_missing",
                "contract_manifest_unreadable",
            ))
            self.assertTrue(item["action"])

    def test_real_repo_contract_warnings_equal_independently_computed_set(self):
        """告警集合必须等于"磁盘存在但未跟踪"的契约文件全集，由磁盘与 index 独立算出。

        早先这题只做反向断言（已跟踪者不误报），一旦 V119-V121 被 git add，
        两个循环都会变空、测试永久绿——那正是本检查要推动的动作，等于自己把自己测没了。
        """
        report = cr.build_report(REPO_ROOT, [])
        contract = json.loads(
            (REPO_ROOT / cr.MIGRATION_CONTRACT).read_text(encoding="utf-8-sig"))
        tracked = cr.tracked_files(REPO_ROOT)
        expected = set()
        for entry in contract["migrations"]:
            for subdir, key in ((cr.MIGRATION_DIR, "file"),
                                (cr.VERIFICATION_DIR, "verification_file")):
                rel = (subdir / entry[key]).as_posix()
                if (REPO_ROOT / rel).is_file() and rel not in tracked:
                    expected.add(rel)
        warned = {w["path"] for w in report["warnings"]
                  if w["status"] == "contract_file_untracked"}
        self.assertEqual(expected, warned)
        missing = {w["path"] for w in report["warnings"]
                   if w["status"] == "contract_file_missing"}
        self.assertEqual(set(), missing)
        for w in report["warnings"]:
            if w["status"] == "contract_file_untracked":
                self.assertIn("C5", w["action"])


class ContractFileTrackingTests(TempRepoTestCase):
    # 用例自己的条目必须避开 setUp 基线占用的 V900 名字，否则基线已跟踪的文件
    # 会被误当成用例条目，告警数与预期不符。
    MIG = "V123__x.sql"
    VER = "V123_verify.sql"

    def write_contract(self, migration_name: str, verification_name: str) -> None:
        manifest = {
            "schema_version": 1,
            "contract_start_version": 90,
            "purpose": "fixture",
            "migrations": [{
                "version": 123,
                "file": migration_name,
                "sha256": "a",
                "verification_file": verification_name,
                "verification_sha256": "b",
            }],
        }
        self.write(cr.MIGRATION_CONTRACT.as_posix(), json.dumps(manifest))

    def test_untracked_contract_sql_warns_without_blocking(self):
        self.write_contract(self.MIG, self.VER)
        # 契约登记的 SQL 在磁盘上存在（与当前工作树的 V119-V121 同形），只是未 git 跟踪
        self.write((cr.MIGRATION_DIR / self.MIG).as_posix())
        self.write((cr.VERIFICATION_DIR / self.VER).as_posix())
        report = cr.build_report(self.repo, [])
        statuses = {w["status"] for w in report["warnings"]}
        self.assertEqual(statuses, {"contract_file_untracked"})
        self.assertEqual(report["counts"]["contract_files_untracked"], 2)
        # 契约与 SQL 必须同批或 SQL 先行，否则只提交契约会造出 C5 自败提交
        self.assertIn("C5", report["warnings"][0]["action"])
        self.assertEqual(report["decision"], "pass")

    def test_tracked_contract_sql_is_not_warned(self):
        self.write_contract(self.MIG, self.VER)
        self.write((cr.MIGRATION_DIR / self.MIG).as_posix())
        self.write((cr.VERIFICATION_DIR / self.VER).as_posix())
        for rel in (cr.MIGRATION_CONTRACT.as_posix(),
                    (cr.MIGRATION_DIR / self.MIG).as_posix(),
                    (cr.VERIFICATION_DIR / self.VER).as_posix()):
            self.assertEqual(git(self.repo, "add", rel).returncode, 0)
        report = cr.build_report(self.repo, [])
        self.assertEqual(report["counts"]["contract_files_untracked"], 0)
        self.assertEqual(report["counts"]["contract_files_missing"], 0)

    def test_contract_file_absent_on_disk_is_reported_as_missing(self):
        self.write_contract(self.MIG, self.VER)
        self.write((cr.MIGRATION_DIR / self.MIG).as_posix())
        report = cr.build_report(self.repo, [])
        missing = [w for w in report["warnings"] if w["status"] == "contract_file_missing"]
        self.assertEqual([w["path"] for w in missing],
                         [(cr.VERIFICATION_DIR / self.VER).as_posix()])

    def test_repo_without_contract_manifest_warns_instead_of_passing(self):
        """契约读不到时不得静默通过：静默 no-op 的守卫与没有守卫无法区分。"""
        self.drop_contract()
        report = cr.build_report(self.repo, [])
        self.assertEqual([w["status"] for w in report["warnings"]],
                         ["contract_manifest_unreadable"])
        self.assertEqual(report["counts"]["contract_manifest_unreadable"], 1)
        self.assertEqual(report["decision"], "pass")  # 仍是告警，不阻断

    def test_empty_migrations_array_warns(self):
        self.seed_contract(migrations=[], tracked=False)
        report = cr.build_report(self.repo, [])
        self.assertIn("contract_manifest_unreadable",
                      [w["status"] for w in report["warnings"]])

    def test_unparseable_manifest_warns_instead_of_crashing(self):
        self.write(cr.MIGRATION_CONTRACT.as_posix(), "{not json")
        report = cr.build_report(self.repo, [])
        warns = [w for w in report["warnings"] if w["status"] == "contract_manifest_unreadable"]
        self.assertEqual(1, len(warns))
        self.assertIn("解析失败", warns[0]["action"])


if __name__ == "__main__":
    unittest.main()
