"""生产种子清理迁移与安装脚本的回归测试。

T-101/T-103 的目标不是让静态扫描“看起来通过”，而是保证：
  * 已部署库通过精确四元组清理历史公开种子；
  * 业务历史记录不被物理删除；
  * 演示 OAuth2/社交客户端被禁用；
  * 安装脚本不再写入任何明文客户端 Secret。
"""

import re
import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
REPO = PRODUCTION_DIR.parents[1]
sys.path.insert(0, str(PRODUCTION_DIR))

import sql_migration_lint as lint  # noqa: E402

MIGRATION = REPO / "zhicloud-server/src/main/resources/db/migration/V115__production_seed_account_cleanup.sql"
VERIFY_SQL = REPO / "zhicloud-server/src/main/resources/db/verification/V115_verify.sql"
INSTALL_SCRIPTS = [
    REPO / "sql/mysql/zhicloud_platform.sql",
    REPO / "sql/postgresql/ruoyi-vue-pro.sql",
    REPO / "sql/oracle/ruoyi-vue-pro.sql",
    REPO / "sql/sqlserver/ruoyi-vue-pro.sql",
    REPO / "sql/dm/ruoyi-vue-pro-dm8.sql",
    REPO / "sql/kingbase/ruoyi-vue-pro.sql",
    REPO / "sql/opengauss/ruoyi-vue-pro.sql",
    REPO / "sql/highgo/ruoyi-vue-pro.sql",
]

# 来自 V1.1 的精确四元组。只按用户名批量删除会误伤同名的真实账号。
EXPECTED_SEEDS = [
    (1, "admin", "$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG", 1),
    (100, "zhicloud", "$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG", 1),
    (103, "yuanma", "$2a$04$k/d6mc0nySN0i2udwcI8Ee8V5aM5OHixBRbQfXmPuFTUl3Zf/DBs.", 1),
    (104, "test", "$2a$04$BrwaYn303hjA/6TnXqdGoOLhyHOAA0bVrAFu6.1dJKycqKUnIoRz2", 1),
    (107, "admin107", "$2a$10$dYOOBKMO93v/.ReCqzyFg.o67Tqk.bbc2bhrpyBGkIw9aypCtr2pm", 118),
    (108, "admin108", "$2a$10$y6mfvKoNYL1GXWak8nYwVOH.kCWqjactkzdoIDgiKl93WN3Ejg.Lu", 119),
    (109, "admin109", "$2a$10$JAqvH0tEc0I7dfDVBI7zyuB4E3j.uH6daIjV53.vUS6PknFkDJkuK", 120),
    (110, "admin110", "$2a$10$mRMIYLDtRHlf6.9ipiqH1.Z.bh/R9dO9d5iHiGYPigi6r5KOoR2Wm", 121),
    (111, "test", "$2a$10$mRMIYLDtRHlf6.9ipiqH1.Z.bh/R9dO9d5iHiGYPigi6r5KOoR2Wm", 121),
    (112, "newobject", "$2a$04$dB0z8Q819fJWz0hbaLe6B.VfHCjYgWx6LFfET5lyz3JwcqlyCkQ4C", 1),
    (113, "aoteman", "$2a$10$0acJOIk2D25/oC87nyclE..0lzeu9DtQ/n3geP4fkun/zIVRhHJIO", 122),
    (114, "hrmgr", "$2a$10$TR4eybBioGRhBmDBWkqWLO6NIh3mzYa8KBKDDB5woiGYFVlRAi.fu", 1),
    (115, "aotemane", "$2a$04$GcyP0Vyzb2F2Yni5PuIK9ueGxM0tkZGMtDwVRwrNbtMvorzbpNsV2", 1),
    (117, "admin123", "$2a$04$sEtimsHu9YCkYY4/oqElHem2Ijc9ld20eYO6lN.g/21NfLUTDLB9W", 1),
    (118, "goudan", "$2a$04$3suGZjnA6rM5bErf38u1felbgqbsPHGdRG3l9NkxPCEt2ah9Y6aJi", 1),
    (139, "wwbwwb", "$2a$04$FJLIyg8lbPytP29pbZaiU.LesJvCsYfEaHqQfB0pGQhK3e9BeZmLy", 1),
    (141, "admin1", "$2a$04$oj6F6d7HrZ70kYVD3TNzEu.m3TPUzajOVuC66zdKna8KRerK1FmVa", 1),
    (142, "test01", "$2a$04$4bCYWZkjxxOC4QE0LY2M9uEEKWeJbLfs489NFtQoyidL5I0FndRaO", 1),
    (143, "a00001", "$2a$04$GhVHFviOw/SsTmiQtifHJesDYFlHMeGK7OWh7aGCCjGGVCmbHVAwa", 1),
    (144, "aoteman001", "$2a$04$omQOmhz8OyUFBKw77nr8KOtMp6xdvoQ1gWStjk9r8.OYT3Bv6oEYe", 1),
]


