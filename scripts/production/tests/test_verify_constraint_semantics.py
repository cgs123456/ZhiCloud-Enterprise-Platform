"""V116/V117 constraint-semantics verifier unit tests.

These tests define the safety boundary before the integration verifier exists:
sandbox-only database names, exact MySQL duplicate classification, and
non-production evidence invariants.
"""

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import verify_constraint_semantics as verifier  # noqa: E402


class SandboxGuardTest(unittest.TestCase):
    def test_accepts_only_sandbox_database_names(self):
        verifier.assert_sandbox_database("zhicloud_sandbox_constraint_semantics_01")
        with self.assertRaises(ValueError):
            verifier.assert_sandbox_database("zhicloud_platform")
        with self.assertRaises(ValueError):
            verifier.assert_sandbox_database("zhicloud_sandbox-bad")
        with self.assertRaises(ValueError):
            verifier.assert_sandbox_database("zhicloud_sandbox`; DROP DATABASE mysql; --")


class MysqlErrorTest(unittest.TestCase):
    def test_extracts_duplicate_key_error_code(self):
        stderr = (
            "ERROR 1062 (23000) at line 2: Duplicate entry 'race-user' "
            "for key 'system_users.uk_system_users_tenant_active_username'"
        )
        self.assertEqual(verifier.parse_mysql_error_code(stderr), 1062)

    def test_missing_error_code_is_none(self):
        self.assertIsNone(verifier.parse_mysql_error_code("query completed"))


class ConcurrentClassificationTest(unittest.TestCase):
    def test_requires_one_success_and_one_duplicate_error(self):
        result = verifier.classify_concurrent_results([
            {"exit_code": 0, "mysql_error_code": None},
            {"exit_code": 1, "mysql_error_code": 1062},
        ])
        self.assertTrue(result["passed"])
        self.assertEqual(result["success_count"], 1)
        self.assertEqual(result["duplicate_key_count"], 1)

    def test_rejects_two_successes(self):
        result = verifier.classify_concurrent_results([
            {"exit_code": 0, "mysql_error_code": None},
            {"exit_code": 0, "mysql_error_code": None},
        ])
        self.assertFalse(result["passed"])
        self.assertEqual(result["success_count"], 2)

    def test_rejects_non_duplicate_failure(self):
        result = verifier.classify_concurrent_results([
            {"exit_code": 0, "mysql_error_code": None},
            {"exit_code": 1, "mysql_error_code": 1205},
        ])
        self.assertFalse(result["passed"])
        self.assertEqual(result["duplicate_key_count"], 0)
        self.assertEqual(result["other_failure_count"], 1)


class EvidenceTest(unittest.TestCase):
    def test_evidence_never_claims_production_readiness(self):
        evidence = verifier.build_evidence(
            server={"version": "8.4.9"},
            artifacts={"v116_sha256": "a" * 64},
            scenarios=[{"id": "v116_concurrent", "passed": True}],
            caveats=["synthetic volume only"],
        )
        self.assertEqual(evidence["status"], "passed")
        self.assertFalse(evidence["production_ready"])
        self.assertFalse(evidence["gate3_passed"])
        self.assertEqual(evidence["r011_status"], "OPEN")
        self.assertEqual(evidence["r029_status"], "OPEN")
        self.assertFalse(evidence["independent_review"])
        self.assertIn("FAKE-SIMULATED-DATA", evidence["disclaimer"])
        self.assertEqual(evidence["caveats"], ["synthetic volume only"])

    def test_any_failed_scenario_fails_evidence(self):
        evidence = verifier.build_evidence(
            server={"version": "8.4.9"},
            artifacts={},
            scenarios=[
                {"id": "ok", "passed": True},
                {"id": "bad", "passed": False},
            ],
        )
        self.assertEqual(evidence["status"], "failed")
        self.assertEqual(evidence["summary"], {"total": 2, "passed": 1, "failed": 1})

    def test_empty_scenarios_fail_closed(self):
        evidence = verifier.build_evidence(server={}, artifacts={}, scenarios=[])
        self.assertEqual(evidence["status"], "failed")

class SqlBuilderTest(unittest.TestCase):
    def test_v116_concurrent_sql_uses_same_username_and_commit(self):
        sql = verifier.build_v116_concurrent_sql("race-user", 2)
        self.assertIn("START TRANSACTION", sql)
        self.assertIn("'race-user'", sql)
        self.assertIn("SELECT SLEEP(2)", sql)
        self.assertIn("COMMIT", sql)

    def test_v117_concurrent_sql_uses_same_idempotency_key_and_commit(self):
        sql = verifier.build_v117_concurrent_sql("race-key", 2)
        self.assertIn("START TRANSACTION", sql)
        self.assertIn("'race-key'", sql)
        self.assertIn("SELECT SLEEP(2)", sql)
        self.assertIn("COMMIT", sql)

    def test_ddl_seed_sql_respects_requested_row_count(self):
        sql = verifier.build_ddl_seed_sql("system_users", 1000)
        self.assertIn("system_users", sql)
        self.assertIn("cte_max_recursion_depth", sql)
        self.assertIn("n < 1000", sql)

    def test_analyze_table_sql_targets_supported_table(self):
        sql = verifier.build_analyze_table_sql("system_users")
        self.assertIn("ANALYZE TABLE `system_users`", sql)
        with self.assertRaises(ValueError):
            verifier.build_analyze_table_sql("mysql")
if __name__ == "__main__":
    unittest.main()