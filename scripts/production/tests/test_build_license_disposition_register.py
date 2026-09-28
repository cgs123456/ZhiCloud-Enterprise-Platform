# -*- coding: utf-8 -*-
# build_license_disposition_register.py 回归测试 (T-107 许可证处置台账)。
#
# 契约目标:
# 1. 台账必须覆盖门禁 denied + review_required 全部条目, 且默认为 PENDING (未处置 = 未解除门禁)。
# 2. 台账本身不产生任何放行效果: 不得写入 allowlist, 不得改变 policy。
# 3. 重扫时必须按 purl 继承上一版人工填写内容 (法务审批不能被重跑冲掉),
#    且上一版被阻断、本版不再出现的条目必须显式列出供复核, 不静默丢弃。
# 4. 输入缺失或结构非法必须 exit=2, 不能被误判为"已处置"。
#
# 本文件不宣称任何许可证已获法务审批, 也不构成法律意见。

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

import build_license_disposition_register as reg  # noqa: E402

REAL_GATE = (
    REPO_ROOT / "docs" / "enterprise-platform" / "evidence" / "production"
    / "license-policy-gate-20260928_0107.json"
)


def record(purl, group, artifact, version, category="weak_copyleft",
           action="review_and_record", runtime=True, label="EPL-2.0", rule="LIC-EXT-001"):
    return {
        "purl": purl,
        "group": group,
        "artifact": artifact,
        "version": version,
        "license_category": category,
        "policy_action": action,
        "rule": rule,
        "server_runtime_scope": runtime,
        "raw_license_label": label,
    }


def gate_payload(denied, review):
    return {
        "decision": "fail",
        "counts": {"components": 862, "denied": len(denied), "review_required": len(review)},
        "denied": denied,
        "review_required": review,
    }


class BuildItemsTests(unittest.TestCase):
    def test_covers_denied_and_review_with_pending_defaults(self):
        gate = gate_payload(
            [record("pkg:maven/com.mysql/mysql-connector-j@9.7.0?type=jar", "com.mysql",
                    "mysql-connector-j", "9.7.0", category="strong_or_restrictive",
                    action="deny_until_approved")],
            [record("pkg:maven/org.aspectj/aspectjweaver@1.9.25.1?type=jar", "org.aspectj",
                    "aspectjweaver", "1.9.25.1")],
        )
        items = reg.build_items(gate)
        self.assertEqual(len(items), 2)
        for item in items:
            self.assertEqual(item["disposition"], "PENDING")
            for field in ("approved_by", "approved_on", "expires_on", "scope"):
                self.assertEqual(item[field], "", field)

    def test_denied_sorted_before_review(self):
        gate = gate_payload(
            [record("pkg:maven/a/b@1?type=jar", "a", "b", "1", action="deny_until_approved")],
            [record("pkg:maven/c/d@1?type=jar", "c", "d", "1")],
        )
        items = reg.build_items(gate)
        self.assertEqual([i["policy_action"] for i in items],
                         ["deny_until_approved", "review_and_record"])

    def test_missing_arrays_rejected(self):
        with self.assertRaises(reg.InputError):
            reg.build_items({"decision": "fail", "denied": []})


class AutoNotesTests(unittest.TestCase):
    def test_weak_copyleft_blocking_strength_hint(self):
        item = reg.normalise_item(record("pkg:maven/x/y@1?type=jar", "x", "y", "1"), "review_and_record")
        self.assertIn("阻断强度待裁定", item["notes"])

    def test_pure_gpl_classification_dispute_hint(self):
        item = reg.normalise_item(
            record("pkg:maven/x/y@1?type=jar", "x", "y", "1", label="GPL-2.0-only"), "review_and_record")
        self.assertIn("分类争议", item["notes"])

    def test_mulan_unmapped_hint(self):
        item = reg.normalise_item(
            record("pkg:maven/x/y@1?type=jar", "x", "y", "1", label="MulanPSL-2.0"), "review_and_record")
        self.assertIn("MulanPSL-2.0", item["notes"])

    def test_netty_native_hint(self):
        item = reg.normalise_item(
            record("pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?type=jar",
                   "io.netty", "netty-tcnative-boringssl-static", "2.0.77.Final"), "review_and_record")
        self.assertIn("BoringSSL", item["notes"])

    def test_non_runtime_removal_hint(self):
        item = reg.normalise_item(
            record("pkg:maven/x/y@1?type=jar", "x", "y", "1", runtime=False), "review_and_record")
        self.assertIn("不在服务端运行时闭包内", item["notes"])


class MergePreviousTests(unittest.TestCase):
    def test_manual_fields_inherited_by_purl(self):
        items = reg.build_items(gate_payload(
            [], [record("pkg:maven/org.aspectj/aspectjweaver@1.9.25.1?type=jar", "org.aspectj",
                        "aspectjweaver", "1.9.25.1")]))
        previous = {"items": [{
            "purl": "pkg:maven/org.aspectj/aspectjweaver@1.9.25.1?type=jar",
            "disposition": "approve",
            "approved_by": "legal@example.com",
            "approved_on": "2026-09-30",
            "expires_on": "2027-09-30",
            "scope": "runtime only",
            "notes": "法务口头确认",
        }]}
        resolved, inherited = reg.merge_previous(items, previous)
        self.assertEqual(resolved, [])
        self.assertGreaterEqual(inherited, 5)
        self.assertEqual(items[0]["disposition"], "approve")
        self.assertEqual(items[0]["approved_by"], "legal@example.com")
        self.assertEqual(items[0]["expires_on"], "2027-09-30")

    def test_pending_previous_does_not_override_fresh_hints(self):
        items = reg.build_items(gate_payload(
            [], [record("pkg:maven/x/y@1?type=jar", "x", "y", "1", runtime=False)]))
        previous = {"items": [{"purl": "pkg:maven/x/y@1?type=jar", "disposition": "PENDING"}]}
        reg.merge_previous(items, previous)
        self.assertEqual(items[0]["disposition"], "PENDING")
        self.assertIn("不在服务端运行时闭包内", items[0]["notes"])

    def test_disappeared_items_reported_not_dropped(self):
        items = reg.build_items(gate_payload([], [record("pkg:maven/keep/keep@1?type=jar", "keep", "keep", "1")]))
        previous = {"items": [
            {"purl": "pkg:maven/keep/keep@1?type=jar", "disposition": "approve"},
            {"purl": "pkg:maven/gone/gone@1?type=jar", "disposition": "approve"},
        ]}
        resolved, _ = reg.merge_previous(items, previous)
        self.assertEqual(resolved, ["pkg:maven/gone/gone@1?type=jar"])


class PayloadAndRenderTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.dir = Path(self.tmp.name)
        self.gate_path = self.dir / "gate.json"
        self.gate_path.write_text(json.dumps(gate_payload(
            [record("pkg:maven/com.mysql/mysql-connector-j@9.7.0?type=jar", "com.mysql",
                    "mysql-connector-j", "9.7.0", category="strong_or_restrictive",
                    action="deny_until_approved")],
            [
                record("pkg:maven/org.junit.jupiter/junit-jupiter@5.12.2?type=jar",
                       "org.junit.jupiter", "junit-jupiter", "5.12.2", runtime=False),
                record("pkg:maven/org.aspectj/aspectjweaver@1.9.25.1?type=jar",
                       "org.aspectj", "aspectjweaver", "1.9.25.1"),
            ],
        ), ensure_ascii=False), encoding="utf-8")

    def tearDown(self):
        self.tmp.cleanup()

    def test_counts_include_runtime_split(self):
        payload = reg.build_payload(self.gate_path, json.loads(self.gate_path.read_text(encoding="utf-8")), None, None)
        counts = payload["counts"]
        self.assertEqual(counts["total"], 3)
        self.assertEqual(counts["denied"], 1)
        self.assertEqual(counts["review_required"], 2)
        self.assertEqual(counts["in_server_runtime"], 2)
        self.assertEqual(counts["not_in_server_runtime"], 1)
        self.assertEqual(counts["dispositions"], {"PENDING": 3})

    def test_flags_remain_open(self):
        payload = reg.build_payload(self.gate_path, json.loads(self.gate_path.read_text(encoding="utf-8")), None, None)
        self.assertFalse(payload["flags"]["production_ready"])
        self.assertFalse(payload["flags"]["gate3_passed"])
        self.assertEqual(payload["flags"]["t107_status"], "OPEN")
        self.assertFalse(payload["flags"]["independent_review"])

    def test_markdown_has_tables_and_no_allowlist_promise(self):
        payload = reg.build_payload(self.gate_path, json.loads(self.gate_path.read_text(encoding="utf-8")), None, None)
        md = reg.render_markdown(payload)
        self.assertIn("deny_until_approved", md)
        self.assertIn("review_and_record", md)
        self.assertIn("待法务裁定", md)
        self.assertIn("禁止事项", md)
        # 台账不得宣称任何项已获批
        self.assertNotIn("已批准", md.replace("未经批准禁止引入", ""))


class CliTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.dir = Path(self.tmp.name)
        self.gate = self.dir / "gate.json"
        self.gate.write_text(json.dumps(gate_payload(
            [record("pkg:maven/a/b@1?type=jar", "a", "b", "1", action="deny_until_approved")], []),
            ensure_ascii=False), encoding="utf-8")

    def tearDown(self):
        self.tmp.cleanup()

    def _run(self, args):
        return subprocess.run(
            [sys.executable, str(PRODUCTION_DIR / "build_license_disposition_register.py"), *args],
            capture_output=True, text=True, encoding="utf-8", errors="replace")

    def test_writes_md_and_json(self):
        md = self.dir / "register.md"
        js = self.dir / "register.json"
        proc = self._run(["--gate", str(self.gate), "--output-md", str(md), "--output-json", str(js)])
        self.assertEqual(proc.returncode, 0, proc.stderr)
        self.assertTrue(md.is_file())
        self.assertTrue(js.is_file())
        data = json.loads(js.read_text(encoding="utf-8"))
        self.assertEqual(data["counts"]["total"], 1)
        self.assertEqual(data["items"][0]["disposition"], "PENDING")

    def test_missing_gate_exits_two(self):
        proc = self._run(["--gate", str(self.dir / "nope.json")])
        self.assertEqual(proc.returncode, 2)

    def test_malformed_gate_exits_two(self):
        bad = self.dir / "bad.json"
        bad.write_text("{not json", encoding="utf-8")
        proc = self._run(["--gate", str(bad)])
        self.assertEqual(proc.returncode, 2)


@unittest.skipUnless(REAL_GATE.is_file(), "需要已审核的基线门禁 JSON")
class RealBaselineTests(unittest.TestCase):
    def test_real_gate_yields_51_pending_items(self):
        gate = json.loads(REAL_GATE.read_text(encoding="utf-8"))
        items = reg.build_items(gate)
        self.assertEqual(len(items), 51)
        self.assertEqual(sum(1 for i in items if i["server_runtime_scope"]), 38)
        self.assertTrue(all(i["disposition"] == "PENDING" for i in items))
        self.assertEqual(gate["counts"]["denied"], 13)
        self.assertEqual(gate["counts"]["review_required"], 38)


if __name__ == "__main__":
    unittest.main()
