import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

SCRIPTS_DIR = Path(__file__).resolve().parents[1]
REPO_ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(SCRIPTS_DIR))

import schema_audit as sa  # noqa: E402

SANDBOX_SCRIPT = SCRIPTS_DIR / "mysql-sandbox.ps1"
VERIFY_SQL_DIR = REPO_ROOT.parent / "script" / "enterprise-platform-upgrade" / "verification"

FORBIDDEN_SQL = ("INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "TRUNCATE", "REPLACE", "GRANT", "REVOKE")


def column(
    table,
    name,
    column_type="bigint",
    data_type="bigint",
    nullable="NO",
    default="<NULL>",
    extra="",
    key="",
    char_length="",
    precision="",
    scale="",
):
    return (table, name, column_type, data_type, nullable, default, extra, key, char_length, precision, scale)


def index_rows(table, name, non_unique, columns):
    return [(table, name, non_unique, str(position + 1), column_name) for position, column_name in enumerate(columns)]


def table_row(name):
    return (name, "InnoDB", 0, "utf8mb4_0900_ai_ci")


class FakeRunner:
    """不触碰真实数据库的执行器：把三个 information_schema 查询映射到内存行。"""

    def __init__(self, tables=(), columns=(), indexes=(), server=None):
        self.tables = list(tables)
        self.columns = list(columns)
        self.indexes = list(indexes)
        self.server = server or (
            "8.4.11",
            "zhicloud_sandbox",
            "ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES",
            "+08:00",
        )
        self.queries = []

    def __call__(self, sql):
        self.queries.append(sql)
        if "information_schema.TABLES" in sql:
            return list(self.tables)
        if "information_schema.COLUMNS" in sql:
            return list(self.columns)
        if "information_schema.STATISTICS" in sql:
            return list(self.indexes)
        return [self.server]


def healthy(table="erp_sale_order", extra_columns=(), extra_indexes=()):
    """返回一张「健康」表的最小行集合：id 自增 + PRIMARY KEY (id)。"""
    columns = [column(table, "id", extra="auto_increment", key="PRI")]
    columns += list(extra_columns)
    indexes = index_rows(table, "PRIMARY", "0", ["id"])
    indexes += list(extra_indexes)
    return [table_row(table)], columns, indexes


def live_from(tables=(), columns=(), indexes=(), server=None):
    return sa.load_live_schema(FakeRunner(tables, columns, indexes, server))


def expected_table(
    name,
    columns,
    *,
    java_types=None,
    ddl_types=None,
    tenant=False,
    unique_indexes=(),
    indexes=(),
    v92=False,
):
    entry = sa.ExpectedTable(name=name, tenant=tenant)
    entry.add_column("id", "do")
    for item in columns:
        entry.add_column(item, "do")
    entry.java_types.update(java_types or {})
    entry.ddl_types.update(ddl_types or {})
    entry.unique_indexes |= set(unique_indexes)
    entry.indexes |= set(indexes)
    entry.sources.add("do")
    if v92:
        entry.sources.add("v92_verify")
    return {name: entry}


def categories(result):
    return sorted(finding.category for finding in result.findings)


class LiveSchemaLoadingTest(unittest.TestCase):
    def test_loader_maps_information_schema_rows(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "total_price", "decimal(20,4)", "decimal")])
        live = live_from(tables, columns, indexes)
        self.assertEqual("8.4.11", live.server_version)
        self.assertEqual("zhicloud_sandbox", live.database)
        self.assertIn("erp_sale_order", live.tables)
        self.assertTrue(live.columns["erp_sale_order"]["id"].is_auto_increment)
        self.assertEqual(("id",), live.primary_key_columns("erp_sale_order"))
        self.assertEqual({("id",)}, live.index_columns("erp_sale_order", unique=True))

    def test_loader_ignores_blank_rows(self):
        live = live_from([("", "", "", "")], [("", "", "", "", "", "", "", "", "", "", "")], [])
        self.assertEqual({}, live.tables)
        self.assertEqual({}, live.columns)

    def test_short_rows_do_not_crash(self):
        live = live_from([("t",)], [("t", "id", "bigint")], [("t", "PRIMARY", "0", "1", "id")])
        self.assertIn("t", live.tables)
        self.assertEqual(("id",), live.primary_key_columns("t"))


