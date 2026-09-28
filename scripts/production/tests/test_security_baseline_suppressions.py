"""安全基线「可审计抑制」的回归测试。

为什么需要它：扫描器把生产路径与演示/历史路径的命中混在一起统计，
demo-data 下的 160 条 R7（P1）与 V1.1 种子里的 32 条 P0 无法区分，门禁长期红。
抑制机制的目标不是"把红变绿"，而是让"生产路径 P0 = 0"成为可判定结论，
同时保证任何一条命中都不会从证据里消失。

本测试固化三条约束：
  1. fail-closed：写错的抑制文件只会让报告更红（invalid / unmatched -> failed）；
  2. 永不隐藏：被抑制项完整保留在 suppressed_findings 与 suppression 段里；
  3. 零预置：仓库内的抑制登记册必须是空数组，不允许预置任何豁免。

口径边界：本测试只证明抑制机制自身行为正确，不证明仓库已安全。
"""

import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import security_baseline_scan as sec  # noqa: E402
import security_baseline_suppressions as sb  # noqa: E402

TODAY = "2026-09-28"
FINDING_EVIDENCE = "INSERT INTO system_users (id, username, password) VALUES (1, 'admin', 'x');"
EVIDENCE_SHA = sb.finding_evidence_hash(FINDING_EVIDENCE)


def finding(file="demo-data/a.sql", rule="R7", line=9, severity="P1",
            evidence=FINDING_EVIDENCE):
    return {
        "rule": rule,
        "code": "LITERAL_USER_ACCOUNT_SEED",
        "severity": severity,
        "file": file,
        "line": line,
        "evidence": evidence,
    }


def entry(**kw):
    """一条默认合格的抑制条目；测试用 kw 破坏其中某一项来断言 fail-closed。"""
    base = {
        "file": "demo-data/a.sql",
        "rule": "R7",
        "scope": "exact",
        "line": 9,
        "evidence_sha256": EVIDENCE_SHA,
        "disposition": "accepted_risk",
        "reason": "演示数据集，不进生产镜像",
        "owner": "platform-ops",
        "approved_by": "ciso",
        "expires_on": "2026-10-01",
        "evidence": "发布流水线排除 demo-data，见 release-pipeline 配置",
    }
    base.update(kw)
    return base


def reasons_of(result):
    """把 invalid 条目里的原因码拍平成集合，便于断言。"""
    out = set()
    for bad in result["invalid"]:
        out.update(bad["reasons"])
    return out


def write_json(path: Path, data) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n",
                    encoding="utf-8", newline="\n")
    return path


