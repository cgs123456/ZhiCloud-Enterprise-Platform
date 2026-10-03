# -*- coding: utf-8 -*-
# generate_license_inventory.py 回归测试 (T-107 Maven 许可证清单可复现性)。
#
# 契约目标:
# 1. 由 CycloneDX BOM + runtime dependency tree 在干净 checkout 上复现治理清单。
#    此前该步骤由未入库的一次性脚本完成, CI 第 4 步无法复现。
# 2. 与已审核基线一致: 862/36/826, 类别桶一致, 门禁 deny/review purl 集合一致。
# 3. 带 classifier 的 6 字段坐标必须被识别 (基线漏统计 12 个 netty 构件)。
# 4. 未知/缺失许可证归一为 UNMAPPED / MISSING, 由门禁 fail-closed 处理。
# 5. 输入缺失或非 CycloneDX 一律 exit=2, 不得被误判为通过。
#
# 本文件不宣称任何许可证已获法务审批, 也不构成法律意见。

import json
import tempfile
import unittest
from datetime import date
from pathlib import Path
import sys

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
REPO_ROOT = PRODUCTION_DIR.parent.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import generate_license_inventory as gen  # noqa: E402
import check_license_policy as gate  # noqa: E402

EVIDENCE = REPO_ROOT / "docs" / "enterprise-platform" / "evidence" / "production"
BASELINE = EVIDENCE / "maven-license-inventory-20260928_0028.json"
REAL_BOM = EVIDENCE / "bom-maven-aggregate-20260928_0028.json"
REAL_TREE = EVIDENCE / "dependency-tree-server-runtime-after-jcip-cleanroom-20260928_0028.log"
REAL_POLICY = PRODUCTION_DIR / "license-policy.json"
TODAY = date(2026, 9, 28)

# 基线运行时集合遗漏的 netty classifier 构件 (全部声明 Apache-2.0 / permissive_standard)。
# 这是修复 6 字段坐标解析后相对基线 20260928_0028 的唯一差异, 不改变门禁结论。
NETTY_CLASSIFIER_DELTA = [
    "pkg:maven/io.netty/netty-transport-native-epoll@4.2.15.Final?classifier=linux-x86_64&type=jar",
    "pkg:maven/io.netty/netty-transport-native-epoll@4.2.15.Final?classifier=linux-aarch_64&type=jar",
    "pkg:maven/io.netty/netty-transport-native-epoll@4.2.15.Final?classifier=linux-riscv64&type=jar",
    "pkg:maven/io.netty/netty-transport-native-kqueue@4.2.15.Final?classifier=osx-x86_64&type=jar",
    "pkg:maven/io.netty/netty-transport-native-kqueue@4.2.15.Final?classifier=osx-aarch_64&type=jar",
    "pkg:maven/io.netty/netty-resolver-dns-native-macos@4.2.15.Final?classifier=osx-x86_64&type=jar",
    "pkg:maven/io.netty/netty-resolver-dns-native-macos@4.2.15.Final?classifier=osx-aarch_64&type=jar",
    "pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=linux-x86_64&type=jar",
    "pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=linux-aarch_64&type=jar",
    "pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=osx-x86_64&type=jar",
    "pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=osx-aarch_64&type=jar",
    "pkg:maven/io.netty/netty-tcnative-boringssl-static@2.0.77.Final?classifier=windows-x86_64&type=jar",
]


def make_component(group, artifact, version, licenses=None, classifier=None):
    if classifier:
        purl = "pkg:maven/{}/{}@{}?classifier={}&type=jar".format(group, artifact, version, classifier)
    else:
        purl = "pkg:maven/{}/{}@{}?type=jar".format(group, artifact, version)
    return {
        "type": "library",
        "group": group,
        "name": artifact,
        "version": version,
        "purl": purl,
        "licenses": [] if licenses is None else licenses,
    }


def lic_id(value):
    return {"license": {"id": value}}


def lic_name(value):
    return {"license": {"name": value}}


def lic_expression(value):
    return {"expression": value}


def tree_line(coord):
    return "[INFO] |  +- " + coord


def write_inputs(tmp, components, tree_lines, bom_text=None):
    bom_path = Path(tmp) / "bom.json"
    tree_path = Path(tmp) / "tree.log"
    if bom_text is None:
        bom_text = json.dumps({"bomFormat": "CycloneDX", "specVersion": "1.6", "components": components})
    bom_path.write_text(bom_text, encoding="utf-8")
    tree_path.write_text("\n".join(tree_lines) + "\n", encoding="utf-8")
    return bom_path, tree_path


