"""Maven 许可证 CI 门禁（check_license_policy.py）回归测试。

契约目标：
1. 默认 fail-closed —— 未获法务审批的 deny/review 组件必须让门禁失败（exit=1）。
2. internal（cn.zhicloud）必须带 MIT 元数据，否则 deny。
3. allowlist 只有在 decision 为审批通过、未过期、且含 approved_on 时才放行。
4. 输入缺失/非法必须 exit=2，不能被误判为"通过"。
"""

import json
import sys
import tempfile
import unittest
from datetime import date
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
REPO_ROOT = PRODUCTION_DIR.parent.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import check_license_policy as gate  # noqa: E402

REAL_INVENTORY = (
    REPO_ROOT / "docs" / "enterprise-platform" / "evidence" / "production"
    / "maven-license-inventory-20260928_0028.json"
)
REAL_POLICY = PRODUCTION_DIR / "license-policy.json"
TODAY = date(2026, 9, 28)


def component(purl, group, artifact, version, category, label=None,
              internal=False, runtime=False, licenses=None):
    return {
        "purl": purl,
        "group": group,
        "artifact": artifact,
        "version": version,
        "type": "jar",
        "is_internal": internal,
        "server_runtime_scope": runtime,
        "raw_license_label": label or category,
        "normalized_licenses": licenses if licenses is not None else [category.upper()],
        "license_category": category,
    }


def inventory(components):
    return {
        "schema_version": 1,
        "evidence_type": "maven_license_inventory",
        "generated_at_local": "2026-09-28T00:28:56+08:00",
        "source": {},
        "method": "test-fixture",
        "summary": {},
        "external_missing_license": [],
        "internal_missing_license": [],
        "components": components,
    }


def policy(allowlist=None, category_actions=None):
    return {
        "schema_version": 1,
        "evidence_type": "maven_license_policy",
        "internal": {"group_prefix": "cn.zhicloud", "required_licenses": ["MIT"], "action": "allow"},
        "category_actions": category_actions or {
            "permissive_standard": "allow",
            "weak_copyleft": "review_and_record",
            "strong_or_restrictive": "deny_until_approved",
            "custom_or_non_spdx": "deny_until_approved",
            "expression_or_dual": "review",
            "missing": "deny",
            "unmapped": "deny",
        },
        "unmapped_category_action": "deny",
        "maintenance": {"max_entry_validity_days": 365},
        "allowlist": allowlist or [],
    }


def approval(purl, **overrides):
    entry = {
        "purl": purl,
        "decision": "approved",
        "justification": "法务审批（测试夹具）",
        "approved_by": "legal@example.com",
        "approved_on": "2026-09-01",
        "expires_on": "2027-09-01",
        "scope": "all",
    }
    entry.update(overrides)
    return entry


class CheckLicensePolicyTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.inventory_path = self.root / "inventory.json"
        self.policy_path = self.root / "policy.json"
        self.output_path = self.root / "decision.json"

    def tearDown(self):
        self.tmp.cleanup()

    def write_inventory(self, components):
        self.inventory_path.write_text(
            json.dumps(inventory(components), ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8", newline="\n",
        )
        return self.inventory_path

    def write_policy(self, allowlist=None, category_actions=None):
        self.policy_path.write_text(
            json.dumps(policy(allowlist, category_actions), ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8", newline="\n",
        )
        return self.policy_path

    def report(self):
        return gate.build_report(self.inventory_path, self.policy_path, TODAY)

    def run_cli(self):
        return gate.main([
            "--inventory", str(self.inventory_path),
            "--policy", str(self.policy_path),
            "--output", str(self.output_path),
        ])

    # ---------- 放行路径 ----------

    def test_internal_mit_and_permissive_pass(self):
        self.write_inventory([
            component("pkg:maven/cn.zhicloud/demo@1.0", "cn.zhicloud", "demo", "1.0",
                      "permissive_standard", "MIT", internal=True, licenses=["MIT"]),
            component("pkg:maven/org.slf4j/slf4j-api@2.0.9", "org.slf4j", "slf4j-api",
                      "2.0.9", "permissive_standard", "MIT", runtime=True),
        ])
        self.write_policy()
        report = self.report()
        self.assertEqual(report["decision"], "allow", report["denied"] + report["review_required"])
        self.assertEqual(report["counts"]["allowed"], 2)
        self.assertEqual(self.run_cli(), 0)

    def test_approved_allowlist_unblocks_strong_copyleft(self):
        purl = "pkg:maven/com.mysql/mysql-connector-j@9.7.0?type=jar"
        self.write_inventory([
            component(purl, "com.mysql", "mysql-connector-j", "9.7.0",
                      "strong_or_restrictive", "GPL-2.0 with Universal FOSS Exception", runtime=True),
        ])
        self.write_policy([approval(purl)])
        report = self.report()
        self.assertEqual(report["decision"], "allow")
        self.assertEqual(report["counts"]["allowlist_hits"], 1)
        self.assertEqual(report["approved_allowlist_hits"][0]["rule"], "LIC-EXT-000")
        self.assertEqual(self.run_cli(), 0)

    def test_allowlist_matches_group_artifact_version(self):
        self.write_inventory([
            component("pkg:maven/org.jdom/jdom2@2.0.6.1?type=jar", "org.jdom", "jdom2",
                      "2.0.6.1", "strong_or_restrictive", "Apache-like custom text", runtime=True),
        ])
        self.write_policy([{
            "group": "org.jdom", "artifact": "jdom2", "version": "2.0.6.1",
            "decision": "approved_with_exception", "approved_by": "legal@example.com",
            "approved_on": "2026-09-01", "expires_on": "2027-09-01",
        }])
        self.assertEqual(self.report()["decision"], "allow")

    # ---------- 阻断路径 ----------

    def test_internal_without_mit_is_denied(self):
        self.write_inventory([
            component("pkg:maven/cn.zhicloud/demo@1.0", "cn.zhicloud", "demo", "1.0",
                      "permissive_standard", "Apache-2.0", internal=True, licenses=["Apache-2.0"]),
        ])
        self.write_policy()
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-INT-001")
        self.assertEqual(self.run_cli(), 1)

    def test_internal_without_license_metadata_is_denied(self):
        self.write_inventory([
            component("pkg:maven/cn.zhicloud/demo@1.0", "cn.zhicloud", "demo", "1.0",
                      "permissive_standard", "", internal=True, licenses=[]),
        ])
        self.write_policy()
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertIn("LIC-INT-001", [item["rule"] for item in report["denied"]])

    def test_missing_license_is_denied(self):
        self.write_inventory([
            component("pkg:maven/com.example/unknown@1.0", "com.example", "unknown",
                      "1.0", "missing", "MISSING", licenses=[]),
        ])
        self.write_policy()
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-EXT-002")

    def test_strong_copyleft_without_approval_blocks(self):
        self.write_inventory([
            component("pkg:maven/com.mysql/mysql-connector-j@9.7.0?type=jar", "com.mysql",
                      "mysql-connector-j", "9.7.0", "strong_or_restrictive",
                      "GPL-2.0 with Universal FOSS Exception", runtime=True),
        ])
        self.write_policy()
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-EXT-001")
        self.assertEqual(report["counts"]["runtime_blocked"], 1)
        self.assertEqual(self.run_cli(), 1)

    def test_weak_copyleft_without_record_blocks(self):
        self.write_inventory([
            component("pkg:maven/ch.qos.logback/logback-classic@1.5.34?type=jar", "ch.qos.logback",
                      "logback-classic", "1.5.34", "weak_copyleft", "EPL-2.0", runtime=True),
        ])
        self.write_policy()
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["review_required"][0]["policy_action"], "review_and_record")
        self.assertEqual(self.run_cli(), 1)

    def test_unknown_category_is_denied(self):
        self.write_inventory([
            component("pkg:maven/com.example/weird@1.0", "com.example", "weird",
                      "1.0", "some_new_category", "Weird License"),
        ])
        self.write_policy()
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-EXT-006")

    def test_unapproved_allowlist_decision_still_blocks(self):
        purl = "pkg:maven/com.github.junrar/junrar@7.6.0?type=jar"
        self.write_inventory([
            component(purl, "com.github.junrar", "junrar", "7.6.0",
                      "strong_or_restrictive", "UnRar License", runtime=True),
        ])
        self.write_policy([approval(purl, decision="pending")])
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-EXT-003")

    def test_expired_allowlist_blocks(self):
        purl = "pkg:maven/org.bouncycastle/bcprov-jdk18on@1.80?type=jar"
        self.write_inventory([
            component(purl, "org.bouncycastle", "bcprov-jdk18on", "1.80",
                      "custom_or_non_spdx", "Bouncy Castle Licence", runtime=True),
        ])
        self.write_policy([approval(purl, approved_on="2025-01-01", expires_on="2025-12-31")])
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-EXT-004")

    def test_allowlist_without_approved_on_blocks(self):
        purl = "pkg:maven/org.jdom/jdom2@2.0.6.1?type=jar"
        self.write_inventory([
            component(purl, "org.jdom", "jdom2", "2.0.6.1",
                      "strong_or_restrictive", "Apache-like custom text", runtime=True),
        ])
        self.write_policy([approval(purl, approved_on=None)])
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-EXT-005")

    def test_stale_allowlist_beyond_validity_blocks(self):
        purl = "pkg:maven/com.mysql/mysql-connector-j@9.7.0?type=jar"
        self.write_inventory([
            component(purl, "com.mysql", "mysql-connector-j", "9.7.0",
                      "strong_or_restrictive", "GPL-2.0", runtime=True),
        ])
        self.write_policy([approval(purl, approved_on="2024-01-01", expires_on="2030-01-01")])
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-EXT-005")

    def test_isolated_decision_blocks_runtime_component(self):
        purl = "pkg:maven/com.github.junrar/junrar@7.6.0?type=jar"
        self.write_inventory([
            component(purl, "com.github.junrar", "junrar", "7.6.0",
                      "strong_or_restrictive", "UnRar License", runtime=True),
        ])
        self.write_policy([approval(purl, decision="isolated")])
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-EXT-007")

    def test_invalid_component_entry_blocks(self):
        self.write_inventory(["not-an-object"])
        self.write_policy()
        report = self.report()
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["denied"][0]["rule"], "LIC-INV-001")

    # ---------- 输入错误 ----------

    def test_missing_policy_returns_exit_two(self):
        self.write_inventory([
            component("pkg:maven/org.slf4j/slf4j-api@2.0.9", "org.slf4j", "slf4j-api",
                      "2.0.9", "permissive_standard", "MIT"),
        ])
        self.assertEqual(
            gate.main(["--inventory", str(self.inventory_path),
                       "--policy", str(self.root / "nope.json")]),
            2,
        )

    def test_missing_inventory_returns_exit_two(self):
        self.write_policy()
        self.assertEqual(
            gate.main(["--inventory", str(self.root / "nope.json"),
                       "--policy", str(self.policy_path)]),
            2,
        )

    def test_wrong_evidence_type_returns_exit_two(self):
        self.inventory_path.write_text(
            json.dumps({"schema_version": 1, "evidence_type": "something_else", "components": []}),
            encoding="utf-8", newline="\n",
        )
        self.write_policy()
        with self.assertRaises(gate.InputError):
            self.report()

    def test_malformed_allowlist_date_returns_exit_two(self):
        purl = "pkg:maven/com.mysql/mysql-connector-j@9.7.0?type=jar"
        self.write_inventory([
            component(purl, "com.mysql", "mysql-connector-j", "9.7.0",
                      "strong_or_restrictive", "GPL-2.0", runtime=True),
        ])
        self.write_policy([approval(purl, approved_on="2026/09/01")])
        with self.assertRaises(gate.InputError):
            self.report()

    # ---------- 语义标志与输出 ----------

    def test_report_carries_fail_closed_flags_and_hashes(self):
        self.write_inventory([
            component("pkg:maven/org.slf4j/slf4j-api@2.0.9", "org.slf4j", "slf4j-api",
                      "2.0.9", "permissive_standard", "MIT"),
        ])
        self.write_policy()
        report = self.report()
        self.assertFalse(report["flags"]["production_ready"])
        self.assertFalse(report["flags"]["gate3_passed"])
        self.assertEqual(report["flags"]["t107_status"], "OPEN")
        self.assertFalse(report["flags"]["independent_review"])
        self.assertEqual(len(report["input"]["inventory_sha256"]), 64)
        self.assertEqual(len(report["input"]["policy_sha256"]), 64)

    def test_output_file_is_written(self):
        self.write_inventory([
            component("pkg:maven/org.slf4j/slf4j-api@2.0.9", "org.slf4j", "slf4j-api",
                      "2.0.9", "permissive_standard", "MIT"),
        ])
        self.write_policy()
        self.assertEqual(self.run_cli(), 0)
        written = json.loads(self.output_path.read_text(encoding="utf-8"))
        self.assertEqual(written["decision"], "allow")

    # ---------- 真实证据门禁（fail-closed 基线） ----------

    @unittest.skipUnless(REAL_INVENTORY.is_file(), "真实 license inventory 证据缺失")
    @unittest.skipUnless(REAL_POLICY.is_file(), "license-policy.json 缺失")
    def test_real_inventory_fails_closed_without_legal_approval(self):
        report = gate.build_report(REAL_INVENTORY, REAL_POLICY, TODAY)
        self.assertEqual(report["decision"], "fail")
        self.assertEqual(report["counts"]["components"], 862)
        self.assertEqual(report["counts"]["internal"], 36)
        self.assertEqual(report["counts"]["external"], 826)
        # 13 = 9 strong_or_restrictive + 4 custom_or_non_spdx（设计文档列出的 14 项含 1 项 dual）
        self.assertEqual(report["counts"]["denied"], 13)
        # 38 = 37 weak_copyleft + 1 expression_or_dual
        self.assertEqual(report["counts"]["review_required"], 38)
        self.assertEqual(report["counts"]["allowlist_hits"], 0)
        self.assertEqual(
            gate.main(["--inventory", str(REAL_INVENTORY), "--policy", str(REAL_POLICY),
                       "--output", str(self.output_path)]),
            1,
        )


    # ---------- --record-only：所有者决定降为非阻断，但判定与数组必须原样留下 ----------

    def deny_fixture(self):
        self.write_inventory([
            component("pkg:maven/com.mysql/mysql-connector-j@9.7.0", "com.mysql",
                      "mysql-connector-j", "9.7.0", "strong_or_restrictive", "GPL-2.0"),
            component("pkg:maven/org.slf4j/slf4j-api@2.0.9", "org.slf4j", "slf4j-api",
                      "2.0.9", "permissive_standard", "MIT"),
        ])
        self.write_policy()

    def run_cli_with(self, extra):
        return gate.main([
            "--inventory", str(self.inventory_path),
            "--policy", str(self.policy_path),
            "--output", str(self.output_path),
        ] + extra)

    def test_record_only_exits_zero_but_keeps_the_denied_array(self):
        self.deny_fixture()
        self.assertEqual(self.run_cli_with(["--record-only"]), 0)
        written = json.loads(self.output_path.read_text(encoding="utf-8"))
        # 只记录 ≠ 判定被改写：decision 仍是 fail，被拒组件仍可复核
        self.assertEqual(written["decision"], "fail")
        self.assertFalse(written["blocking"])
        self.assertEqual(written["counts"]["denied"], 1)
        self.assertEqual(len(written["denied"]), 1)
        self.assertEqual(written["denied"][0]["purl"], "pkg:maven/com.mysql/mysql-connector-j@9.7.0")

    def test_same_fixture_without_record_only_still_blocks(self):
        # 变异对照：同一个夹具去掉 --record-only 必须回落到 exit=1，
        # 否则"只记录"就等价于把门禁永久摘掉，而不是一次可撤销的声明。
        self.deny_fixture()
        self.assertEqual(self.run_cli_with([]), 1)
        written = json.loads(self.output_path.read_text(encoding="utf-8"))
        self.assertTrue(written["blocking"])

    def test_record_only_does_not_swallow_gate_errors(self):
        # 输入坏了（exit=2）不能被 --record-only 伪装成"记录成功"
        self.deny_fixture()
        self.policy_path.unlink()
        self.assertEqual(self.run_cli_with(["--record-only"]), 2)

if __name__ == "__main__":
    unittest.main()
