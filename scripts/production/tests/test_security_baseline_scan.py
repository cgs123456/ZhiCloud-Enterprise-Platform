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
    def test_r5_secret_reference_names_are_not_secret_values(self):
        rep = self._scan(
            "deploy/helm/values.yaml",
            'existingSecret: ""\n'
            'passwordSecretName: ""\n'
            'passwordSecretKey: ""\n'
            'fileKey: ""\n',
        )
        self.assertEqual(codes(rep, "R5"), [])

    def test_r5_existing_secret_revision_metadata_is_not_a_secret_value(self):
        """revision 是轮转指纹，不是凭据；其非空约束由 Helm required 控制。"""
        rep = self._scan(
            "deploy/helm/zhicloud/values.yaml",
            'existingSecretRevision: ""\n'
            'mysqlTls:\n'
            '  truststore:\n'
            '    existingSecretRevision: ""\n',
        )
        self.assertEqual(codes(rep, "R5"), [])

    def test_r5_empty_api_key_value_is_still_a_secret_value(self):
        rep = self._scan("deploy/helm/values-prod.yaml", 'API_KEY: ""\n')
        self.assertEqual(len(codes(rep, "R5")), 1)

    def test_r6_literal_weak_secret(self):
        rep = self._scan("application-dev.yaml", "      client-secret: xx\n")
        self.assertEqual(len(codes(rep, "R6")), 1)

    def test_r7_test_account_seed(self):
        rep = self._scan("sql/mysql/zhicloud_platform.sql",
                         "INSERT INTO `system_users` (`id`, `username`) VALUES (1, 'admin');\n")
        self.assertEqual(len(codes(rep, "R7")), 1)

    def test_r7_insert_ignore_test_account_seed(self):
        rep = self._scan(
            "zhicloud-server/src/main/resources/db/migration/V1.1__system_base_schema.sql",
            "INSERT IGNORE INTO `system_users` (`id`, `username`) "
            "VALUES (117, 'admin123');\n")
        self.assertEqual(len(codes(rep, "R7")), 1)
        self.assertEqual(codes(rep, "R7")[0]["severity"], "P0")

    def test_r7_non_whitelisted_production_seed_account(self):
        rep = self._scan(
            "zhicloud-server/src/main/resources/db/migration/V1.1__system_base_schema.sql",
            "INSERT IGNORE INTO `system_users` (`id`, `username`) "
            "VALUES (28, 'hrmgr');\n")
        self.assertEqual(len(codes(rep, "R7")), 1)
        self.assertEqual(codes(rep, "R7")[0]["severity"], "P0")

    def test_r7_bootstrap_placeholder_is_not_a_seed_account(self):
        rep = self._scan(
            "sql/mysql/zhicloud_platform_bootstrap.sql",
            "INSERT INTO system_users (id, username, password) "
            "VALUES (1, '__ZHICLOUD_BOOTSTRAP_USERNAME__', "
            "'__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__');\n")
        self.assertEqual(codes(rep, "R7"), [])

    def test_r7_sqlserver_n_literal_seed_account(self):
        rep = self._scan(
            "demo-data/sql/sqlserver/ruoyi-vue-pro_demo_seed.sql",
            "INSERT INTO system_users (id, username, password) "
            "VALUES (1, N'admin', N'hash');\n")
        self.assertEqual(len(codes(rep, "R7")), 1)
        self.assertEqual(codes(rep, "R7")[0]["severity"], "P1")

    def test_r10_literal_oauth2_seed_secret(self):
        rep = self._scan(
            "zhicloud-server/src/main/resources/db/migration/V1.1__system_base_schema.sql",
            "INSERT IGNORE INTO `system_oauth2_client` (`id`, `client_id`, `secret`) "
            "VALUES (1, 'default', 'admin123');\n")
        self.assertEqual(len(codes(rep, "R10")), 1)
        self.assertEqual(codes(rep, "R10")[0]["code"], "SQL_LITERAL_SEED_CREDENTIAL")
        self.assertEqual(codes(rep, "R10")[0]["severity"], "P0")

    def test_r10_literal_social_client_seed_secret(self):
        rep = self._scan(
            "sql/mysql/zhicloud_platform.sql",
            "INSERT INTO `system_social_client` "
            "(`id`, `name`, `social_type`, `user_type`, `client_id`, `client_secret`) "
            "VALUES (1, 'dingtalk', 20, 2, 'client-id', 'client-secret');\n")
        self.assertEqual(len(codes(rep, "R10")), 1)

    def test_r10_placeholder_secret_is_not_a_literal_seed(self):
        rep = self._scan(
            "zhicloud-server/src/main/resources/db/migration/V1.1__system_base_schema.sql",
            "INSERT IGNORE INTO `system_oauth2_client` (`id`, `client_id`, `secret`) "
            "VALUES (1, 'default', ?);\n")
        self.assertEqual(codes(rep, "R10"), [])

    def test_r8_demo_mode_enabled(self):
        rep = self._scan("application.yaml", "  demo: true\n")
        self.assertEqual(len(codes(rep, "R8")), 1)

    def test_r9_deprecated_identity_verification_disabled(self):
        rep = self._scan(
            "deploy/helm/zhicloud/values-prod.yaml",
            "url: jdbc:mysql://db:3306/x?verifyServerCertificate=false\n")
        self.assertEqual(len(codes(rep, "R9")), 1)
        self.assertEqual(codes(rep, "R9")[0]["code"],
                         "JDBC_TLS_IDENTITY_NOT_VERIFIED")
        self.assertEqual(codes(rep, "R9")[0]["severity"], "P0")

    def test_r9_ssl_mode_required_is_not_identity_verification(self):
        rep = self._scan(
            "deploy/helm/zhicloud/values-prod.yaml",
            "url: jdbc:mysql://db:3306/x?sslMode=REQUIRED\n")
        self.assertEqual(len(codes(rep, "R9")), 1)

    def test_r9_ssl_mode_verify_ca_is_not_identity_verification(self):
        rep = self._scan(
            "k8s/zhicloud-server.yaml",
            "url: jdbc:mysql://db:3306/x?sslMode=VERIFY_CA\n")
        self.assertEqual(len(codes(rep, "R9")), 1)

    def test_r9_comment_only_is_not_a_finding(self):
        rep = self._scan(
            "deploy/helm/zhicloud/values-prod.yaml",
            "# sslMode=REQUIRED/VERIFY_CA 只是加密或证书链校验，不能替代 VERIFY_IDENTITY\n")
        self.assertEqual(codes(rep, "R9"), [])

    def test_r9_verify_identity_is_allowed(self):
        rep = self._scan(
            "deploy/helm/zhicloud/values-prod.yaml",
            "url: jdbc:mysql://db:3306/x?sslMode=VERIFY_IDENTITY\n")
        self.assertEqual(codes(rep, "R9"), [])

    def test_clean_file_has_no_finding(self):
        rep = self._scan("deploy/helm/values.yaml",
                         "url: jdbc:mysql://db:3306/x?useSSL=true\n"
                         "MASTER_DATASOURCE_PASSWORD: ${DB_PASSWORD:?required}\n")
        self.assertEqual(rep["findings"], [])

    def test_unscanned_suffix_is_ignored(self):
        write(self.root, "notes.txt", "password: ${X:weak}\n")
        rep = sec.scan_repo(self.root)
        self.assertEqual(rep["findings"], [])

    def test_generated_evidence_yaml_is_not_rescanned(self):
        write(
            self.root,
            "docs/enterprise-platform/evidence/production/"
            "helm-render-20260927_212935/render-helm3-dev.yaml",
            "url: jdbc:mysql://db:3306/x?useSSL=false\n"
            'MASTER_DATASOURCE_PASSWORD: ""\n',
        )
        rep = sec.scan_repo(self.root)
        self.assertEqual(rep["scanned_files"], 0)
        self.assertEqual(rep["findings"], [])