def generate(components, tree_lines, **kwargs):
    with tempfile.TemporaryDirectory() as tmp:
        bom_path, tree_path = write_inputs(tmp, components, tree_lines)
        return gen.build_report(bom_path, tree_path, **kwargs)


def run_gate(inventory):
    with tempfile.TemporaryDirectory() as tmp:
        path = Path(tmp) / "inventory.json"
        path.write_text(json.dumps(inventory, ensure_ascii=False), encoding="utf-8")
        return gate.build_report(path, REAL_POLICY, TODAY)


def category_of(report, purl):
    for item in report["components"]:
        if item["purl"] == purl:
            return item
    raise AssertionError("component not found: " + purl)


@unittest.skipUnless(REAL_BOM.is_file() and REAL_TREE.is_file() and BASELINE.is_file(),
                     "需要基线 BOM / runtime tree / inventory 证据文件")
class RealBaselineRoundTrip(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.baseline = json.loads(BASELINE.read_text(encoding="utf-8"))
        cls.regenerated = gen.build_report(REAL_BOM, REAL_TREE)

    def test_totals_match_baseline(self):
        summary = self.regenerated["summary"]
        base_summary = self.baseline["summary"]
        self.assertEqual(summary["total_components"], base_summary["total_components"])
        self.assertEqual(summary["total_components"], 862)
        self.assertEqual(summary["internal_components"], 36)
        self.assertEqual(summary["external_components"], 826)
        self.assertEqual(summary["external_missing_license"], 0)
        self.assertEqual(summary["internal_missing_license"], 0)

    def test_category_buckets_match_baseline(self):
        summary = self.regenerated["summary"]
        base_summary = self.baseline["summary"]
        for key in ("category_counts_all", "category_counts_external"):
            self.assertEqual(summary[key], base_summary[key], key)

    def test_review_required_matches_baseline(self):
        self.assertEqual(self.regenerated["summary"]["review_required_external_components"], 14)

    def test_runtime_delta_is_exactly_known_netty_classifier_set(self):
        base_runtime = {c["purl"] for c in self.baseline["components"] if c["server_runtime_scope"]}
        new_runtime = {c["purl"] for c in self.regenerated["components"] if c["server_runtime_scope"]}
        self.assertEqual(base_runtime - new_runtime, set())
        self.assertEqual(new_runtime - base_runtime, set(NETTY_CLASSIFIER_DELTA))
        self.assertEqual(self.regenerated["summary"]["server_runtime_scope_components"], 719)

    def test_newly_detected_runtime_components_are_permissive(self):
        for purl in NETTY_CLASSIFIER_DELTA:
            item = category_of(self.regenerated, purl)
            self.assertTrue(item["server_runtime_scope"], purl)
            self.assertEqual(item["license_category"], "permissive_standard", purl)
            self.assertEqual(item["normalized_licenses"], ["Apache-2.0"], purl)

    def test_gate_outcome_identical_to_baseline(self):
        base_gate = run_gate(self.baseline)
        new_gate = run_gate(self.regenerated)
        self.assertEqual(base_gate["decision"], "fail")
        self.assertEqual(new_gate["decision"], "fail")
        self.assertEqual(base_gate["counts"], new_gate["counts"])
        self.assertEqual({x["purl"] for x in base_gate["denied"]},
                         {x["purl"] for x in new_gate["denied"]})
        self.assertEqual({x["purl"] for x in base_gate["review_required"]},
                         {x["purl"] for x in new_gate["review_required"]})
        self.assertEqual(len(new_gate["denied"]), 13)
        self.assertEqual(len(new_gate["review_required"]), 38)
        self.assertEqual(new_gate["approved_allowlist_hits"], [])

    def test_flags_stay_unresolved(self):
        new_gate = run_gate(self.regenerated)
        self.assertFalse(new_gate["flags"]["production_ready"])
        self.assertFalse(new_gate["flags"]["gate3_passed"])
        self.assertEqual(new_gate["flags"]["t107_status"], "OPEN")
        self.assertFalse(new_gate["flags"]["independent_review"])

    def test_source_hashes_recorded(self):
        source = self.regenerated["source"]
        self.assertTrue(source["bom_sha256"])
        self.assertTrue(source["runtime_tree_sha256"])
        self.assertEqual(self.regenerated["evidence_type"], "maven_license_inventory")
        self.assertEqual(self.regenerated["schema_version"], 1)


class NormalisationRules(unittest.TestCase):
    def test_external_missing_license_is_missing_category(self):
        comp = make_component("com.example", "thing", "1.0")
        report = generate([comp], [tree_line("com.example:thing:jar:1.0:runtime")])
        item = category_of(report, comp["purl"])
        self.assertEqual(item["normalized_licenses"], ["MISSING"])
        self.assertEqual(item["license_category"], "missing")
        self.assertEqual(report["summary"]["external_missing_license"], 1)
        self.assertEqual(report["external_missing_license"], [comp["purl"]])

    def test_internal_missing_license_recorded_separately(self):
        comp = make_component("cn.zhicloud", "core", "1.0")
        report = generate([comp], [])
        item = category_of(report, comp["purl"])
        self.assertEqual(item["license_category"], "missing")
        self.assertEqual(report["summary"]["internal_missing_license"], 1)
        self.assertEqual(report["internal_missing_license"], [comp["purl"]])
        self.assertEqual(report["summary"]["external_missing_license"], 0)

    def test_unknown_label_becomes_unmapped_and_gate_denies(self):
        comp = make_component("com.example", "thing", "1.0", [lic_name("FooCorp Corporate License")])
        report = generate([comp], [tree_line("com.example:thing:jar:1.0:runtime")])
        item = category_of(report, comp["purl"])
        self.assertEqual(item["normalized_licenses"], ["UNMAPPED:FooCorp Corporate License"])
        self.assertEqual(item["license_category"], "unmapped")
        gate_report = run_gate(report)
        self.assertEqual(gate_report["decision"], "fail")
        self.assertIn(comp["purl"], [x["purl"] for x in gate_report["denied"]])

    def test_expression_label_is_dual_category(self):
        comp = make_component("javax.annotation", "javax.annotation-api", "1.3.2",
                              [lic_expression("(CDDL-1.0 OR GPL-2.0-with-classpath-exception)")])
        report = generate([comp], [tree_line("javax.annotation:javax.annotation-api:jar:1.3.2:runtime")])
        item = category_of(report, comp["purl"])
        self.assertEqual(item["license_category"], "expression_or_dual")
        self.assertEqual(item["normalized_licenses"],
                         ["expression:CDDL-1.0 OR GPL-2.0-with-classpath-exception"])

    def test_precedence_strong_beats_weak_and_permissive(self):
        comp = make_component("com.example", "multi", "1.0",
                              [lic_id("Apache-2.0"), lic_id("EPL-2.0"), lic_name("UnRar License")])
        report = generate([comp], [])
        self.assertEqual(category_of(report, comp["purl"])["license_category"], "strong_or_restrictive")

    def test_precedence_expression_beats_strong(self):
        comp = make_component("com.example", "multi", "1.0",
                              [lic_name("UnRar License"),
                               lic_expression("(CDDL-1.0 OR GPL-2.0-with-classpath-exception)")])
        report = generate([comp], [])
        self.assertEqual(category_of(report, comp["purl"])["license_category"], "expression_or_dual")

    def test_precedence_weak_beats_permissive(self):
        comp = make_component("com.example", "multi", "1.0",
                              [lic_id("Apache-2.0"), lic_id("EPL-2.0")])
        report = generate([comp], [])
        self.assertEqual(category_of(report, comp["purl"])["license_category"], "weak_copyleft")

    def test_duplicate_labels_are_deduplicated(self):
        comp = make_component("com.example", "dup", "1.0", [lic_id("Apache-2.0"), lic_id("Apache-2.0")])
        report = generate([comp], [])
        self.assertEqual(category_of(report, comp["purl"])["normalized_licenses"], ["Apache-2.0"])


class RuntimeCoordinateParsing(unittest.TestCase):
    def test_five_field_coordinate(self):
        coords = gen.parse_runtime_coordinates("[INFO] +- com.foo:bar:jar:1.2.3:runtime\n")
        self.assertEqual(coords, {("com.foo", "bar", "1.2.3")})

    def test_six_field_coordinate_with_classifier(self):
        coords = gen.parse_runtime_coordinates(
            "[INFO] |  +- io.netty:netty-transport-native-epoll:jar:linux-x86_64:4.2.15.Final:runtime\n")
        self.assertEqual(coords, {("io.netty", "netty-transport-native-epoll", "4.2.15.Final")})

    def test_compile_scope_still_parsed_but_not_filtered_here(self):
        # 解析层只负责取坐标; 是否 runtime 取决于 dependency:tree 命令的 scope 参数。
        coords = gen.parse_runtime_coordinates("[INFO] +- com.foo:bar:jar:1.0:compile\n")
        self.assertEqual(coords, {("com.foo", "bar", "1.0")})

    def test_non_coordinate_lines_ignored(self):
        text = "\n".join([
            "[INFO] --- maven-dependency-plugin:3.6.1:tree (default-cli) @ zhicloud-server ---",
            "[INFO] Scanning for projects...",
            "[WARNING] Some problem",
            "",
        ])
        self.assertEqual(gen.parse_runtime_coordinates(text), set())

    def test_classifier_component_is_marked_runtime(self):
        comp = make_component("io.netty", "netty-transport-native-epoll", "4.2.15.Final",
                              [lic_id("Apache-2.0")], classifier="linux-x86_64")
        report = generate([comp], [
            tree_line("io.netty:netty-transport-native-epoll:jar:linux-x86_64:4.2.15.Final:runtime")])
        self.assertTrue(category_of(report, comp["purl"])["server_runtime_scope"])
        self.assertEqual(report["summary"]["server_runtime_scope_components"], 1)

    def test_component_absent_from_tree_is_not_runtime(self):
        comp = make_component("com.example", "test-only", "1.0", [lic_id("Apache-2.0")])
        report = generate([comp], [])
        self.assertFalse(category_of(report, comp["purl"])["server_runtime_scope"])
        self.assertEqual(report["summary"]["server_runtime_scope_components"], 0)


class InternalPrefixOverride(unittest.TestCase):
    def test_default_prefix_classifies_zhicloud_as_internal(self):
        comp = make_component("cn.zhicloud", "module", "1.0", [lic_id("MIT")])
        report = generate([comp], [])
        self.assertTrue(category_of(report, comp["purl"])["is_internal"])
        self.assertEqual(report["summary"]["internal_components"], 1)

    def test_override_prefix(self):
        comp = make_component("com.acme", "module", "1.0", [lic_id("MIT")])
        report = generate([comp], [], internal_prefix="com.acme")
        self.assertTrue(category_of(report, comp["purl"])["is_internal"])

    def test_prefix_is_case_insensitive(self):
        comp = make_component("CN.ZhiCloud", "module", "1.0", [lic_id("MIT")])
        report = generate([comp], [])
        self.assertTrue(category_of(report, comp["purl"])["is_internal"])


class CliContract(unittest.TestCase):
    def test_main_writes_output_and_change_note(self):
        comp = make_component("com.example", "thing", "1.0", [lic_id("Apache-2.0")])
        with tempfile.TemporaryDirectory() as tmp:
            bom_path, tree_path = write_inputs(tmp, [comp],
                                               [tree_line("com.example:thing:jar:1.0:runtime")])
            out = Path(tmp) / "nested" / "inventory.json"
            code = gen.main(["--bom", str(bom_path), "--runtime-tree", str(tree_path),
                             "--output", str(out), "--change-note", "unit test note"])
            self.assertEqual(code, 0)
            self.assertTrue(out.is_file())
            data = json.loads(out.read_text(encoding="utf-8"))
            self.assertEqual(data["summary"]["change_note"], "unit test note")
            self.assertEqual(data["source"]["bom_sha256"], gen.sha256_file(bom_path))
            self.assertEqual(data["method"]["disclaimer"],
                             "This is engineering evidence for governance, not a legal opinion.")

    def test_missing_bom_returns_exit_2(self):
        with tempfile.TemporaryDirectory() as tmp:
            tree_path = Path(tmp) / "tree.log"
            tree_path.write_text("", encoding="utf-8")
            code = gen.main(["--bom", str(Path(tmp) / "nope.json"), "--runtime-tree", str(tree_path),
                             "--output", str(Path(tmp) / "out.json")])
            self.assertEqual(code, 2)

    def test_missing_tree_returns_exit_2(self):
        with tempfile.TemporaryDirectory() as tmp:
            bom_path, _ = write_inputs(tmp, [], [])
            code = gen.main(["--bom", str(bom_path), "--runtime-tree", str(Path(tmp) / "nope.log"),
                             "--output", str(Path(tmp) / "out.json")])
            self.assertEqual(code, 2)

    def test_non_cyclonedx_bom_returns_exit_2(self):
        comp = make_component("com.example", "thing", "1.0", [lic_id("Apache-2.0")])
        with tempfile.TemporaryDirectory() as tmp:
            bom_path, tree_path = write_inputs(tmp, [], [], bom_text=json.dumps({"components": [comp]}))
            code = gen.main(["--bom", str(bom_path), "--runtime-tree", str(tree_path),
                             "--output", str(Path(tmp) / "out.json")])
            self.assertEqual(code, 2)

    def test_malformed_bom_returns_exit_2(self):
        with tempfile.TemporaryDirectory() as tmp:
            bom_path, tree_path = write_inputs(tmp, [], [], bom_text="{not json")
            code = gen.main(["--bom", str(bom_path), "--runtime-tree", str(tree_path),
                             "--output", str(Path(tmp) / "out.json")])
            self.assertEqual(code, 2)


if __name__ == "__main__":
    unittest.main()