class ProductionSeedCleanupMigrationTest(unittest.TestCase):
    def test_migration_exists_and_lints_clean(self):
        self.assertTrue(MIGRATION.is_file(), f"缺少迁移: {MIGRATION}")
        report = lint.lint_paths([MIGRATION])
        self.assertEqual(report["status"], "passed", report["files"])

    def test_migration_matches_all_seed_accounts_by_exact_tuple(self):
        text = MIGRATION.read_text(encoding="utf-8")
        for user_id, username, password_hash, tenant_id in EXPECTED_SEEDS:
            with self.subTest(user_id=user_id, username=username, tenant_id=tenant_id):
                self.assertRegex(
                    text,
                    rf"\(\s*{user_id}\s*,\s*'{re.escape(username)}'\s*,\s*'{re.escape(password_hash)}'\s*,\s*{tenant_id}\s*\)",
                )

    def test_migration_revokes_relationships_and_sessions(self):
        text = MIGRATION.read_text(encoding="utf-8")
        for table in (
            "system_user_role",
            "system_user_post",
            "system_oauth2_access_token",
            "system_oauth2_refresh_token",
            "system_oauth2_approve",
            "system_oauth2_code",
            "system_social_user_bind",
        ):
            with self.subTest(table=table):
                self.assertIn(table, text)
        self.assertIn("deleted = b'1'", text)
        self.assertIn("expires_time = NOW()", text)

    def test_migration_disables_users_without_physically_deleting_them(self):
        text = MIGRATION.read_text(encoding="utf-8")
        self.assertIn("UPDATE `system_users`", text)
        self.assertIn("`status` = 1", text)
        self.assertIn("`deleted` = b'1'", text)
        self.assertNotRegex(text, r"\bDELETE\s+FROM\s+`?system_users`?\b")

    def test_migration_blocks_cleanup_without_non_seed_super_admin(self):
        text = MIGRATION.read_text(encoding="utf-8")
        self.assertIn("SIGNAL SQLSTATE '45000'", text)
        self.assertIn("non-seed super_admin", text)
        self.assertIn("r.code = 'super_admin'", text)

    def test_migration_disables_demo_clients_without_embedding_secrets(self):
        text = MIGRATION.read_text(encoding="utf-8")
        self.assertIn("system_oauth2_client", text)
        self.assertIn("system_social_client", text)
        self.assertIn("`status` = 1", text)
        # admin123 是必须精确匹配的公开种子用户名；dingvrnreaje3yqvzhxg
        # 是社交客户端标识，不是 client_secret。两者不能与真正的 Secret 混为一谈。
        for secret in ("test2", "i8E6iZyDvZj51JIb0tYsYfVQYOks9Cq1lgryEjFRqC79P3iJcrxEwT6Qk2QvLrLI"):
            with self.subTest(secret=secret):
                self.assertNotIn(secret, text)
        self.assertNotRegex(
            text,
            r"INSERT\s+(?:IGNORE\s+)?INTO\s+`?system_(?:oauth2|social)_client`?",
        )
        self.assertNotRegex(text, r"`?(?:secret|client_secret)`?\s*=\s*'")

    def test_temp_seed_table_uses_explicit_schema_collation(self):
        text = MIGRATION.read_text(encoding="utf-8")
        self.assertRegex(
            text,
            r"CREATE\s+TEMPORARY\s+TABLE\s+tmp_v115_seed_users\s*\([\s\S]*?\)\s*ENGINE\s*=\s*InnoDB\s+DEFAULT\s+CHARSET\s*=\s*utf8mb4\s+COLLATE\s*=\s*utf8mb4_unicode_ci",
        )

    def test_verify_sql_exists_and_is_read_only(self):
        self.assertTrue(VERIFY_SQL.is_file(), f"缺少核验脚本: {VERIFY_SQL}")
        text = VERIFY_SQL.read_text(encoding="utf-8")
        self.assertIn("V115", text)
        self.assertIn("FAIL", text)
        self.assertNotRegex(text, r"\b(?:UPDATE|INSERT|DELETE|ALTER|DROP)\b")


class InstallerSecretSeedTest(unittest.TestCase):
    def test_install_scripts_do_not_insert_client_secret_rows(self):
        pattern = re.compile(
            r"INSERT\s+(?:IGNORE\s+)?INTO\s+`?system_(?:oauth2|social)_client`?",
            re.IGNORECASE,
        )
        for path in INSTALL_SCRIPTS:
            with self.subTest(path=path):
                text = path.read_text(encoding="utf-8", errors="replace")
                match = pattern.search(text)
                self.assertIsNone(
                    match,
                    f"{path} 仍包含客户端 Secret 种子插入语句，首个匹配位置={match.start() if match else None}",
                )


if __name__ == "__main__":
    unittest.main()
