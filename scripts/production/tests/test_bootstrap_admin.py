"""生产初始管理员 bootstrap 模板与渲染器的回归测试。

这些测试只验证静态契约和渲染安全，不替代 MySQL/PostgreSQL/Oracle/DM8/
SQL Server 的真实数据库语法与执行验证。真实执行边界必须记录在部署证据中。
"""

import argparse
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
REPO = PRODUCTION_DIR.parents[1]
sys.path.insert(0, str(PRODUCTION_DIR))

import init_bootstrap_admin as bootstrap  # noqa: E402

HASH = "$2a$12$" + ("a" * 53)
SEED_PASSWORD_HASHES = (
    "$2a$04$.vd8nPeLwxt6hnSzmAoAyul8BOLX7Cib6QhcxRe30rfvrIPQHH1OG",
    "$2a$10$dYOOBKMO93v/.ReCqzyFg.o67Tqk.bbc2bhrpyBGkIw9aypCtr2pm",
)
ALL_DIALECTS = sorted(bootstrap.TEMPLATES)


def read_template(dialect: str) -> tuple[Path, str]:
    path = REPO / bootstrap.TEMPLATES[dialect]
    return path, path.read_text(encoding="utf-8")


class BootstrapTemplateContractTest(unittest.TestCase):
    def test_all_templates_exist_and_have_placeholders(self):
        for dialect in ALL_DIALECTS:
            with self.subTest(dialect=dialect):
                path, text = read_template(dialect)
                self.assertTrue(path.is_file())
                self.assertIn(bootstrap.USER_PLACEHOLDER, text)
                self.assertIn(bootstrap.HASH_PLACEHOLDER, text)

    def test_templates_do_not_hardcode_user_id_one(self):
        for dialect in ALL_DIALECTS:
            with self.subTest(dialect=dialect):
                _, text = read_template(dialect)
                user_stmt = bootstrap._insert_statement(text, "system_users")
                self.assertIsNotNone(user_stmt)
                self.assertNotRegex(
                    user_stmt,
                    r"\b(?:VALUES|SELECT)\s*\(?\s*1\s*,",
                    "bootstrap 用户不能固定 id=1",
                )

    def test_templates_create_role_and_post_relations(self):
        for dialect in ALL_DIALECTS:
            with self.subTest(dialect=dialect):
                _, text = read_template(dialect)
                role_stmt = bootstrap._insert_statement(text, "system_user_role")
                post_stmt = bootstrap._insert_statement(text, "system_user_post")
                self.assertIsNotNone(role_stmt)
                self.assertIsNotNone(post_stmt)
                self.assertRegex(role_stmt, r"(?:\b1\b|\(\s*1\s*,)")
                self.assertRegex(post_stmt, r"(?:\b4\b|\(\s*4\s*,)")

    def test_post_ids_never_reference_missing_post_one(self):
        for dialect in ALL_DIALECTS:
            with self.subTest(dialect=dialect):
                _, text = read_template(dialect)
                self.assertNotRegex(text, r"post_ids[^\n]*\[1\]")
                self.assertNotRegex(text, r"post_ids[^\n]*\(1\)")

    def test_sqlserver_does_not_contain_nnull_typo(self):
        _, text = read_template("sqlserver")
        self.assertNotRegex(text, r"\bNNULL\b", re.IGNORECASE)

    def test_sqlserver_throw_is_inside_begin_end_block(self):
        _, text = read_template("sqlserver")
        self.assertRegex(
            text,
            r"IF\s+@zhicloud_bootstrap_user_count\s*>\s*1\s*"
            r"BEGIN\s+THROW\b",
            "SQL Server 的 THROW 必须放在 BEGIN...END 中，避免前一语句分号边界问题",
        )

    def test_templates_do_not_contain_known_seed_secrets(self):
        for dialect in ALL_DIALECTS:
            with self.subTest(dialect=dialect):
                _, text = read_template(dialect)
                for value in SEED_PASSWORD_HASHES:
                    self.assertNotIn(value, text)
                self.assertNotRegex(
                    text,
                    r"(?<!\d)1[3-9]\d{9}(?!\d)",
                    "模板不得包含真实手机号",
                )
                self.assertNotRegex(
                    text,
                    r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}",
                    "模板不得包含真实邮箱",
                )

    def test_non_mysql_templates_declare_single_use_boundary(self):
        for dialect in ALL_DIALECTS:
            if dialect == "mysql":
                continue
            with self.subTest(dialect=dialect):
                _, text = read_template(dialect)
                self.assertIn(
                    "执行边界：",
                    text,
                    "非 MySQL bootstrap 必须明确单次执行边界",
                )

    def test_mysql_template_declares_client_only_delimiter_boundary(self):
        _, text = read_template("mysql")
        self.assertIn("MySQL 执行边界：", text)
        self.assertIn("DELIMITER", text)
        self.assertIn("JDBC", text)

    def test_all_templates_guard_duplicate_bootstrap_usernames(self):
        for dialect in ALL_DIALECTS:
            with self.subTest(dialect=dialect):
                _, text = read_template(dialect)
                self.assertRegex(
                    text,
                    r"COUNT\s*\(\s*\*\s*\)",
                    "bootstrap 必须显式检测重复账号，不能静默更新任意一条",
                )

    def test_all_templates_guard_lookup_by_tenant(self):
        for dialect in ALL_DIALECTS:
            with self.subTest(dialect=dialect):
                _, text = read_template(dialect)
                lookup = re.search(
                    r"SELECT[\s\S]{0,160}?COUNT\s*\(\s*\*\s*\)[\s\S]{0,300}?"
                    r"FROM\s+[`\"]?system_users[`\"]?[\s\S]{0,300}?;",
                    text,
                    re.IGNORECASE,
                )
                self.assertIsNotNone(lookup, "必须存在账号预检查询")
                self.assertRegex(
                    lookup.group(0),
                    r"[`\"]?tenant_id[`\"]?\s*=\s*1",
                    "账号预检必须限定 tenant_id=1",
                )

    def test_non_mysql_templates_are_retry_safe(self):
        for dialect in ("postgresql", "highgo", "kingbase", "opengauss", "oracle", "dm"):
            with self.subTest(dialect=dialect):
                _, text = read_template(dialect)
                self.assertRegex(text, r"\bUPDATE\s+system_users\b", re.IGNORECASE)
                self.assertRegex(text, r"\bDELETE\s+FROM\s+system_user_role\b", re.IGNORECASE)
                self.assertRegex(text, r"\bDELETE\s+FROM\s+system_user_post\b", re.IGNORECASE)
                self.assertRegex(text, r"\bBEGIN\b", re.IGNORECASE)
                self.assertRegex(text, r"\bEND\b", re.IGNORECASE)

    def test_my_sql_and_sqlserver_are_retry_safe(self):
        for dialect in ("mysql", "sqlserver"):
            with self.subTest(dialect=dialect):
                _, text = read_template(dialect)
                self.assertRegex(text, r"\bSELECT\b", "必须查询已有账号")
                self.assertRegex(text, r"\bUPDATE\b", "必须支持已有账号更新")
                self.assertRegex(text, r"\bDELETE\b", "必须重建角色/岗位关系")
                lookup = re.search(
                    r"SELECT[\s\S]{0,160}?COUNT\s*\(\s*\*\s*\)[\s\S]{0,300}?"
                    r"FROM\s+[`\"]?system_users[`\"]?[\s\S]{0,300}?;",
                    text,
                    re.IGNORECASE,
                )
                self.assertIsNotNone(lookup, "必须存在账号预检查询")
                self.assertRegex(
                    lookup.group(0),
                    r"[`\"]?tenant_id[`\"]?\s*=\s*1",
                    "账号预检必须限定 tenant_id=1",
                )


