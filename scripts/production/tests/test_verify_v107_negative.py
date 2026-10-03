"""V107 真机负向验证脚本的单元测试。

脚本的职责不是证明 V107 一定通过，而是让“重复数据/错误索引必须阻断迁移”
这一行为可重复执行、可机读，并且任何输出都必须带禁止生产导入标识。
"""

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import verify_v107_negative as verifier  # noqa: E402


class V107NegativeVerifierTest(unittest.TestCase):
    def test_accepts_only_safe_sandbox_database(self):
        verifier.assert_sandbox_database("zhicloud_sandbox_v107_negative")
        with self.assertRaises(ValueError):
            verifier.assert_sandbox_database("zhicloud_prod")
        with self.assertRaises(ValueError):
            verifier.assert_sandbox_database("zhicloud_sandbox-bad")
        with self.assertRaises(ValueError):
            verifier.assert_sandbox_database("zhicloud_sandbox`; DROP DATABASE mysql; --")

    def test_expected_error_requires_exact_migration_signal(self):
        stderr = (
            "ERROR 1644 (45000) at line 126: "
            "V107 duplicate rows block unique index uk_event_tenant_event on event_outbox"
        )
        self.assertTrue(verifier.contains_expected_error(
            stderr,
            "V107 duplicate rows block unique index "
            "uk_event_tenant_event on event_outbox",
        ))
        self.assertFalse(verifier.contains_expected_error(
            stderr,
            "V107 duplicate rows block unique index "
            "uk_event_tenant_event on event_inbox",
        ))

    def test_evidence_always_marks_simulated_data_as_non_production(self):
        evidence = verifier.build_evidence(
            server={"version": "8.4.9"},
            artifacts={"migration_sha256": "a" * 64},
            scenarios=[{"id": "demo", "passed": True}],
        )
        self.assertEqual(evidence["status"], "passed")
        self.assertFalse(evidence["production_import_allowed"])
        self.assertIn("FAKE-SIMULATED-DATA", evidence["disclaimer"])
        self.assertEqual(evidence["summary"], {"total": 1, "passed": 1, "failed": 0})


if __name__ == "__main__":
    unittest.main()
