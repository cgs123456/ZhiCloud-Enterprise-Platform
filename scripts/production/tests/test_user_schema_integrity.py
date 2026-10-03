"""用户账号 schema 生产完整性回归测试。

覆盖两类必须在实库前固化的缺陷：
1. MySQL `system_users` 缺少活跃账号数据库唯一约束，应用层 COUNT 校验无法防并发重复；
2. 非 MySQL 全新安装脚本缺少 TOTP 列，而 AdminUserDO 已映射这两个字段。

这些测试只证明静态契约，不证明目标数据库已执行成功。
"""

import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
MIGRATION = REPO / "zhicloud-server/src/main/resources/db/migration/V116__system_users_active_username_unique.sql"
VERIFY = REPO / "zhicloud-server/src/main/resources/db/verification/V116_verify.sql"

NON_MYSQL_INSTALLS = (
    "sql/postgresql/ruoyi-vue-pro.sql",
    "sql/highgo/ruoyi-vue-pro.sql",
    "sql/kingbase/ruoyi-vue-pro.sql",
    "sql/opengauss/ruoyi-vue-pro.sql",
    "sql/oracle/ruoyi-vue-pro.sql",
    "sql/sqlserver/ruoyi-vue-pro.sql",
    "sql/dm/ruoyi-vue-pro-dm8.sql",
)


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8-sig")


def system_users_block(text: str) -> str:
    match = re.search(
        r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?system_users`?\s*\((.*?)\)\s*(?:;|GO\b)",
        text,
        re.IGNORECASE | re.DOTALL,
    )
    if not match:
        raise AssertionError("system_users CREATE TABLE block not found")
    return match.group(1)


class V116MigrationContractTest(unittest.TestCase):
    def test_migration_declares_generated_active_username(self):
        text = read(MIGRATION)
        self.assertRegex(
            text,
            r"`?active_username`?\s+varchar\(30\)[\s\S]*?"
            r"GENERATED\s+ALWAYS\s+AS\s*\(\s*CASE\s+WHEN\s+`?deleted`?\s*=\s*0\s+"
            r"THEN\s+`?username`?\s+ELSE\s+NULL\s+END\s*\)\s+STORED",
        )
        self.assertIn("casewhendeleted=0thenusernameelsenullend", text)
        self.assertIn("character_maximum_length = 30", text)

    def test_migration_fails_closed_on_existing_active_duplicates(self):
        text = read(MIGRATION)
        self.assertRegex(text, r"GROUP\s+BY\s+tenant_id\s*,\s*username")
        self.assertRegex(text, r"HAVING\s+COUNT\s*\(\s*\*\s*\)\s*>\s*1")
        self.assertRegex(text, r"SIGNAL\s+SQLSTATE\s+'45000'")

    def test_migration_adds_tenant_scoped_unique_index_idempotently(self):
        text = read(MIGRATION)
        self.assertIn("uk_system_users_tenant_active_username", text)
        self.assertRegex(
            text,
            r"UNIQUE\s+KEY\s+`?uk_system_users_tenant_active_username`?\s*"
            r"\(\s*`?tenant_id`?\s*,\s*`?active_username`?\s*\)",
        )
        self.assertIn("information_schema.columns", text)
        self.assertIn("information_schema.statistics", text)
        self.assertIn("sub_part IS NULL", text)
        self.assertIn("UPPER(index_type) = 'BTREE'", text)
        self.assertRegex(text, r"DELIMITER\s+\$\$")
        self.assertRegex(text, r"DELIMITER\s+;")

    def test_verification_sql_checks_the_constraint(self):
        text = read(VERIFY)
        self.assertIn("V116_ACTIVE_USERNAME_COLUMN", text)
        self.assertIn("V116_ACTIVE_USERNAME_UNIQUE_INDEX", text)
        self.assertIn("casewhendeleted=0thenusernameelsenullend", text)
        self.assertIn("character_maximum_length = 30", text)
        self.assertIn("WRONG_PREFIX", text)
        self.assertIn("WRONG_INDEX_TYPE", text)
        self.assertIn("uk_system_users_tenant_active_username", text)
        self.assertIn("active_username", text)


class InstallScriptMfaSchemaTest(unittest.TestCase):
    def test_all_install_scripts_define_totp_columns(self):
        paths = [REPO / "sql/mysql/zhicloud_platform.sql"]
        paths.extend(REPO / path for path in NON_MYSQL_INSTALLS)
        for path in paths:
            with self.subTest(path=str(path)):
                block = system_users_block(read(path))
                self.assertIn("totp_secret", block.lower())
                self.assertIn("totp_enabled", block.lower())


if __name__ == "__main__":
    unittest.main()