class BootstrapRenderTest(unittest.TestCase):
    def test_render_succeeds_for_all_shipped_templates(self):
        for dialect in ALL_DIALECTS:
            with self.subTest(dialect=dialect):
                path, _ = read_template(dialect)
                rendered = bootstrap.render(path, "zhicloud_admin", HASH)
                self.assertIn("zhicloud_admin", rendered)
                self.assertIn(HASH, rendered)
                self.assertNotIn(bootstrap.USER_PLACEHOLDER, rendered)
                self.assertNotIn(bootstrap.HASH_PLACEHOLDER, rendered)

    def test_render_rejects_missing_role_relation(self):
        path, text = read_template("postgresql")
        text = text.replace("INSERT INTO system_user_role", "INSERT INTO system_user_role_disabled", 1)
        with tempfile.TemporaryDirectory() as tmp:
            broken = Path(tmp) / "broken.sql"
            broken.write_text(text, encoding="utf-8")
            with self.assertRaises(SystemExit):
                bootstrap.render(broken, "zhicloud_admin", HASH)

    def test_render_rejects_missing_post_relation(self):
        path, text = read_template("postgresql")
        text = text.replace("INSERT INTO system_user_post", "INSERT INTO system_user_post_disabled", 1)
        with tempfile.TemporaryDirectory() as tmp:
            broken = Path(tmp) / "broken.sql"
            broken.write_text(text, encoding="utf-8")
            with self.assertRaises(SystemExit):
                bootstrap.render(broken, "zhicloud_admin", HASH)

    def test_render_rejects_template_without_duplicate_guard(self):
        path, text = read_template("postgresql")
        text = text.replace("COUNT(*)", "MAX(1)", 1)
        with tempfile.TemporaryDirectory() as tmp:
            broken = Path(tmp) / "broken.sql"
            broken.write_text(text, encoding="utf-8")
            with self.assertRaises(SystemExit):
                bootstrap.render(broken, "zhicloud_admin", HASH)

    def test_render_rejects_template_without_tenant_scoped_lookup(self):
        path, text = read_template("postgresql")
        text = text.replace("tenant_id = 1", "tenant_id >= 1", 1)
        with tempfile.TemporaryDirectory() as tmp:
            broken = Path(tmp) / "broken.sql"
            broken.write_text(text, encoding="utf-8")
            with self.assertRaises(SystemExit):
                bootstrap.render(broken, "zhicloud_admin", HASH)

    def test_render_rejects_non_mysql_without_execution_boundary(self):
        path, text = read_template("postgresql")
        text = text.replace("执行边界：", "执行说明：")
        with tempfile.TemporaryDirectory() as tmp:
            broken = Path(tmp) / "broken.sql"
            broken.write_text(text, encoding="utf-8")
            with self.assertRaises(SystemExit):
                bootstrap.render(broken, "zhicloud_admin", HASH)

    def test_render_rejects_mysql_without_client_only_delimiter_boundary(self):
        path, text = read_template("mysql")
        text = text.replace("MySQL 执行边界：", "执行说明：", 1)
        with tempfile.TemporaryDirectory() as tmp:
            broken = Path(tmp) / "mysql" / "broken.sql"
            broken.parent.mkdir()
            broken.write_text(text, encoding="utf-8")
            with self.assertRaises(SystemExit):
                bootstrap.render(broken, "zhicloud_admin", HASH)

    def test_render_rejects_public_seed_username(self):
        path, _ = read_template("mysql")
        with self.assertRaises(SystemExit):
            bootstrap.render(path, "admin", HASH)

    def test_default_username_is_not_a_public_seed(self):
        self.assertEqual(bootstrap.DEFAULT_USERNAME, "zhicloud_admin")
        self.assertNotIn(bootstrap.DEFAULT_USERNAME.lower(), bootstrap.KNOWN_SEED_USERNAMES)