class TemplateTest(unittest.TestCase):
    def test_empty_template_has_no_predefined_entry(self):
        self.assertEqual(json.loads(sb.empty_template_text()), [])

    def test_load_requires_top_level_array(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = write_json(Path(tmp) / "s.json", {"suppressions": []})
            with self.assertRaises(ValueError):
                sb.load_suppression_file(path)

    def test_load_accepts_empty_array(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = write_json(Path(tmp) / "s.json", [])
            self.assertEqual(sb.load_suppression_file(path), [])

    def test_repo_register_is_empty(self):
        """仓库内的抑制登记册必须是空数组：不允许预置任何豁免。

        这是"零预置"约束的机器可执行形式。任何人往里加条目都必须同时填
        reason / owner / approved_by / expires_on / evidence，diff 可见。
        """
        register = PRODUCTION_DIR / "security-baseline-suppressions.json"
        self.assertTrue(register.is_file(), "缺少抑制登记册文件")
        self.assertEqual(json.loads(register.read_text(encoding="utf-8")), [])


class ValidateTest(unittest.TestCase):
    def test_valid_entry_passes(self):
        got = sb.validate_entries([entry()], today=TODAY, known_rules=["R7"])
        self.assertEqual(len(got["valid"]), 1)
        self.assertEqual(got["invalid"], [])

    def test_each_required_field_is_mandatory(self):
        for field in sb.REQUIRED_FIELDS:
            with self.subTest(field=field):
                bad = entry()
                del bad[field]
                got = sb.validate_entries([bad], today=TODAY, known_rules=["R7"])
                self.assertEqual(len(got["valid"]), 0)
                self.assertIn("missing_field:" + field, reasons_of(got))

    def test_blank_string_counts_as_missing(self):
        got = sb.validate_entries([entry(reason="   ")], today=TODAY,
                                 known_rules=["R7"])
        self.assertIn("empty_field:reason", reasons_of(got))

    def test_absolute_paths_are_rejected(self):
        for raw in ("/etc/app.sql", "C:/repo/app.sql", "~/app.sql", "\\\\srv\\app.sql"):
            with self.subTest(path=raw):
                got = sb.validate_entries([entry(file=raw)], today=TODAY,
                                          known_rules=["R7"])
                self.assertIn("absolute_path", reasons_of(got))

    def test_path_traversal_is_rejected(self):
        got = sb.validate_entries([entry(file="../outside.sql")], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("path_traversal", reasons_of(got))

    def test_backslash_path_is_normalized(self):
        got = sb.validate_entries([entry(file="demo-data\\a.sql")], today=TODAY,
                                  known_rules=["R7"])
        self.assertEqual(got["valid"][0]["_file"], "demo-data/a.sql")

    def test_unknown_rule_is_rejected(self):
        got = sb.validate_entries([entry(rule="R99")], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("unknown_rule:R99", reasons_of(got))

    def test_invalid_disposition_and_scope_are_rejected(self):
        got = sb.validate_entries([entry(disposition="whatever")], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("invalid_disposition", reasons_of(got))
        got = sb.validate_entries([entry(scope="whole_repo")], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("invalid_scope", reasons_of(got))

    def test_expiry_rules(self):
        got = sb.validate_entries([entry(expires_on="2020-01-01")], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("expired", reasons_of(got))
        got = sb.validate_entries([entry(expires_on="2030-01-01")], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("expiry_too_far", reasons_of(got))
        got = sb.validate_entries([entry(expires_on="2026/10/01")], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("invalid_expires_on_format", reasons_of(got))

    def test_exact_scope_requires_line_and_digest(self):
        got = sb.validate_entries([entry(line=None)], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("missing_line", reasons_of(got))
        got = sb.validate_entries([entry(line="9")], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("invalid_line", reasons_of(got))
        got = sb.validate_entries([entry(evidence_sha256=None)], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("missing_evidence_sha256", reasons_of(got))
        got = sb.validate_entries([entry(evidence_sha256="abc")], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("invalid_evidence_sha256", reasons_of(got))

    def test_default_scope_is_the_strictest(self):
        """不写 scope 时按 exact 处理：少写一个字段不能变成整文件豁免。"""
        no_scope = entry()
        del no_scope["scope"]
        got = sb.validate_entries([no_scope], today=TODAY, known_rules=["R7"])
        self.assertEqual(got["valid"][0]["_scope"], "exact")

    def test_file_rule_scope_does_not_need_line(self):
        got = sb.validate_entries([entry(scope="file_rule", line=None,
                                         evidence_sha256=None)],
                                  today=TODAY, known_rules=["R7"])
        self.assertEqual(len(got["valid"]), 1)

    def test_non_dict_entry_is_rejected(self):
        got = sb.validate_entries(["demo-data/a.sql"], today=TODAY,
                                  known_rules=["R7"])
        self.assertIn("not_an_object", reasons_of(got))

class ApplyTest(unittest.TestCase):
    def test_exact_match_suppresses_one_finding(self):
        got = sb.apply_suppressions([finding()], [entry()], today=TODAY,
                                    known_rules=["R7"])
        self.assertEqual(got["remaining"], [])
        self.assertEqual(len(got["suppressed"]), 1)
        self.assertEqual(got["invalid"], [])
        self.assertEqual(got["unmatched"], [])

    def test_line_mismatch_is_not_suppressed_and_becomes_stale(self):
        got = sb.apply_suppressions([finding()], [entry(line=10)], today=TODAY,
                                    known_rules=["R7"])
        self.assertEqual(len(got["remaining"]), 1)
        self.assertEqual(got["suppressed"], [])
        self.assertEqual(len(got["unmatched"]), 1)

    def test_evidence_change_makes_entry_stale(self):
        """代码改了但抑制还挂着 -> unmatched，而不是继续豁免。

        这是 fail-closed 的关键：指纹对不上就失去豁免资格。
        """
        changed = finding(evidence=FINDING_EVIDENCE + " -- updated")
        got = sb.apply_suppressions([changed], [entry()], today=TODAY,
                                    known_rules=["R7"])
        self.assertEqual(len(got["remaining"]), 1)
        self.assertEqual(len(got["unmatched"]), 1)

    def test_file_rule_scope_suppresses_whole_file(self):
        findings = [finding(line=9), finding(line=20, evidence="other")]
        got = sb.apply_suppressions(findings,
                                    [entry(scope="file_rule", line=None,
                                           evidence_sha256=None)],
                                    today=TODAY, known_rules=["R7"])
        self.assertEqual(got["remaining"], [])
        self.assertEqual(len(got["suppressed"]), 2)

    def test_other_file_is_not_suppressed(self):
        got = sb.apply_suppressions([finding(file="demo-data/b.sql")], [entry()],
                                    today=TODAY, known_rules=["R7"])
        self.assertEqual(len(got["remaining"]), 1)
        self.assertEqual(len(got["unmatched"]), 1)

    def test_suppressed_findings_keep_full_evidence(self):
        """永不隐藏：被抑制项必须带着原文与出处留在报告里。"""
        raw = finding(severity="P0")
        got = sb.apply_suppressions([raw], [entry()], today=TODAY,
                                    known_rules=["R7"])
        kept = got["suppressed"][0]
        self.assertEqual(kept["file"], raw["file"])
        self.assertEqual(kept["line"], raw["line"])
        self.assertEqual(kept["evidence"], FINDING_EVIDENCE)
        self.assertEqual(kept["severity"], "P0")
        self.assertEqual(kept["suppression"]["approved_by"], "ciso")
        self.assertEqual(kept["suppression"]["expires_on"], "2026-10-01")

    def test_invalid_entry_suppresses_nothing(self):
        bad = entry()
        del bad["approved_by"]
        got = sb.apply_suppressions([finding()], [bad], today=TODAY,
                                    known_rules=["R7"])
        self.assertEqual(len(got["remaining"]), 1)
        self.assertEqual(got["suppressed"], [])
        self.assertEqual(len(got["invalid"]), 1)

    def test_no_entries_leaves_everything_blocking(self):
        got = sb.apply_suppressions([finding()], [], today=TODAY,
                                    known_rules=["R7"])
        self.assertEqual(len(got["remaining"]), 1)
        self.assertEqual(got["suppressed"], [])


class StatusTest(unittest.TestCase):
    def test_failed_when_p0_still_blocking(self):
        got = sb.apply_suppressions([finding(severity="P0"), finding(severity="P0",
                                                                     line=10)],
                                    [entry()], today=TODAY, known_rules=["R7"])
        self.assertEqual(sb.status_for({"P0": 1}, got), "failed")

    def test_failed_when_entry_invalid(self):
        bad = entry()
        del bad["owner"]
        got = sb.apply_suppressions([finding()], [bad], today=TODAY,
                                    known_rules=["R7"])
        self.assertEqual(sb.status_for({"P0": 0}, got), "failed")

    def test_failed_when_entry_stale(self):
        got = sb.apply_suppressions([finding(line=10)], [entry()], today=TODAY,
                                    known_rules=["R7"])
        self.assertEqual(sb.status_for({"P0": 0}, got), "failed")

    def test_passed_with_suppressions(self):
        got = sb.apply_suppressions([finding()], [entry()], today=TODAY,
                                    known_rules=["R7"])
        self.assertEqual(sb.status_for({"P0": 0}, got), "passed_with_suppressions")

    def test_passed_without_suppressions(self):
        got = sb.apply_suppressions([], [], today=TODAY, known_rules=["R7"])
        self.assertEqual(sb.status_for({"P0": 0}, got), "passed")

    def test_failed_controls_still_block(self):
        got = sb.apply_suppressions([finding()], [entry()], today=TODAY,
                                    known_rules=["R7"])
        self.assertEqual(sb.status_for({"P0": 0}, got, controls_failed=2), "failed")


class SectionTest(unittest.TestCase):
    def test_section_counts_and_policy(self):
        got = sb.apply_suppressions([finding()], [entry()], today=TODAY,
                                    known_rules=["R7"])
        sec_section = sb.suppression_section(got, [entry()])
        self.assertTrue(sec_section["enabled"])
        self.assertEqual(sec_section["entries"], 1)
        self.assertEqual(sec_section["applied"], 1)
        self.assertEqual(sec_section["by_severity"], {"P1": 1})
        self.assertEqual(sec_section["by_disposition"], {"accepted_risk": 1})
        self.assertTrue(sec_section["policy"]["fail_closed"])
        self.assertTrue(sec_section["policy"]["never_hidden"])
        self.assertEqual(sec_section["policy"]["predefined_entries"], 0)

    def test_section_keeps_invalid_and_unmatched_visible(self):
        got = sb.apply_suppressions([finding(line=10)],
                                    [entry(), entry(line=10, owner="")],
                                    today=TODAY, known_rules=["R7"])
        sec_section = sb.suppression_section(got, [entry(), entry()])
        self.assertTrue(sec_section["fail_closed_triggered"])
        self.assertTrue(sec_section["invalid_entries"])
        self.assertTrue(sec_section["unmatched_entries"])

    def test_internal_fields_are_not_exported(self):
        got = sb.validate_entries([entry()], today=TODAY, known_rules=["R7"])
        public = sb.suppression_entry_public(got["valid"][0])
        self.assertFalse([k for k in public if k.startswith("_")])

class ScannerIntegrationTest(unittest.TestCase):
    """抑制能力真正接进扫描器之后的端到端行为。"""

    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.addCleanup(self.tmp.cleanup)
        # deploy/helm/values.yaml 属生产路径，R1 命中即 P0。
        self.yaml = self.root / "deploy/helm/values.yaml"
        self.yaml.parent.mkdir(parents=True, exist_ok=True)
        self.yaml.write_text("url: jdbc:mysql://d/x?useSSL=false\n",
                             encoding="utf-8", newline="\n")
        self.evidence = "url: jdbc:mysql://d/x?useSSL=false"

    def _suppression_file(self, **kw):
        data = {
            "file": "deploy/helm/values.yaml",
            "rule": "R1",
            "scope": "exact",
            "line": 1,
            "evidence_sha256": sb.finding_evidence_hash(self.evidence),
            "disposition": "mitigated",
            "reason": "已在 values-prod 用 sslMode=VERIFY_IDENTITY 覆盖",
            "owner": "platform-ops",
            "approved_by": "ciso",
            "expires_on": "2026-10-01",
            "evidence": "见 deploy/helm/values-prod.yaml",
        }
        data.update(kw)
        return write_json(self.root / "sup.json", [data])

    def test_suppression_removes_finding_from_blocking_count(self):
        path = self._suppression_file()
        rep = sec.build_report(self.root, suppression_file=path)
        self.assertEqual(rep["finding_count_before_suppression"], 1)
        self.assertEqual(rep["finding_count"], 0)
        self.assertEqual(rep["findings_by_severity"], {})
        self.assertEqual(len(rep["suppressed_findings"]), 1)
        # 测试仓库没有正向控制文件，controls_failed > 0，因此仍判 failed：
        # 抑制只能消掉 finding，消不掉缺失的正向控制。
        self.assertGreater(rep["controls_failed"], 0)
        self.assertEqual(rep["status"], "failed")

    def test_status_becomes_passed_with_suppressions(self):
        path = self._suppression_file()
        with mock.patch.object(sec, "run_controls", return_value=[]):
            rep = sec.build_report(self.root, suppression_file=path)
        self.assertEqual(rep["controls_failed"], 0)
        self.assertEqual(rep["status"], "passed_with_suppressions")

    def test_stale_suppression_still_fails(self):
        """指纹/行号对不上就失去豁免，报告必须 failed。"""
        path = self._suppression_file(line=2)
        with mock.patch.object(sec, "run_controls", return_value=[]):
            rep = sec.build_report(self.root, suppression_file=path)
        self.assertEqual(rep["suppression"]["unmatched"], 1)
        self.assertEqual(rep["suppression"]["applied"], 0)
        self.assertEqual(rep["status"], "failed")
        self.assertEqual(rep["finding_count"], 1)

    def test_without_suppression_file_behaviour_is_unchanged(self):
        rep = sec.build_report(self.root)
        self.assertFalse(rep["suppression"]["enabled"])
        self.assertEqual(rep["suppression"]["entries"], 0)
        self.assertEqual(rep["findings_by_severity"].get("P0"), 1)
        self.assertEqual(rep["status"], "failed")

    def test_repo_register_does_not_change_verdict(self):
        """登记册当前是空数组，接上去不应改变任何结论。"""
        register = PRODUCTION_DIR / "security-baseline-suppressions.json"
        with mock.patch.object(sec, "run_controls", return_value=[]):
            rep = sec.build_report(self.root, suppression_file=register)
        self.assertEqual(rep["suppression"]["applied"], 0)
        self.assertEqual(rep["status"], "failed")


class CliTest(unittest.TestCase):
    def test_valid_file_without_report_passes(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = write_json(Path(tmp) / "s.json", [entry()])
            self.assertEqual(sb.main(["--check", str(path)]), 0)

    def test_invalid_file_returns_one(self):
        with tempfile.TemporaryDirectory() as tmp:
            bad = entry()
            del bad["owner"]
            path = write_json(Path(tmp) / "s.json", [bad])
            self.assertEqual(sb.main(["--check", str(path)]), 1)

    def test_stale_entry_returns_one_when_report_given(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = write_json(Path(tmp) / "s.json", [entry()])
            report = write_json(Path(tmp) / "r.json",
                                {"findings": [finding(line=10)]})
            self.assertEqual(sb.main(["--check", str(path),
                                      "--report", str(report)]), 1)

    def test_unparsable_file_returns_two(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "s.json"
            path.write_text("{not json", encoding="utf-8", newline="\n")
            self.assertEqual(sb.main(["--check", str(path)]), 2)

    def test_output_file_is_written(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = write_json(Path(tmp) / "s.json", [entry()])
            out = Path(tmp) / "out" / "sup.json"
            self.assertEqual(sb.main(["--check", str(path),
                                      "--output", str(out)]), 0)
            payload = json.loads(out.read_text(encoding="utf-8"))
            self.assertEqual(payload["suppression"]["entries"], 1)
            self.assertEqual(payload["suppression"]["stale_check"], "skipped")


if __name__ == "__main__":
    unittest.main()