"""安全基线静态扫描器的回归测试（Phase 1 / P0-2）。

先 RED 后 GREEN 的证据（2026-09-25 首扫）：
  * 全仓 378 个配置文件里命中 200 条，其中 P0 121 条；
  * 命中项集中在 JDBC 关 TLS、helm 空密钥、SQL 预置账号三类；
  * 本测试固化「规则能命中 / 严重度可分级 / 正向控制可判缺失 / 结论带免责」。

口径边界：本测试只证明**扫描器自身行为正确**，不证明仓库已安全。
"""

import sys
import tempfile
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import security_baseline_scan as sec  # noqa: E402


def write(root: Path, rel: str, text: str) -> Path:
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")
    return path


def codes(report, rule_id):
    return [f for f in report["findings"] if f["rule"] == rule_id]


class RuleHitTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def _scan(self, rel, text):
        write(self.root, rel, text)
        return sec.scan_repo(self.root)

    def test_r1_jdbc_tls_disabled(self):
        rep = self._scan("deploy/helm/values.yaml",
                         "url: jdbc:mysql://db:3306/x?useSSL=false\n")
        self.assertEqual(len(codes(rep, "R1")), 1)
        self.assertEqual(codes(rep, "R1")[0]["code"], "JDBC_TLS_DISABLED")

    def test_r2_public_key_retrieve(self):
        rep = self._scan("k8s/zhicloud-server.yaml",
                         "url: jdbc:mysql://db:3306/x?allowPublicKeyRetrieval=true\n")
        self.assertEqual(len(codes(rep, "R2")), 1)

    def test_r3_default_credential(self):
        rep = self._scan("application.yaml",
                         "password: ${MYSQL_PASSWORD:zhicloud123}\n")
        self.assertEqual(len(codes(rep, "R3")), 1)

    def test_r3_required_placeholder_is_not_a_default_credential(self):
        """${ENV:?required} 是「缺失即失败」的必填占位，不算默认口令。"""
        rep = self._scan("application-prod.yaml",
                         "    password: ${MYBATIS_ENCRYPTOR_PASSWORD:?required}\n")
        self.assertEqual(codes(rep, "R3"), [])

    def test_r3_env_fallback_chain_is_not_a_default_credential(self):
        """${A:${B}} 是变量回退链，默认值本身不是硬编码口令。"""
        rep = self._scan("application-prod.yaml",
                         "password: ${SLAVE_DATASOURCE_PASSWORD:${MASTER_DATASOURCE_PASSWORD}}\n")
        self.assertEqual(codes(rep, "R3"), [])

    def test_r4_placeholder_secret(self):
        rep = self._scan("deploy/helm/values-prod.yaml", "jwtSecret: CHANGE_ME\n")
        self.assertEqual(len(codes(rep, "R4")), 1)

    def test_r5_empty_secret_value(self):
        rep = self._scan("deploy/helm/values.yaml", 'MASTER_DATASOURCE_PASSWORD: ""\n')
        self.assertEqual(len(codes(rep, "R5")), 1)

    def test_r6_literal_weak_secret(self):
        rep = self._scan("application-dev.yaml", "      client-secret: xx\n")
        self.assertEqual(len(codes(rep, "R6")), 1)

    def test_r7_test_account_seed(self):
        rep = self._scan("sql/mysql/zhicloud_platform.sql",
                         "INSERT INTO `system_users` (`id`, `username`) VALUES (1, 'admin');\n")
        self.assertEqual(len(codes(rep, "R7")), 1)

    def test_r8_demo_mode_enabled(self):
        rep = self._scan("application.yaml", "  demo: true\n")
        self.assertEqual(len(codes(rep, "R8")), 1)

    def test_clean_file_has_no_finding(self):
        rep = self._scan("deploy/helm/values.yaml",
                         "url: jdbc:mysql://db:3306/x?useSSL=true\n"
                         "MASTER_DATASOURCE_PASSWORD: ${DB_PASSWORD:?required}\n")
        self.assertEqual(rep["findings"], [])

    def test_unscanned_suffix_is_ignored(self):
        write(self.root, "notes.txt", "password: ${X:weak}\n")
        rep = sec.scan_repo(self.root)
        self.assertEqual(rep["findings"], [])