class HelmBootstrapAdminContractTest(unittest.TestCase):
    """固定 ArgoCD 管理员引导 Job 的静态安全与执行顺序契约。"""

    HELM = REPO / "deploy/helm/zhicloud"

    def _read(self, relative: str) -> str:
        return (self.HELM / relative).read_text(encoding="utf-8")

    def test_job_exists_and_is_a_presync_hook(self):
        job = self._read("templates/job-bootstrap-admin.yaml")
        self.assertIn("kind: Job", job)
        self.assertIn("argocd.argoproj.io/hook: PreSync", job)
        self.assertIn("argocd.argoproj.io/hook-delete-policy: before-hook-creation", job)
        self.assertIn("completions: 1", job)
        self.assertIn("parallelism: 1", job)
        self.assertIn("restartPolicy: Never", job)

    def test_job_runs_bootstrap_runner_without_spring_application_startup(self):
        job = self._read("templates/job-bootstrap-admin.yaml")
        self.assertIn(
            "-Dloader.main=cn.zhicloud.server.BootstrapAdminRunner",
            job,
        )
        self.assertIn("org.springframework.boot.loader.launch.PropertiesLauncher", job)
        self.assertIn("/zhicloud-server/app.jar", job)

    def test_job_password_comes_only_from_an_external_secret(self):
        job = self._read("templates/job-bootstrap-admin.yaml")
        password_block = re.search(
            r"- name: BOOTSTRAP_ADMIN_PASSWORD\n(?P<body>.*?)(?=\n\s*- name: MYSQL_TLS_TRUSTSTORE_URL|\n\s+command:)",
            job,
            re.DOTALL,
        )
        self.assertIsNotNone(password_block, "未找到管理员口令注入块")
        block = password_block.group("body")
        self.assertIn("valueFrom:", block)
        self.assertIn("secretKeyRef:", block)
        self.assertIn("bootstrapAdmin.passwordSecretName", block)
        self.assertIn("bootstrapAdmin.passwordSecretKey", block)
        self.assertNotRegex(block, r"\bvalue:\s*(?!\{\{)")
        self.assertNotRegex(job, r"BOOTSTRAP_ADMIN_PASSWORD\s*[:=]\s*[A-Za-z0-9$./_-]{8,}")

    def test_default_and_environment_values_keep_bootstrap_disabled(self):
        for relative in (
            "values.yaml",
            "values-dev.yaml",
            "values-staging.yaml",
            "values-prod.yaml",
        ):
            with self.subTest(relative=relative):
                values = self._read(relative)
                match = re.search(
                    r"^bootstrapAdmin:\n(?P<body>(?:[ \t]+.*\n?)*)",
                    values,
                    re.MULTILINE,
                )
                self.assertIsNotNone(match, f"{relative} 缺少 bootstrapAdmin 配置")
                body = match.group("body")
                self.assertIsNotNone(
                    re.search(r"^[ \t]+enabled:[ \t]*false[ \t]*$", body, re.MULTILINE),
                    f"{relative} 必须显式保持 bootstrapAdmin.enabled=false",
                )

    def test_existing_mode_bootstrap_runs_before_migration(self):
        job = self._read("templates/job-bootstrap-admin.yaml")
        migration = self._read("templates/job-migration.yaml")
        self.assertRegex(job, r'argocd\.argoproj\.io/sync-wave:\s*"-2"')
        self.assertRegex(migration, r'argocd\.argoproj\.io/sync-wave:\s*"-1"')

    def test_clean_mode_bootstrap_runs_after_migration_and_requires_it(self):
        job = self._read("templates/job-bootstrap-admin.yaml")
        self.assertRegex(job, r'argocd\.argoproj\.io/sync-wave:\s*"0"')
        self.assertIn("bootstrapAdmin 在 clean 模式下必须由 migration.enabled=true", job)

    def test_job_reuses_mysql_tls_truststore_contract(self):
        job = self._read("templates/job-bootstrap-admin.yaml")
        for token in (
            "MYSQL_TLS_TRUSTSTORE_URL",
            "MYSQL_TLS_TRUSTSTORE_TYPE",
            "MYSQL_TLS_TRUSTSTORE_PASSWORD",
            "mysql-tls-truststore",
            "mysqlTls.truststore.existingSecret",
            "mysqlTls.truststore.passwordKey",
            "mysqlTls.truststore.mountPath",
            "mysqlTls.truststore.fileName",
        ):
            with self.subTest(token=token):
                self.assertIn(token, job)


