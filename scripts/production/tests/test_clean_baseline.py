"""Clean baseline 生成器与产物的回归测试。

目标：
  * 不修改 V1.1 历史迁移，保证已部署库 Flyway checksum 不漂移；
  * 新库使用独立 clean baseline，移除公开账号、OAuth2/社交客户端和 demo 数据；
  * 保留所有表 DDL 与非敏感参考数据；
  * 生成器遇到目标表的多行 INSERT 必须 fail-closed。
"""

import hashlib
import json
import re
import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
REPO = PRODUCTION_DIR.parents[1]
sys.path.insert(0, str(PRODUCTION_DIR))

import generate_clean_baseline as generator  # noqa: E402

SOURCE = REPO / "zhicloud-server/src/main/resources/db/migration/V1.1__system_base_schema.sql"
CLEAN = REPO / "zhicloud-server/src/main/resources/db/clean-baseline/V1.1__system_base_schema.clean.sql"
MANIFEST = CLEAN.with_name("manifest.json")
TARGET_INSERT = re.compile(
    r"\bINSERT\s+(?:IGNORE\s+)?INTO\s+`?(?:system_users|system_oauth2_client|"
    r"system_social_client|system_user_role|system_user_post|zhicloud_demo\w*)`?",
    re.IGNORECASE,
)


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class CleanBaselineRenderTest(unittest.TestCase):
    def test_removes_only_target_insert_lines(self):
        source = (
            "-- keep this comment\n"
            "CREATE TABLE `system_users` (`id` bigint);\n"
            "INSERT IGNORE INTO `system_users` VALUES (1, 'admin');\n"
            "INSERT IGNORE INTO `system_role` VALUES (1, 'super_admin');\n"
            "INSERT IGNORE INTO `zhicloud_demo01_contact` VALUES (1);\n"
            "CREATE TABLE `zhicloud_demo01_contact` (`id` bigint);\n"
        )
        output, removed, removed_lines = generator.render_clean_baseline(source)
        self.assertNotRegex(output, TARGET_INSERT.pattern)
        self.assertIn("CREATE TABLE `system_users`", output)
        self.assertIn("CREATE TABLE `zhicloud_demo01_contact`", output)
        self.assertIn("INSERT IGNORE INTO `system_role`", output)
        self.assertEqual(removed_lines, 2)
        self.assertEqual(removed["system_users"], 1)
        self.assertEqual(removed["zhicloud_demo01_contact"], 1)

    def test_multiline_target_insert_fails_closed(self):
        source = (
            "INSERT IGNORE INTO `system_users` (`id`, `username`)\n"
            "VALUES (1, 'admin');\n"
        )
        with self.assertRaises(ValueError):
            generator.render_clean_baseline(source)


class ShippedCleanBaselineTest(unittest.TestCase):
    def test_source_and_output_are_tracked_and_hash_locked(self):
        self.assertTrue(SOURCE.is_file())
        self.assertTrue(CLEAN.is_file())
        self.assertTrue(MANIFEST.is_file())
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
        self.assertEqual(manifest["schema_version"], 1)
        self.assertEqual(manifest["source_sha256"], sha256(SOURCE))
        self.assertEqual(manifest["clean_baseline_sha256"], sha256(CLEAN))
        self.assertEqual(manifest["removed_insert_lines"], 95)

    def test_clean_baseline_has_no_target_inserts(self):
        text = CLEAN.read_text(encoding="utf-8")
        self.assertIsNone(TARGET_INSERT.search(text), "clean baseline 仍包含目标种子 INSERT")
        self.assertNotRegex(text, r"\bINSERT\s+(?:IGNORE\s+)?INTO\s+`system_users`")
        self.assertNotRegex(text, r"\bINSERT\s+(?:IGNORE\s+)?INTO\s+`system_oauth2_client`")
        self.assertNotRegex(text, r"\bINSERT\s+(?:IGNORE\s+)?INTO\s+`system_social_client`")

    def test_clean_baseline_keeps_schema_and_demo_ddl(self):
        text = CLEAN.read_text(encoding="utf-8")
        for table in (
            "system_users",
            "system_role",
            "system_user_role",
            "system_user_post",
            "system_oauth2_client",
            "system_social_client",
            "zhicloud_demo01_contact",
            "zhicloud_demo02_category",
            "zhicloud_demo03_course",
            "zhicloud_demo03_grade",
            "zhicloud_demo03_student",
        ):
            with self.subTest(table=table):
                self.assertRegex(
                    text,
                    rf"CREATE TABLE IF NOT EXISTS `{re.escape(table)}`",
                )

    def test_clean_baseline_does_not_contain_known_public_secrets(self):
        text = CLEAN.read_text(encoding="utf-8")
        for value in (
            "i8E6iZyDvZj51JIb0tYsYfVQYOks9Cq1lgryEjFRqC79P3iJcrxEwT6Qk2QvLrLI",
            "FP_bnSq_HAHKCSncmJjw5hxhnzs6vaVDSZZn3egj6rdqTQ_hu5tQVJyLMpgCakdP",
        ):
            with self.subTest(value=value[:12]):
                self.assertNotIn(value, text)


if __name__ == "__main__":
    unittest.main()