class P0ChecksTest(unittest.TestCase):
    def test_healthy_table_has_no_findings(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "account_id")])
        result = sa.analyze(expected_table("erp_sale_order", ["account_id"]), live_from(tables, columns, indexes))
        self.assertEqual([], [finding.to_dict() for finding in result.findings])

    def test_missing_table_is_p0_blocker(self):
        result = sa.analyze(expected_table("erp_sale_order", ["account_id"]), live_from())
        self.assertEqual(["missing_table"], categories(result))
        self.assertEqual("P0_BLOCKER", result.findings[0].severity)

    def test_missing_column_is_p0_blocker(self):
        tables, columns, indexes = healthy()
        result = sa.analyze(
            expected_table("erp_sale_order", ["account_id"]), live_from(tables, columns, indexes)
        )
        self.assertEqual(["missing_column"], categories(result))
        self.assertEqual("P0_BLOCKER", result.findings[0].severity)
        self.assertEqual("account_id", result.findings[0].column)

    def test_id_without_auto_increment_is_p0_blocker(self):
        tables = [table_row("erp_sale_order")]
        columns = [column("erp_sale_order", "id", key="PRI")]
        indexes = index_rows("erp_sale_order", "PRIMARY", "0", ["id"])
        result = sa.analyze(expected_table("erp_sale_order", []), live_from(tables, columns, indexes))
        self.assertEqual(["id_not_auto_increment"], categories(result))
        self.assertIn("1364", result.findings[0].message)

    def test_id_auto_increment_is_tagged_with_v92_source(self):
        tables = [table_row("erp_sale_order")]
        columns = [column("erp_sale_order", "id", key="PRI")]
        indexes = index_rows("erp_sale_order", "PRIMARY", "0", ["id"])
        result = sa.analyze(
            expected_table("erp_sale_order", [], v92=True),
            live_from(tables, columns, indexes),
            auto_increment_tables=["erp_sale_order"],
        )
        self.assertEqual(["id_not_auto_increment"], categories(result))
        self.assertIn("v92_verify", result.findings[0].sources)

    def test_missing_id_column_is_p0_blocker(self):
        tables = [table_row("erp_sale_order")]
        columns = [column("erp_sale_order", "order_no", "varchar(64)", "varchar", char_length="64", key="PRI")]
        indexes = index_rows("erp_sale_order", "PRIMARY", "0", ["order_no"])
        result = sa.analyze(expected_table("erp_sale_order", []), live_from(tables, columns, indexes))
        self.assertEqual(["id_column_missing"], categories(result))

    def test_primary_key_mismatch_is_p0_blocker(self):
        tables = [table_row("erp_sale_order")]
        columns = [column("erp_sale_order", "id", extra="auto_increment", key="PRI")]
        indexes = index_rows("erp_sale_order", "PRIMARY", "0", ["id", "tenant_id"])
        result = sa.analyze(expected_table("erp_sale_order", []), live_from(tables, columns, indexes))
        self.assertEqual(["primary_key_mismatch"], categories(result))
        self.assertEqual("id, tenant_id", result.findings[0].actual)

    def test_required_column_missing_from_do_is_p0_with_medium_confidence(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "bill_no", "varchar(64)", "varchar", default="<NULL>", char_length="64")])
        result = sa.analyze(expected_table("erp_sale_order", []), live_from(tables, columns, indexes))
        self.assertEqual(["required_column_not_in_do"], categories(result))
        self.assertEqual("P0_BLOCKER", result.findings[0].severity)
        self.assertEqual("medium", result.findings[0].confidence)

    def test_not_null_column_with_default_is_not_a_finding(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "bill_no", "varchar(64)", "varchar", default="''")])
        result = sa.analyze(expected_table("erp_sale_order", []), live_from(tables, columns, indexes))
        self.assertEqual([], categories(result))