class BootstrapMysqlClientBoundaryTest(unittest.TestCase):
    """固定 MySQL bootstrap 只能经 mysql 客户端文件输入执行。"""

    def _args(self, client: str) -> argparse.Namespace:
        return argparse.Namespace(
            mysql_client=client,
            host="127.0.0.1",
            port=3306,
            user="root",
            password_env=None,
        )

    def test_run_sql_file_streams_rendered_file_to_mysql_client(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "bootstrap.sql"
            path.write_text("DELIMITER $$\nSELECT 1;\n", encoding="utf-8")
            completed = subprocess.CompletedProcess(["mysql"], 0, b"ok", b"")
            with mock.patch.object(bootstrap.subprocess, "run", return_value=completed) as run:
                result = bootstrap.run_sql_file(
                    self._args(r"C:\tools\mysql.exe"), path, "zhicloud_platform")

        self.assertEqual(0, result["exit_code"])
        command = run.call_args.args[0]
        self.assertEqual(r"C:\tools\mysql.exe", command[0])
        self.assertIn("--protocol=TCP", command)
        self.assertIn("zhicloud_platform", command)
        self.assertNotIn("input", run.call_args.kwargs)
        stdin = run.call_args.kwargs["stdin"]
        self.assertEqual(str(path), stdin.name)
        self.assertTrue(stdin.closed, "run_sql_file 必须在调用后关闭文件句柄")

    def test_run_sql_text_streams_text_without_sql_file_boundary(self):
        completed = subprocess.CompletedProcess(["mysql"], 0, b"0\n", b"")
        with mock.patch.object(bootstrap.subprocess, "run", return_value=completed) as run:
            result = bootstrap.run_sql_text(
                self._args("mysql.exe"), "SELECT COUNT(*) FROM system_users;", "zhicloud_platform")

        self.assertEqual("0", result["stdout"])
        self.assertEqual(b"SELECT COUNT(*) FROM system_users;", run.call_args.kwargs["input"])
        self.assertNotIn("stdin", run.call_args.kwargs)

if __name__ == "__main__":
    unittest.main()