class SeverityTest(unittest.TestCase):
    """严重度分级：生产路径上的凭据类命中必须是 P0。"""

    def test_prod_path_credential_is_p0(self):
        self.assertEqual(sec.severity_for("R1", "deploy/helm/values.yaml"), "P0")
        self.assertEqual(sec.severity_for("R5", "deploy/helm/values.yaml"), "P0")
        self.assertEqual(sec.severity_for("R6", "k8s/zhicloud-server.yaml"), "P0")

    def test_sql_install_path_is_p0(self):
        self.assertEqual(sec.severity_for("R7", "sql/mysql/zhicloud_platform.sql"), "P0")
        self.assertEqual(sec.severity_for("R7", "sql/oracle/other.sql"), "P0")

    def test_non_prod_path_is_downgraded(self):
        self.assertEqual(sec.severity_for("R1", "application-dev.yaml"), "P1")
        self.assertEqual(sec.severity_for("R4", "deploy/helm/values-prod.yaml"), "P1")
        self.assertEqual(sec.severity_for("R8", "application-dev.yaml"), "P2")

    def test_non_sql_test_account_is_p1(self):
        self.assertEqual(sec.severity_for("R7", "docs/seed.sql"), "P1")


class ControlTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def _controls(self):
        return {c["control"]: c for c in sec.run_controls(self.root)}

    def test_passing_controls(self):
        write(self.root, "application-prod.yaml",
              "springdoc:\n  api-docs:\n    enabled: false\n"
              "  swagger-ui:\n    enabled: false\n"
              "knife4j:\n  enable: false\n"
              "mybatis-plus:\n  encryptor:\n"
              "    password: ${MYBATIS_ENCRYPTOR_PASSWORD:?required}\n")
        write(self.root, "deploy/helm/templates/_helpers.tpl",
              "{{- define \"zhicloud.requiredSecret\" -}}\n")
        write(self.root, "deploy/helm/templates/secret.yaml",
              "{{ include \"zhicloud.requiredSecret\" . }}\n")
        got = self._controls()
        self.assertEqual(got["C1"]["status"], "passed")
        self.assertEqual(got["C2"]["status"], "passed")
        self.assertEqual(got["C3"]["status"], "passed")
        self.assertEqual(got["C4"]["status"], "passed")

    def test_missing_file_is_reported(self):
        got = self._controls()
        self.assertEqual({c["status"] for c in got.values()}, {"file_missing"})

    def test_partial_pattern_is_failed(self):
        write(self.root, "application-prod.yaml", "springdoc:\n  api-docs:\n    enabled: false\n")
        got = self._controls()
        self.assertEqual(got["C1"]["status"], "failed")
        self.assertTrue(got["C1"]["missing_patterns"])


class ReportTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def test_p0_finding_makes_status_failed(self):
        write(self.root, "deploy/helm/values.yaml", "url: jdbc:mysql://d/x?useSSL=false\n")
        rep = sec.build_report(self.root)
        self.assertEqual(rep["status"], "failed")
        self.assertEqual(rep["findings_by_severity"].get("P0"), 1)
        self.assertEqual(rep["finding_count"], 1)

    def test_clean_repo_passes(self):
        """零命中 + 四个正向控制齐备，才允许 status=passed。"""
        write(self.root, "application.yaml", "demo: false\n")
        write(self.root, "application-prod.yaml",
              "springdoc:\n  api-docs:\n    enabled: false\n"
              "  swagger-ui:\n    enabled: false\n"
              "knife4j:\n  enable: false\n"
              "    password: ${MYBATIS_ENCRYPTOR_PASSWORD:?required}\n")
        write(self.root, "deploy/helm/templates/_helpers.tpl",
              "{{- define \"zhicloud.requiredSecret\" -}}\n")
        write(self.root, "deploy/helm/templates/secret.yaml",
              "{{ include \"zhicloud.requiredSecret\" . }}\n")
        rep = sec.build_report(self.root)
        self.assertEqual(rep["controls_failed"], 0)
        self.assertEqual(rep["status"], "passed")
        self.assertEqual(rep["finding_count"], 0)

    def test_report_carries_disclaimers(self):
        rep = sec.build_report(self.root)
        self.assertGreaterEqual(len(rep["disclaimers"]), 3)
        self.assertIn("静态文本扫描", rep["disclaimers"][0])
        self.assertIn("CVE", rep["disclaimers"][1])
        self.assertIn("不等于安全基线已达标", rep["disclaimers"][2])

    def test_report_records_scanned_files_and_rules(self):
        write(self.root, "application.yaml", "demo: false\n")
        rep = sec.build_report(self.root)
        self.assertEqual(rep["scanned_files"], 1)
        self.assertEqual(len(rep["rules"]), 8)
        self.assertEqual(rep["rules"]["R1"], "JDBC_TLS_DISABLED")

    def test_main_writes_output_and_returns_failed(self):
        write(self.root, "deploy/helm/values.yaml", "url: jdbc:mysql://d/x?useSSL=false\n")
        out = self.root / "out" / "security-baseline.json"
        code = sec.main(["--repo", str(self.root), "--output", str(out), "--summary"])
        self.assertEqual(code, 1)
        self.assertTrue(out.is_file())


if __name__ == "__main__":
    unittest.main()