class P1ChecksTest(unittest.TestCase):
    def test_tenant_column_missing_is_p1_high(self):
        tables, columns, indexes = healthy()
        result = sa.analyze(
            expected_table("erp_sale_order", [], tenant=True), live_from(tables, columns, indexes)
        )
        self.assertEqual(["tenant_column_missing"], categories(result))
        self.assertEqual("P1_HIGH", result.findings[0].severity)

    def test_tenant_index_missing_is_p1_high(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "tenant_id")])
        result = sa.analyze(
            expected_table("erp_sale_order", [], tenant=True), live_from(tables, columns, indexes)
        )
        self.assertEqual(["tenant_index_missing"], categories(result))
        self.assertEqual("P1_HIGH", result.findings[0].severity)
        self.assertEqual("medium", result.findings[0].confidence)

    def test_tenant_index_present_is_not_a_finding(self):
        tables, columns, indexes = healthy(
            extra_columns=[column("erp_sale_order", "tenant_id")],
            extra_indexes=index_rows("erp_sale_order", "idx_tenant", "1", ["tenant_id"]),
        )
        result = sa.analyze(
            expected_table("erp_sale_order", [], tenant=True), live_from(tables, columns, indexes)
        )
        self.assertEqual([], categories(result))

    def test_tenant_index_prefix_matches_tenant_first_column(self):
        tables, columns, indexes = healthy(
            extra_columns=[column("erp_sale_order", "tenant_id")],
            extra_indexes=index_rows("erp_sale_order", "idx_tenant_deleted", "1", ["tenant_id", "deleted"]),
        )
        result = sa.analyze(
            expected_table("erp_sale_order", [], tenant=True), live_from(tables, columns, indexes)
        )
        self.assertEqual([], categories(result))

    def test_tenant_index_requires_tenant_as_first_column(self):
        tables, columns, indexes = healthy(
            extra_columns=[column("erp_sale_order", "tenant_id")],
            extra_indexes=index_rows("erp_sale_order", "idx_deleted_tenant", "1", ["deleted", "tenant_id"]),
        )
        result = sa.analyze(
            expected_table("erp_sale_order", [], tenant=True), live_from(tables, columns, indexes)
        )
        self.assertEqual(["tenant_index_missing"], categories(result))

    def test_missing_unique_index_is_p1_high(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "bill_no", "varchar(64)", "varchar", default="''")])
        expected = expected_table("erp_sale_order", [], unique_indexes=[("bill_no",)])
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual(["missing_unique_index"], categories(result))
        self.assertEqual("P1_HIGH", result.findings[0].severity)

    def test_evolved_unique_index_is_divergence_not_missing(self):
        tables, columns, indexes = healthy(
            extra_columns=[column("erp_sale_order", "bill_no", "varchar(64)", "varchar", default="''")],
            extra_indexes=index_rows("erp_sale_order", "uk_tenant_bill", "0", ["tenant_id", "bill_no"]),
        )
        expected = expected_table("erp_sale_order", [], unique_indexes=[("bill_no",)])
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual([], categories(result))
        self.assertEqual(["unique_index_evolved"], [item.kind for item in result.divergences])

    def test_unique_index_is_matched_by_columns_not_name(self):
        tables, columns, indexes = healthy(
            extra_columns=[column("erp_sale_order", "bill_no", "varchar(64)", "varchar", default="''")],
            extra_indexes=index_rows("erp_sale_order", "uk_something_else", "0", ["bill_no"]),
        )
        expected = expected_table("erp_sale_order", [], unique_indexes=[("bill_no",)])
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual([], categories(result))

    def test_missing_plain_index_is_p1_high(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "create_time", "datetime", "datetime", nullable="YES", default="<NULL>")])
        expected = expected_table("erp_sale_order", [], indexes=[("create_time",)])
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual(["missing_index"], categories(result))
        self.assertEqual("P1_HIGH", result.findings[0].severity)

    def test_type_family_mismatch_is_p1_high(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "bill_no", "bigint", "bigint", default="'0'")])
        expected = expected_table("erp_sale_order", [], java_types={"bill_no": "String"})
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual(["column_type_mismatch"], categories(result))
        self.assertEqual("P1_HIGH", result.findings[0].severity)

    def test_boolean_tinyint_matches_java_boolean(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "enabled", "tinyint(1)", "tinyint", default="'1'")])
        expected = expected_table("erp_sale_order", [], java_types={"enabled": "Boolean"})
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual([], categories(result))

    def test_bigdecimal_on_bigint_is_compatible(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "amount", "bigint", "bigint", default="'0'")])
        expected = expected_table("erp_sale_order", [], java_types={"amount": "BigDecimal"})
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual([], categories(result))


class P2ChecksTest(unittest.TestCase):
    def test_date_versus_localdatetime_is_p2_review(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "order_time", "date", "date", nullable="YES", default="<NULL>")])
        expected = expected_table("erp_sale_order", [], java_types={"order_time": "LocalDateTime"})
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual(["column_width_review"], categories(result))
        self.assertEqual("P2_REVIEW", result.findings[0].severity)

    def test_declaration_drift_is_p2_review(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "total_price", "decimal(20,2)", "decimal", default="'0.0000'")])
        expected = expected_table("erp_sale_order", [], ddl_types={"total_price": "decimal(20,4)"})
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual(["column_type_declaration_drift"], categories(result))
        self.assertEqual("P2_REVIEW", result.findings[0].severity)

    def test_unsigned_normalisation_avoids_false_positive(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "sort", "int unsigned", "int", default="'0'")])
        expected = expected_table("erp_sale_order", [], ddl_types={"sort": "int unsigned"})
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual([], categories(result))


class DivergenceAndOrderingTest(unittest.TestCase):
    def test_live_db_wins_when_static_ddl_lacks_the_column(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "account_id")])
        expected = expected_table("erp_sale_order", ["account_id"])
        expected["erp_sale_order"].sources.add("ddl")
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual([], [finding.to_dict() for finding in result.findings])
        self.assertEqual(1, len(result.divergences))
        self.assertEqual("static_do_column_missing_in_live", result.divergences[0].kind)
        self.assertEqual("live_wins", result.divergences[0].resolution)

    def test_do_only_table_is_not_reported_as_divergence(self):
        tables, columns, indexes = healthy(extra_columns=[column("erp_sale_order", "account_id")])
        result = sa.analyze(expected_table("erp_sale_order", ["account_id"]), live_from(tables, columns, indexes))
        self.assertEqual([], result.divergences)

    def test_findings_are_sorted_by_severity_then_table(self):
        tables = [table_row("b_table"), table_row("a_table")]
        columns = [
            column("b_table", "id", extra="auto_increment", key="PRI"),
            column("a_table", "id", extra="auto_increment", key="PRI"),
            column("b_table", "account_id"),
            column("a_table", "account_id"),
            column("a_table", "tenant_id"),
        ]
        indexes = index_rows("b_table", "PRIMARY", "0", ["id"]) + index_rows("a_table", "PRIMARY", "0", ["id"])
        expected = {}
        expected.update(expected_table("a_table", ["account_id", "gone_a"], tenant=True))
        expected.update(expected_table("b_table", ["account_id", "gone_b"]))
        result = sa.analyze(expected, live_from(tables, columns, indexes))
        self.assertEqual(
            [
                ("P0_BLOCKER", "a_table", "missing_column"),
                ("P0_BLOCKER", "b_table", "missing_column"),
                ("P1_HIGH", "a_table", "tenant_index_missing"),
            ],
            [(finding.severity, finding.table, finding.category) for finding in result.findings],
        )
        severities = [finding.severity for finding in result.findings]
        self.assertEqual(sorted(severities, key=lambda item: sa.SEVERITY_ORDER[item]), severities)

    def test_counts_are_stable(self):
        result = sa.AnalysisResult(
            findings=[
                sa.Finding(severity="P1_HIGH", category="missing_index", table="t", message="m"),
                sa.Finding(severity="P0_BLOCKER", category="missing_column", table="t", message="m"),
                sa.Finding(severity="P0_BLOCKER", category="missing_column", table="t", message="m"),
            ]
        )
        self.assertEqual({"P0_BLOCKER": 2, "P1_HIGH": 1, "P2_REVIEW": 0}, result.counts_by_severity())
        self.assertEqual({"missing_column": 2, "missing_index": 1}, result.counts_by_category())