class SeverityTest(unittest.TestCase):
    """严重度分级：生产路径上的凭据类命中必须是 P0。"""

    def test_prod_path_credential_is_p0(self):
        self.assertEqual(sec.severity_for("R1", "deploy/helm/values.yaml"), "P0")
        self.assertEqual(sec.severity_for("R5", "deploy/helm/values.yaml"), "P0")
        self.assertEqual(sec.severity_for("R6", "k8s/zhicloud-server.yaml"), "P0")
        self.assertEqual(sec.severity_for(
            "R9", "deploy/helm/zhicloud/values-prod.yaml"), "P0")

    def test_sql_install_path_is_p0(self):
        self.assertEqual(sec.severity_for("R7", "sql/mysql/zhicloud_platform.sql"), "P0")
        self.assertEqual(sec.severity_for("R7", "sql/oracle/other.sql"), "P0")

    def test_non_prod_path_is_downgraded(self):
        self.assertEqual(sec.severity_for("R1", "application-dev.yaml"), "P1")
        self.assertEqual(sec.severity_for(
            "R9", "deploy/helm/zhicloud/values-dev.yaml"), "P1")
        self.assertEqual(sec.severity_for("R4", "deploy/helm/values-prod.yaml"), "P1")
        self.assertEqual(sec.severity_for("R8", "application-dev.yaml"), "P2")

    def test_production_flyway_seed_is_p0(self):
        path = "zhicloud-server/src/main/resources/db/migration/V1.1__system_base_schema.sql"
        self.assertEqual(sec.severity_for("R7", path), "P0")
        self.assertEqual(sec.severity_for("R10", path), "P0")

    def test_non_sql_test_account_is_p1(self):
        self.assertEqual(sec.severity_for("R7", "docs/seed.sql"), "P1")
        self.assertEqual(sec.severity_for("R10", "docs/seed.sql"), "P1")


class ControlTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def _controls(self):
        return {c["control"]: c for c in sec.run_controls(self.root)}

    def test_passing_controls(self):
        write(self.root, "zhicloud-server/src/main/resources/application-prod.yaml",
              "springdoc:\n  api-docs:\n    enabled: false\n"
              "  swagger-ui:\n    enabled: false\n"
              "knife4j:\n  enable: false\n"
              "mybatis-plus:\n  encryptor:\n"
              "    password: ${MYBATIS_ENCRYPTOR_PASSWORD:?required}\n"
              "spring:\n  datasource:\n    dynamic:\n      druid:\n"
              "        connection-properties:\n"
              "          trustCertificateKeyStoreUrl: ${MYSQL_TLS_TRUSTSTORE_URL}\n"
              "          trustCertificateKeyStorePassword: ${MYSQL_TLS_TRUSTSTORE_PASSWORD}\n"
              "          trustCertificateKeyStoreType: ${MYSQL_TLS_TRUSTSTORE_TYPE:PKCS12}\n")
        write(self.root, "deploy/helm/templates/_helpers.tpl",
              "{{- define \"zhicloud.requiredSecret\" -}}\n")
        write(self.root, "deploy/helm/zhicloud/templates/secret.yaml",
              "{{ include \"zhicloud.requiredSecret\" . }}\n"
              "ZHICLOUD_AIRAG_DATASOURCE_PASSWORD: required\n")
        write(self.root, "sql/postgresql/airag_pgvector.sql",
              "CREATE TABLE IF NOT EXISTS airag_vector_store (id UUID PRIMARY KEY);\n"
              "CREATE INDEX IF NOT EXISTS airag_vector_store_index ON airag_vector_store USING hnsw (embedding vector_cosine_ops);\n")
        write(self.root, "deploy/helm/zhicloud/values-prod.yaml",
              "env:\n"
              "  MASTER_DATASOURCE_URL: \"jdbc:mysql://db:3306/x?sslMode=VERIFY_IDENTITY\"\n"
              "  SLAVE_DATASOURCE_URL: \"jdbc:mysql://db-slave:3306/x?sslMode=VERIFY_IDENTITY\"\n"
              "  ZHICLOUD_AIRAG_DATASOURCE_URL: \"jdbc:postgresql://db:5432/rag?sslmode=verify-full\"\n"
              "  ZHICLOUD_AIRAG_INITIALIZE_SCHEMA: \"false\"\n")
        write(self.root, "k8s/zhicloud-server.yaml",
              "MASTER_DATASOURCE_URL: \"jdbc:mysql://db:3306/x?sslMode=VERIFY_IDENTITY\"\n"
              "SLAVE_DATASOURCE_URL: \"jdbc:mysql://db-slave:3306/x?sslMode=VERIFY_IDENTITY\"\n"
              "ZHICLOUD_AIRAG_DATASOURCE_URL: \"jdbc:postgresql://db:5432/rag?sslmode=verify-full\"\n"
              "ZHICLOUD_AIRAG_INITIALIZE_SCHEMA: \"false\"\n"
              "MYSQL_TLS_TRUSTSTORE_URL\n"
              "MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "secretKeyRef:\n"
              "  name: zhicloud-mysql-tls-secret\n"
              "  key: MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "  mountPath: /etc/zhicloud/mysql-tls\n"
              "  secretName: zhicloud-mysql-tls-secret\n"
              "  key: truststore.p12\n")
        write(self.root, "deploy/helm/zhicloud/templates/deployment.yaml",
              "MYSQL_TLS_TRUSTSTORE_URL\n"
              "MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "secretKeyRef:\n"
              "  name: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.passwordKey | quote }}\n"
              "  mountPath: {{ .Values.mysqlTls.truststore.mountPath | quote }}\n"
              "  secretName: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.fileKey | quote }}\n")
        write(self.root, "deploy/helm/zhicloud/templates/job-migration.yaml",
              "MYSQL_TLS_TRUSTSTORE_URL\n"
              "MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "secretKeyRef:\n"
              "  name: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.passwordKey | quote }}\n"
              "  mountPath: {{ .Values.mysqlTls.truststore.mountPath | quote }}\n"
              "  secretName: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.fileKey | quote }}\n")
        write(self.root, "deploy/helm/zhicloud/templates/job-bootstrap-admin.yaml",
              "MYSQL_TLS_TRUSTSTORE_URL\n"
              "MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "secretKeyRef:\n"
              "  name: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.passwordKey | quote }}\n"
              "  mountPath: {{ .Values.mysqlTls.truststore.mountPath | quote }}\n"
              "  secretName: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.fileKey | quote }}\n"
              "BOOTSTRAP_ADMIN_PASSWORD\n"
              "valueFrom:\n"
              "secretKeyRef:\n"
              "  name: {{ required \"admin secret\" .Values.bootstrapAdmin.passwordSecretName | quote }}\n"
              "  key: {{ .Values.bootstrapAdmin.passwordSecretKey | quote }}\n")
        got = self._controls()
        self.assertEqual(got["C1"]["status"], "passed")
        self.assertEqual(got["C2"]["status"], "passed")
        self.assertEqual(got["C3"]["status"], "passed")
        self.assertEqual(got["C4"]["status"], "passed")
        self.assertEqual(got["C5"]["status"], "passed")
        self.assertEqual(got["C6"]["status"], "passed")
        self.assertEqual(got["C7"]["status"], "passed")
        self.assertEqual(got["C8"]["status"], "passed")
        self.assertEqual(got["C9"]["status"], "passed")
        self.assertEqual(got["C10"]["status"], "passed")
        self.assertEqual(got["C11"]["status"], "passed")
        self.assertEqual(got["C12"]["status"], "passed")
        self.assertEqual(got["C13"]["status"], "passed")
        self.assertEqual(got["C14"]["status"], "passed")
        self.assertEqual(got["C15"]["status"], "passed")
        self.assertEqual(got["C16"]["status"], "passed")
        self.assertEqual(got["C17"]["status"], "passed")
        self.assertEqual(got["C18"]["status"], "passed")

    def test_missing_file_is_reported(self):
        got = self._controls()
        self.assertEqual({c["status"] for c in got.values()}, {"file_missing"})

    def test_partial_pattern_is_failed(self):
        write(self.root, "zhicloud-server/src/main/resources/application-prod.yaml", "springdoc:\n  api-docs:\n    enabled: false\n")
        got = self._controls()
        self.assertEqual(got["C1"]["status"], "failed")
        self.assertTrue(got["C1"]["missing_patterns"])

    def test_airag_secret_missing_app_key_fails(self):
        write(self.root, "deploy/helm/zhicloud/templates/secret.yaml",
              "POSTGRES_PASSWORD: only_database_password\n")
        got = self._controls()
        self.assertEqual(got["C5"]["status"], "failed")
        self.assertTrue(got["C5"]["missing_patterns"])

    def test_missing_job_truststore_fails(self):
        write(self.root, "deploy/helm/zhicloud/templates/job-migration.yaml",
              "MYSQL_TLS_TRUSTSTORE_URL\n")
        got = self._controls()
        self.assertEqual(got["C15"]["status"], "failed")
        self.assertTrue(got["C15"]["missing_patterns"])

    def test_missing_bootstrap_job_truststore_fails(self):
        write(self.root, "deploy/helm/zhicloud/templates/job-bootstrap-admin.yaml",
              "MYSQL_TLS_TRUSTSTORE_URL\n")
        got = self._controls()
        self.assertEqual(got["C17"]["status"], "failed")
        self.assertTrue(got["C17"]["missing_patterns"])

    def test_missing_bootstrap_job_password_secret_fails(self):
        write(self.root, "deploy/helm/zhicloud/templates/job-bootstrap-admin.yaml",
              "BOOTSTRAP_ADMIN_PASSWORD\n")
        got = self._controls()
        self.assertEqual(got["C18"]["status"], "failed")
        self.assertTrue(got["C18"]["missing_patterns"])

    def test_missing_k8s_truststore_fails(self):
        write(self.root, "k8s/zhicloud-server.yaml",
              "MASTER_DATASOURCE_URL: \"jdbc:mysql://db:3306/x?sslMode=VERIFY_IDENTITY\"\n")
        got = self._controls()
        self.assertEqual(got["C16"]["status"], "failed")
        self.assertTrue(got["C16"]["missing_patterns"])


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
        write(self.root, "zhicloud-server/src/main/resources/application-prod.yaml",
              "springdoc:\n  api-docs:\n    enabled: false\n"
              "  swagger-ui:\n    enabled: false\n"
              "knife4j:\n  enable: false\n"
              "mybatis-plus:\n  encryptor:\n"
              "    password: ${MYBATIS_ENCRYPTOR_PASSWORD:?required}\n"
              "spring:\n  datasource:\n    dynamic:\n      druid:\n"
              "        connection-properties:\n"
              "          trustCertificateKeyStoreUrl: ${MYSQL_TLS_TRUSTSTORE_URL}\n"
              "          trustCertificateKeyStorePassword: ${MYSQL_TLS_TRUSTSTORE_PASSWORD}\n"
              "          trustCertificateKeyStoreType: ${MYSQL_TLS_TRUSTSTORE_TYPE:PKCS12}\n")
        write(self.root, "deploy/helm/templates/_helpers.tpl",
              "{{- define \"zhicloud.requiredSecret\" -}}\n")
        write(self.root, "deploy/helm/zhicloud/templates/secret.yaml",
              "{{ include \"zhicloud.requiredSecret\" . }}\n"
              "ZHICLOUD_AIRAG_DATASOURCE_PASSWORD: required\n")
        write(self.root, "sql/postgresql/airag_pgvector.sql",
              "CREATE TABLE IF NOT EXISTS airag_vector_store (id UUID PRIMARY KEY);\n"
              "CREATE INDEX IF NOT EXISTS airag_vector_store_index ON airag_vector_store USING hnsw (embedding vector_cosine_ops);\n")
        write(self.root, "deploy/helm/zhicloud/values-prod.yaml",
              "env:\n"
              "  MASTER_DATASOURCE_URL: \"jdbc:mysql://db:3306/x?sslMode=VERIFY_IDENTITY\"\n"
              "  SLAVE_DATASOURCE_URL: \"jdbc:mysql://db-slave:3306/x?sslMode=VERIFY_IDENTITY\"\n"
              "  ZHICLOUD_AIRAG_DATASOURCE_URL: \"jdbc:postgresql://db:5432/rag?sslmode=verify-full\"\n"
              "  ZHICLOUD_AIRAG_INITIALIZE_SCHEMA: \"false\"\n")
        write(self.root, "k8s/zhicloud-server.yaml",
              "MASTER_DATASOURCE_URL: \"jdbc:mysql://db:3306/x?sslMode=VERIFY_IDENTITY\"\n"
              "SLAVE_DATASOURCE_URL: \"jdbc:mysql://db-slave:3306/x?sslMode=VERIFY_IDENTITY\"\n"
              "ZHICLOUD_AIRAG_DATASOURCE_URL: \"jdbc:postgresql://db:5432/rag?sslmode=verify-full\"\n"
              "ZHICLOUD_AIRAG_INITIALIZE_SCHEMA: \"false\"\n"
              "MYSQL_TLS_TRUSTSTORE_URL\n"
              "MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "secretKeyRef:\n"
              "  name: zhicloud-mysql-tls-secret\n"
              "  key: MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "  mountPath: /etc/zhicloud/mysql-tls\n"
              "  secretName: zhicloud-mysql-tls-secret\n"
              "  key: truststore.p12\n")
        write(self.root, "deploy/helm/zhicloud/templates/deployment.yaml",
              "MYSQL_TLS_TRUSTSTORE_URL\n"
              "MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "secretKeyRef:\n"
              "  name: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.passwordKey | quote }}\n"
              "  mountPath: {{ .Values.mysqlTls.truststore.mountPath | quote }}\n"
              "  secretName: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.fileKey | quote }}\n")
        write(self.root, "deploy/helm/zhicloud/templates/job-migration.yaml",
              "MYSQL_TLS_TRUSTSTORE_URL\n"
              "MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "secretKeyRef:\n"
              "  name: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.passwordKey | quote }}\n"
              "  mountPath: {{ .Values.mysqlTls.truststore.mountPath | quote }}\n"
              "  secretName: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.fileKey | quote }}\n")
        write(self.root, "deploy/helm/zhicloud/templates/job-bootstrap-admin.yaml",
              "MYSQL_TLS_TRUSTSTORE_URL\n"
              "MYSQL_TLS_TRUSTSTORE_PASSWORD\n"
              "secretKeyRef:\n"
              "  name: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.passwordKey | quote }}\n"
              "  mountPath: {{ .Values.mysqlTls.truststore.mountPath | quote }}\n"
              "  secretName: {{ required \"truststore secret\" .Values.mysqlTls.truststore.existingSecret | quote }}\n"
              "  key: {{ .Values.mysqlTls.truststore.fileKey | quote }}\n"
              "BOOTSTRAP_ADMIN_PASSWORD\n"
              "valueFrom:\n"
              "secretKeyRef:\n"
              "  name: {{ required \"admin secret\" .Values.bootstrapAdmin.passwordSecretName | quote }}\n"
              "  key: {{ .Values.bootstrapAdmin.passwordSecretKey | quote }}\n")
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
        self.assertEqual(len(rep["rules"]), 10)
        self.assertEqual(rep["rules"]["R1"], "JDBC_TLS_DISABLED")
        self.assertEqual(rep["rules"]["R9"], "JDBC_TLS_IDENTITY_NOT_VERIFIED")

    def test_main_writes_output_and_returns_failed(self):
        write(self.root, "deploy/helm/values.yaml", "url: jdbc:mysql://d/x?useSSL=false\n")
        out = self.root / "out" / "security-baseline.json"
        code = sec.main(["--repo", str(self.root), "--output", str(out), "--summary"])
        self.assertEqual(code, 1)
        self.assertTrue(out.is_file())


if __name__ == "__main__":
    unittest.main()
