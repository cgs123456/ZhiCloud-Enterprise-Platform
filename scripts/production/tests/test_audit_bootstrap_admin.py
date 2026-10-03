"""bootstrap 管理员只读审计器测试。"""

import argparse
import os
import sys
import unittest
from pathlib import Path
from unittest import mock

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import audit_bootstrap_admin as auditor  # noqa: E402


def args(**overrides):
    values = dict(
        mysql_client=r"C:\tools\mysql.exe",
        host="127.0.0.1",
        port=53306,
        database="zhicloud_runner_clean_test",
        user="root",
        password_env=None,
        username="zhicloud_admin",
        timeout=60,
    )
    values.update(overrides)
    return argparse.Namespace(**values)


def passing_query(sql: str) -> str:
    if "SELECT DATABASE(), VERSION()" in sql:
        return "zhicloud_runner_clean_test\t8.4.9"
    if "SELECT COUNT(*) FROM system_users" in sql:
        return "1"
    if "SELECT id, tenant_id, status" in sql:
        return "225\t1\t0\t0\t60\t$2a$"
    if "FROM system_user_role WHERE user_id=225 AND role_id=1" in sql:
        return "1"
    if "SELECT COALESCE(GROUP_CONCAT(role_id" in sql:
        return "1"
    if "FROM system_user_post WHERE user_id=225 AND post_id=4" in sql:
        return "1"
    if "SELECT COALESCE(GROUP_CONCAT(post_id" in sql:
        return "4"
    if "FROM information_schema.statistics" in sql:
        return "uk_system_users_tenant_active_username\t0\ttenant_id,active_username"
    if "duplicate_active_usernames" in sql:
        return "0"
    raise AssertionError(f"unexpected SQL: {sql}")


class BootstrapAuditTest(unittest.TestCase):
    def test_password_is_passed_via_env_not_argv(self):
        namespace = args(password_env="ZHICLOUD_DB_PASSWORD")
        with mock.patch.dict(os.environ, {"ZHICLOUD_DB_PASSWORD": "super-secret"}, clear=False):
            command = auditor.mysql_command(namespace, "zhicloud_runner_clean_test")
            environment = auditor.mysql_env(namespace)

        self.assertNotIn("super-secret", command)
        self.assertNotIn("-p", command)
        self.assertEqual("super-secret", environment["MYSQL_PWD"])

    def test_docker_container_mode_keeps_password_out_of_argv_and_off_the_host(self):
        namespace = args(password_env="ZHICLOUD_DB_PASSWORD")
        namespace.docker_container = "zhicloud-mysql-sandbox-abcdef01"
        with mock.patch.dict(os.environ, {"ZHICLOUD_DB_PASSWORD": "super-secret"}, clear=False):
            command = auditor.mysql_command(namespace, "zhicloud_runner_clean_test")
            environment = auditor.mysql_env(namespace)

        self.assertEqual("docker", command[0])
        self.assertIn("zhicloud-mysql-sandbox-abcdef01", command)
        # -e MYSQL_PWD（不带 =）由 docker 从进程环境取值：口令既不在 argv，也不在容器命令日志里
        self.assertIn("-e", command)
        self.assertIn("MYSQL_PWD", command)
        self.assertNotIn("MYSQL_PWD=super-secret", command)
        self.assertNotIn("super-secret", command)
        # 命名空间形态里连库端点必须是容器内真实回环，而不是宿主的发布端口
        self.assertIn("--host=127.0.0.1", command)
        self.assertIn("--port=3306", command)
        self.assertNotIn(f"--port={namespace.port}", command)
        self.assertEqual("super-secret", environment["MYSQL_PWD"])

    def test_default_command_is_unchanged_without_docker_container(self):
        namespace = args(password_env="ZHICLOUD_DB_PASSWORD")
        command = auditor.mysql_command(namespace, "zhicloud_runner_clean_test")
        self.assertEqual(namespace.mysql_client, command[0])
        self.assertNotIn("docker", command)
        self.assertIn(f"--port={namespace.port}", command)

    def test_collect_evidence_passes_for_expected_admin(self):
        evidence = auditor.collect_evidence(args(), query_func=passing_query)
        self.assertEqual("passed", evidence["status"])
        self.assertEqual([], evidence["failures"])
        self.assertEqual(225, evidence["bootstrap"]["user_id"])
        self.assertEqual(1, evidence["bootstrap"]["role_id"])
        self.assertEqual(4, evidence["bootstrap"]["post_id"])
        self.assertFalse(evidence["secret_handling"]["plaintext_password_persisted"])
        self.assertEqual(["mysql"], evidence["dialect_scope"]["executed_in_this_evidence"])
        self.assertIn("postgresql", evidence["dialect_scope"]["not_executed_in_this_evidence"])

    def test_collect_evidence_fails_on_missing_role(self):
        def missing_role(sql: str) -> str:
            if "FROM system_user_role WHERE user_id=225 AND role_id=1" in sql:
                return "0"
            return passing_query(sql)

        evidence = auditor.collect_evidence(args(), query_func=missing_role)
        self.assertEqual("failed", evidence["status"])
        self.assertIn("role_binding_exact", evidence["failures"])

    def test_username_validation_rejects_sql_metacharacters(self):
        with self.assertRaises(auditor.AuditError):
            auditor.validate_username("admin'; DROP TABLE system_users;--")


if __name__ == "__main__":
    unittest.main()