class ModuleSelectionTest(unittest.TestCase):
    def test_default_module_dirs_only_uses_active_maven_modules(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "pom.xml").write_text(
                """<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modules>
    <module>zhicloud-module-erp</module>
    <!-- <module>zhicloud-module-im</module> -->
    <module>zhicloud-framework</module>
  </modules>
</project>""",
                encoding="utf-8",
            )
            (root / "zhicloud-framework").mkdir()
            (root / "zhicloud-framework" / "pom.xml").write_text(
                """<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modules>
    <module>zhicloud-spring-boot-starter-biz-mdm</module>
  </modules>
</project>""",
                encoding="utf-8",
            )
            (root / "zhicloud-module-erp").mkdir()
            (root / "zhicloud-module-im").mkdir()
            (root / "zhicloud-framework" / "zhicloud-spring-boot-starter-biz-mdm").mkdir()

            selected = {path.name for path in sa.default_module_dirs(root)}

            self.assertEqual(
                {"zhicloud-module-erp", "zhicloud-spring-boot-starter-biz-mdm"},
                selected,
            )

    def test_default_module_dirs_fail_closed_without_root_pom(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            with self.assertRaises(sa.SchemaAuditError):
                sa.default_module_dirs(Path(temp_dir))


class StaticParsingTest(unittest.TestCase):
    def test_parse_do_fields_handles_annotations(self):
        text = (
            '@TableName("erp_demo")\n'
            "public class ErpDemoDO extends TenantBaseDO {\n"
            "    private Long id;\n"
            '    @TableField(value = "custom_col")\n'
            "    private String customField;\n"
            "    @TableField(exist = false)\n"
            "    private String ignoreMe;\n"
            "    private BigDecimal totalPrice;\n"
            "}\n"
        )
        fields = sa.parse_do_fields(text)
        self.assertEqual(
            {"id": "Long", "custom_col": "String", "total_price": "BigDecimal"}, fields
        )

    def test_parsed_columns_match_check_do_ddl(self):
        sys.path.insert(0, str(SCRIPTS_DIR.parent))
        import check_do_ddl

        do_files = sorted((REPO_ROOT / "zhicloud-module-erp").rglob("*DO.java"))[:10]
        self.assertTrue(do_files)
        for do_file in do_files:
            table, columns = check_do_ddl.parse_do(do_file)
            if not table:
                continue
            fields = sa.parse_do_fields(do_file.read_text(encoding="utf-8", errors="replace"))
            self.assertEqual(columns, set(fields), do_file.name)

    def test_parse_ddl_captures_columns_indexes_and_uniques(self):
        ddl = (
            "CREATE TABLE `erp_demo` (\n"
            "  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',\n"
            "  `bill_no` varchar(64) NOT NULL,\n"
            "  `total_price` decimal(20,4) NULL DEFAULT 0.0000,\n"
            "  `create_time` datetime NOT NULL,\n"
            "  PRIMARY KEY (`id`),\n"
            "  UNIQUE KEY `uk_bill_no` (`bill_no`),\n"
            "  KEY `idx_create_time` (`create_time`)\n"
            ") ENGINE = InnoDB COMMENT = '演示';\n"
        )
        with tempfile.TemporaryDirectory() as temp_dir:
            ddl_file = Path(temp_dir) / "demo.sql"
            ddl_file.write_text(ddl, encoding="utf-8")
            tables = sa.parse_ddl_expectations([ddl_file])
        entry = tables["erp_demo"]
        self.assertEqual({"id", "bill_no", "total_price", "create_time"}, set(entry.columns))
        self.assertEqual({("bill_no",)}, entry.unique_indexes)
        self.assertEqual({("create_time",)}, entry.indexes)
        self.assertEqual("decimal(20,4)", entry.ddl_types["total_price"])

    def test_ddl_constraint_lines_are_not_parsed_as_columns(self):
        ddl = (
            "CREATE TABLE `erp_demo` (\n"
            "  `id` bigint NOT NULL AUTO_INCREMENT,\n"
            "  `account_id` bigint NULL,\n"
            "  PRIMARY KEY (`id`),\n"
            "  UNIQUE KEY `uk_account` (`account_id`),\n"
            "  KEY `idx_account` (`account_id`),\n"
            "  CONSTRAINT `fk_parent` FOREIGN KEY (`account_id`) REFERENCES `erp_parent` (`id`),\n"
            "  FULLTEXT KEY `ft_account` (`account_id`)\n"
            ") ENGINE = InnoDB;\n"
        )
        with tempfile.TemporaryDirectory() as temp_dir:
            ddl_file = Path(temp_dir) / "constraints.sql"
            ddl_file.write_text(ddl, encoding="utf-8")
            tables = sa.parse_ddl_expectations([ddl_file])
        entry = tables["erp_demo"]
        self.assertEqual({"id", "account_id"}, set(entry.columns))
        self.assertEqual({("account_id",)}, entry.unique_indexes)
        self.assertIn(("account_id",), entry.indexes)


    def test_parse_ddl_applies_static_alter_add_column_and_indexes(self):
        ddl = (
            "CREATE TABLE `erp_demo` (\n"
            "  `id` bigint NOT NULL AUTO_INCREMENT,\n"
            "  `bill_no` varchar(64) NOT NULL,\n"
            "  PRIMARY KEY (`id`)\n"
            ") ENGINE = InnoDB;\n"
            "ALTER TABLE `erp_demo` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;\n"
            "ALTER TABLE `erp_demo` ADD UNIQUE KEY `uk_tenant_bill` (`tenant_id`, `bill_no`);\n"
            "ALTER TABLE `erp_demo` ADD KEY `idx_tenant_bill` (`tenant_id`, `bill_no`);\n"
        )
        with tempfile.TemporaryDirectory() as temp_dir:
            ddl_file = Path(temp_dir) / "alter.sql"
            ddl_file.write_text(ddl, encoding="utf-8")
            tables = sa.parse_ddl_expectations([ddl_file])
        entry = tables["erp_demo"]
        self.assertIn("tenant_id", entry.columns)
        self.assertIn(("tenant_id", "bill_no"), entry.unique_indexes)
        self.assertIn(("tenant_id", "bill_no"), entry.indexes)

    def test_check_do_ddl_applies_static_alter_add_column(self):
        sys.path.insert(0, str(SCRIPTS_DIR.parent))
        import check_do_ddl

        ddl = (
            "CREATE TABLE `erp_demo` (\n"
            "  `id` bigint NOT NULL,\n"
            "  PRIMARY KEY (`id`)\n"
            ") ENGINE = InnoDB;\n"
            "ALTER TABLE `erp_demo` ADD COLUMN `tenant_id` bigint NOT NULL DEFAULT 0;\n"
        )
        with tempfile.TemporaryDirectory() as temp_dir:
            ddl_file = Path(temp_dir) / "alter.sql"
            ddl_file.write_text(ddl, encoding="utf-8")
            tables = check_do_ddl.parse_ddl([ddl_file])
        self.assertIn("tenant_id", tables["erp_demo"])

    def test_default_ddl_files_prefer_flyway_migrations(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            old_dir = root / "sql" / "mysql"
            old_dir.mkdir(parents=True)
            old_ddl = old_dir / "zhicloud_platform.sql"
            old_ddl.write_text("SELECT 1;\n", encoding="utf-8")
            migration_dir = root / "zhicloud-server" / "src" / "main" / "resources" / "db" / "migration"
            migration_dir.mkdir(parents=True)
            migration = migration_dir / "V1__baseline.sql"
            migration.write_text("SELECT 1;\n", encoding="utf-8")
            self.assertEqual([migration], sa.default_ddl_files(root))

    def test_parse_verify_expectations_from_frozen_sql(self):
        files = sa.default_verify_sql_files(REPO_ROOT)
        if not files:
            self.skipTest("V9x verification SQL not present")
        verify = sa.parse_verify_expectations(files)
        self.assertEqual(6, len(verify["columns"]))
        self.assertEqual(71, sum(len(columns) for columns in verify["columns"].values()))
        self.assertEqual(95, len(verify["auto_increment_tables"]))
        self.assertIn("erp_sale_order", verify["columns"])
        self.assertIn("account_id", verify["columns"]["erp_sale_order"])

    def test_verify_nullable_expectations_are_parsed(self):
        files = sa.default_verify_sql_files(REPO_ROOT)
        if not files:
            self.skipTest("V9x verification SQL not present")
        verify = sa.parse_verify_expectations(files)
        self.assertEqual(25, sum(len(columns) for columns in verify["nullable_columns"].values()))

    def test_v91_nullable_expectation_unmet_is_p0(self):
        tables, columns, indexes = healthy(table="erp_product", extra_columns=[column("erp_product", "code", "varchar(64)", "varchar")])
        expected = expected_table("erp_product", ["code"])
        result = sa.analyze(
            expected,
            live_from(tables, columns, indexes),
            nullable_expectations={"erp_product": {"code": "v91_verify"}},
        )
        self.assertEqual(["nullable_expectation_unmet"], categories(result))
        self.assertEqual("P0_BLOCKER", result.findings[0].severity)
        self.assertEqual(["v91_verify"], result.findings[0].sources)

    def test_v91_nullable_expectation_met_is_not_a_finding(self):
        tables, columns, indexes = healthy(
            table="erp_product",
            extra_columns=[column("erp_product", "code", "varchar(64)", "varchar", nullable="YES", default="<NULL>")]
        )
        expected = expected_table("erp_product", ["code"])
        result = sa.analyze(
            expected,
            live_from(tables, columns, indexes),
            nullable_expectations={"erp_product": {"code": "v91_verify"}},
        )
        self.assertEqual([], categories(result))

    def test_build_expectations_merges_sources(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            temp = Path(temp_dir)
            module = temp / "demo-module"
            module.mkdir()
            (module / "DemoDO.java").write_text(
                '@TableName("demo_table")\n'
                "public class DemoDO {\n"
                "    private Long id;\n"
                "    private String doOnly;\n"
                "}\n",
                encoding="utf-8",
            )
            ddl = temp / "demo.sql"
            ddl.write_text(
                "CREATE TABLE `demo_table` (\n"
                "  `id` bigint NOT NULL AUTO_INCREMENT,\n"
                "  `ddl_only` varchar(32) NULL,\n"
                "  PRIMARY KEY (`id`)\n"
                ") ENGINE = InnoDB;\n",
                encoding="utf-8",
            )
            expected, sources = sa.build_expectations([module], [ddl], [])
        entry = expected["demo_table"]
        self.assertEqual({"id", "do_only", "ddl_only"}, set(entry.columns))
        self.assertEqual({"do"}, entry.columns["do_only"])
        self.assertEqual({"ddl"}, entry.columns["ddl_only"])
        self.assertEqual(1, sources["do_tables"])
        self.assertEqual(1, sources["ddl_tables"])


class SqlSafetyTest(unittest.TestCase):
    def test_audit_sql_is_read_only(self):
        for statement in sa.READ_ONLY_SQL:
            upper = statement.upper()
            self.assertTrue(upper.lstrip().startswith("SELECT"), statement)
            for keyword in FORBIDDEN_SQL:
                self.assertNotIn(keyword, upper, f"{keyword} found in: {statement}")

    def test_sql_is_passed_via_stdin_not_command_line(self):
        captured = {}

        class Completed:
            returncode = 0
            stdout = "8.4.11\tzhicloud_sandbox\tSTRICT_TRANS_TABLES\t+08:00\n"
            stderr = ""

        def fake_run(argv, **kwargs):
            captured["argv"] = argv
            captured["input"] = kwargs.get("input")
            return Completed()

        runner = sa.mysql_cli_runner(host="127.0.0.1", user="root", password="p", database="db")
        with mock.patch.object(sa.subprocess, "run", fake_run):
            rows = runner(sa.SQL_SERVER_INFO)
        self.assertEqual([("8.4.11", "zhicloud_sandbox", "STRICT_TRANS_TABLES", "+08:00")], rows)
        self.assertEqual(sa.SQL_SERVER_INFO, captured["input"])
        self.assertNotIn(sa.SQL_SERVER_INFO, captured["argv"])
        self.assertFalse(any(str(item).startswith("-e") for item in captured["argv"]))
        self.assertNotIn("p", captured["argv"])

    def test_runner_raises_on_non_zero_exit(self):
        class Completed:
            returncode = 1
            stdout = ""
            stderr = "ERROR 1045 (28000): Access denied"

        runner = sa.mysql_cli_runner(host="127.0.0.1", user="root")
        with mock.patch.object(sa.subprocess, "run", lambda *a, **k: Completed()):
            with self.assertRaises(sa.SchemaAuditError):
                runner(sa.SQL_SERVER_INFO)


def write_module(temp_dir, table="erp_demo", tenant=True, extra_field="accountId"):
    module_dir = Path(temp_dir) / "module"
    module_dir.mkdir(parents=True, exist_ok=True)
    base = "TenantBaseDO" if tenant else "BaseDO"
    (module_dir / "ErpDemoDO.java").write_text(
        f'@TableName("{table}")\n'
        f"public class ErpDemoDO extends {base} {{\n"
        "    private Long id;\n"
        f"    private Long {extra_field};\n"
        "}\n",
        encoding="utf-8",
    )
    ddl_file = Path(temp_dir) / "demo.sql"
    ddl_file.write_text(
        f"CREATE TABLE `{table}` (\n"
        "  `id` bigint NOT NULL AUTO_INCREMENT,\n"
        f"  `{sa.camel_to_snake(extra_field)}` bigint NULL,\n"
        "  PRIMARY KEY (`id`)\n"
        ") ENGINE = InnoDB;\n",
        encoding="utf-8",
    )
    return [str(module_dir)], [str(ddl_file)]


class CliTest(unittest.TestCase):
    def test_exit_code_is_2_when_p0_findings_exist(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            modules, ddls = write_module(temp_dir)
            output = Path(temp_dir) / "audit.json"
            runner = FakeRunner([table_row("erp_demo")], [column("erp_demo", "id")], [])
            with mock.patch.object(sa, "resolve_runner", return_value=(runner, {"mode": "test"})):
                code = sa.main(
                    [
                        "--repo-root", temp_dir,
                        "--module", modules[0],
                        "--ddl", ddls[0],
                        "--skip-verify-sql",
                        "--output", str(output),
                    ]
                )
            self.assertEqual(2, code)
            report = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual("failed", report["status"])
            self.assertEqual("P0_BLOCKER", report["summary"]["fail_on"])
            found = {finding["category"] for finding in report["findings"]}
            self.assertIn("id_not_auto_increment", found)
            self.assertEqual(2, report["summary"]["exit_code"])
            self.assertIn("只读审计", "".join(report["notes"]))

    def test_exit_code_is_0_when_only_p1_findings_with_default_threshold(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            modules, ddls = write_module(temp_dir)
            output = Path(temp_dir) / "audit.json"
            tables = [table_row("erp_demo")]
            columns = [
                column("erp_demo", "id", extra="auto_increment", key="PRI"),
                column("erp_demo", "account_id"),
                column("erp_demo", "tenant_id"),
            ]
            indexes = index_rows("erp_demo", "PRIMARY", "0", ["id"])
            runner = FakeRunner(tables, columns, indexes)
            with mock.patch.object(sa, "resolve_runner", return_value=(runner, {"mode": "test"})):
                code = sa.main(
                    [
                        "--repo-root", temp_dir,
                        "--module", modules[0],
                        "--ddl", ddls[0],
                        "--skip-verify-sql",
                        "--output", str(output),
                    ]
                )
            self.assertEqual(0, code)
            report = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual({"tenant_index_missing": 1}, report["summary"]["by_category"])
            self.assertEqual(1, report["summary"]["by_severity"]["P1_HIGH"])

    def test_fail_on_p1_turns_p1_into_exit_code_2(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            modules, ddls = write_module(temp_dir)
            tables = [table_row("erp_demo")]
            columns = [
                column("erp_demo", "id", extra="auto_increment", key="PRI"),
                column("erp_demo", "account_id"),
                column("erp_demo", "tenant_id"),
            ]
            indexes = index_rows("erp_demo", "PRIMARY", "0", ["id"])
            runner = FakeRunner(tables, columns, indexes)
            with mock.patch.object(sa, "resolve_runner", return_value=(runner, {"mode": "test"})):
                code = sa.main(
                    [
                        "--repo-root", temp_dir,
                        "--module", modules[0],
                        "--ddl", ddls[0],
                        "--skip-verify-sql",
                        "--fail-on", "P1_HIGH",
                    ]
                )
            self.assertEqual(2, code)

    def test_password_never_reaches_the_report(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            modules, ddls = write_module(temp_dir)
            output = Path(temp_dir) / "audit.json"
            runner = FakeRunner(*healthy("erp_demo", extra_columns=[column("erp_demo", "account_id")], extra_indexes=[]))
            captured = {}

            def fake_cli_runner(**kwargs):
                captured.update(kwargs)
                return runner

            with mock.patch.object(sa, "mysql_cli_runner", fake_cli_runner):
                code = sa.main(
                    [
                        "--repo-root", temp_dir,
                        "--module", modules[0],
                        "--ddl", ddls[0],
                        "--skip-verify-sql",
                        "--output", str(output),
                        "--host", "127.0.0.1",
                        "--database", "zhicloud_platform",
                        "--password", "topsecret",
                    ]
                )
            self.assertIn(code, (0, 2))
            self.assertNotIn("topsecret", output.read_text(encoding="utf-8"))
            self.assertEqual("topsecret", captured["password"])

    def test_remote_host_is_refused_without_explicit_flag(self):
        args = sa.build_parser().parse_args(["--host", "db.production.example.com", "--database", "zhicloud"])
        with self.assertRaises(sa.SchemaAuditError) as context:
            sa.resolve_runner(args)
        self.assertIn("refusing host", str(context.exception))

    def test_remote_host_is_allowed_with_explicit_flag(self):
        args = sa.build_parser().parse_args(
            ["--host", "db.production.example.com", "--database", "zhicloud", "--allow-remote-host"]
        )
        with mock.patch.object(sa, "mysql_cli_runner", lambda **kwargs: FakeRunner()):
            runner, connection = sa.resolve_runner(args)
        self.assertEqual("mysql-cli", connection["mode"])
        self.assertEqual("db.production.example.com", connection["host"])

    def test_missing_sandbox_state_is_an_execution_error(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            modules, ddls = write_module(temp_dir)
            code = sa.main(
                [
                    "--repo-root", temp_dir,
                    "--module", modules[0],
                    "--ddl", ddls[0],
                    "--skip-verify-sql",
                    "--sandbox-state", str(Path(temp_dir) / "missing.json"),
                ]
            )
            self.assertEqual(1, code)

    def test_unknown_database_connection_is_an_execution_error(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            modules, ddls = write_module(temp_dir)
            code = sa.main(
                [
                    "--repo-root", temp_dir,
                    "--module", modules[0],
                    "--ddl", ddls[0],
                    "--skip-verify-sql",
                    "--host", "127.0.0.1",
                ]
            )
            self.assertEqual(1, code)


def docker_available():
    docker = shutil.which("docker")
    if docker is None:
        return False
    probe = subprocess.run(
        [docker, "info", "--format", "{{.ServerVersion}}"],
        capture_output=True, text=True, check=False, timeout=60,
    )
    return probe.returncode == 0


@unittest.skipUnless(docker_available(), "docker daemon is not available")
class SandboxEndToEndTest(unittest.TestCase):
    """真库端到端：一次性 MySQL 8 沙箱 + 真实 information_schema 查询。"""

    def _force_cleanup(self, temp_dir, box):
        temp_dir = Path(temp_dir)
        state_file = temp_dir / "state.json"
        if state_file.exists():
            subprocess.run(
                ["pwsh", "-NoProfile", "-File", str(SANDBOX_SCRIPT), "stop",
                 "-StateFile", str(state_file), "-EvidenceFile", str(temp_dir / "teardown.json"), "-SkipEvidence"],
                capture_output=True, text=True, check=False, timeout=300,
            )
            if not state_file.exists():
                return
        docker = shutil.which("docker")
        if docker is None or not box.get("container_name"):
            return
        subprocess.run([docker, "rm", "--force", box["container_name"]], capture_output=True, check=False)
        subprocess.run([docker, "volume", "rm", "--force", box["volume"]], capture_output=True, check=False)

    def test_audit_detects_real_schema_drift(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        temp_dir = Path(temp.name)
        box = {"container_name": None, "volume": None}
        self.addCleanup(self._force_cleanup, str(temp_dir), box)

        state_file = temp_dir / "state.json"
        started = subprocess.run(
            ["pwsh", "-NoProfile", "-File", str(SANDBOX_SCRIPT), "start",
             "-StateFile", str(state_file), "-EvidenceFile", str(temp_dir / "evidence.json")],
            capture_output=True, text=True, check=False, timeout=300,
        )
        self.assertEqual(0, started.returncode, started.stdout + started.stderr)
        state = json.loads(state_file.read_text(encoding="utf-8"))
        box.update(state)
        container = state["container_name"]
        docker = shutil.which("docker")
        ddl = (
            "CREATE TABLE `erp_demo` ("
            "`id` bigint NOT NULL,"
            "`account_id` bigint NULL,"
            "`tenant_id` bigint NOT NULL,"
            "PRIMARY KEY (`id`)) ENGINE=InnoDB;"
            "CREATE TABLE `erp_ok` ("
            "`id` bigint NOT NULL AUTO_INCREMENT,"
            "`tenant_id` bigint NULL,"
            "PRIMARY KEY (`id`)) ENGINE=InnoDB;"
        )
        created = subprocess.run(
            [docker, "exec", "-i", "-e", f"MYSQL_PWD={state['root_password']}", container,
             "mysql", "-uroot", state["database"], "-e", ddl],
            capture_output=True, text=True, check=False,
        )
        self.assertEqual(0, created.returncode, created.stdout + created.stderr)

        modules, ddls = write_module(str(temp_dir))
        (Path(modules[0]) / "ErpOkDO.java").write_text(
            '@TableName("erp_ok")\n'
            "public class ErpOkDO extends BaseDO {\n"
            "    private Long id;\n"
            "    private Long accountId;\n"
            "}\n",
            encoding="utf-8",
        )
        output = temp_dir / "audit.json"
        audit = subprocess.run(
            [sys.executable, str(SCRIPTS_DIR / "schema_audit.py"),
             "--repo-root", str(REPO_ROOT),
             "--module", modules[0],
             "--ddl", ddls[0],
             "--skip-verify-sql",
             "--sandbox-state", str(state_file),
             "--output", str(output),
             "--print-findings", "5"],
            capture_output=True, text=True, encoding="utf-8", errors="replace", check=False, timeout=600,
        )
        self.assertEqual(2, audit.returncode, audit.stdout + audit.stderr)
        report = json.loads(output.read_text(encoding="utf-8"))
        self.assertEqual("8.4", report["server"]["version"][:3])
        self.assertEqual(state["database"], report["server"]["database"])
        findings = {(finding["category"], finding["table"]) for finding in report["findings"]}
        self.assertIn(("id_not_auto_increment", "erp_demo"), findings)
        self.assertIn(("missing_column", "erp_ok"), findings)
        self.assertNotIn(("id_not_auto_increment", "erp_ok"), findings)
        self.assertNotIn("root_password", json.dumps(report))


class DockerRunnerPasswordTransportTest(unittest.TestCase):
    """R-041/R-044：docker exec 变体不能把口令放进 argv。

    同文件的宿主侧 `mysql_cli_runner` 一直是 `env["MYSQL_PWD"]` 的正确形态，
    容器侧却写成 `-e MYSQL_PWD=<值>` —— 同一规则的两份实现漂移过一次，
    而注释还替它辩白说"不写进任何证据文件"。
    """

    def setUp(self):
        src = Path(__file__).resolve().parents[1] / "schema_audit.py"
        self.src = src.read_text(encoding="utf-8")

    def test_password_is_passed_via_child_env_not_argv(self):
        self.assertIn('argv += ["-e", "MYSQL_PWD"]', self.src)
        self.assertIn('child_env = {**os.environ, "MYSQL_PWD": password}', self.src)
        self.assertIn('env=child_env', self.src)
        self.assertEqual(0, self.src.count('MYSQL_PWD={password}'),
                         "口令被拼进 docker 命令行，会同时出现在进程表和命令记录里")


if __name__ == "__main__":
    unittest.main()
