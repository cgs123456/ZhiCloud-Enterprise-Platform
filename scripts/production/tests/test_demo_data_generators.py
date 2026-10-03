"""模拟数据生成器的回归测试（demo-data/generators）。

为什么需要这组测试：
  * 模拟数据是要被导入开发/演示库的，一旦列写错、NOT NULL 漏填、BIT/TINYINT 字面量写反，
    现场表现是「导入失败」或「脏数据混进真实台账」，排查成本极高；
  * 生成器宣称「确定性可复现」，这必须有机器校验，而不是口头保证；
  * 隔离规则（tenant 9001 / DEMO_ 前缀 / creator=demo-seed）是清理脚本正确性的前提，
    一旦被后续改动破坏，cleanup_demo.sql 就可能删不干净或误删。

注意：本测试只校验生成器与静态产物，不连接任何数据库。
"""

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
REPO_ROOT = TESTS_DIR.parents[2]
GENERATORS_DIR = REPO_ROOT / "demo-data" / "generators"
sys.path.insert(0, str(GENERATORS_DIR))

import generate_master_data as g  # noqa: E402

SNAPSHOT_PATH = REPO_ROOT / "demo-data" / "schema" / "schema-snapshot.json"
SQL_PATH = REPO_ROOT / "demo-data" / "sql" / "mysql" / "demo_master_data.sql"
SUMMARY_PATH = REPO_ROOT / "demo-data" / "schema" / "demo_master_data_summary.json"


def _load_snapshot():
    if not SNAPSHOT_PATH.is_file():
        raise unittest.SkipTest(f"schema 快照不存在，请先运行 schema_snapshot.py: {SNAPSHOT_PATH}")
    return g.load_snapshot(SNAPSHOT_PATH)


class DeterminismTest(unittest.TestCase):
    """同 seed 必须字节级复现；换 seed 必须变化（否则说明 seed 没生效）。"""

    def test_same_seed_renders_identical_sql(self):
        snap = _load_snapshot()
        args = (snap, 20260928, 9001, "small")
        first = g.render_sql(g.build_dataset(*args), *args, SNAPSHOT_PATH)
        second = g.render_sql(g.build_dataset(*args), *args, SNAPSHOT_PATH)
        self.assertEqual(g.sha256_text(first), g.sha256_text(second))

    def test_different_seed_changes_output(self):
        snap = _load_snapshot()
        a = g.render_sql(g.build_dataset(snap, 20260928, 9001, "small"),
                         snap, 20260928, 9001, "small", SNAPSHOT_PATH)
        b = g.render_sql(g.build_dataset(snap, 20260929, 9001, "small"),
                         snap, 20260929, 9001, "small", SNAPSHOT_PATH)
        self.assertNotEqual(g.sha256_text(a), g.sha256_text(b))

    def test_build_dataset_does_not_depend_on_hash_randomization(self):
        """random.Random 是唯一随机源；同一 seed 两次构建的数据集必须完全一致。"""
        snap = _load_snapshot()
        a = g.build_dataset(snap, 20260928, 9001, "small")
        b = g.build_dataset(snap, 20260928, 9001, "small")
        self.assertEqual(a, b)


class FailClosedTest(unittest.TestCase):
    """出现异常输入时必须抛错退出，绝不静默产出残缺数据。"""

    def test_unknown_column_is_rejected(self):
        snap = _load_snapshot()
        cols = g.column_index(snap, "erp_product")
        with self.assertRaises(Exception):
            g.validate_rows("erp_product", cols, [{"id": 1, "no_such_column": "x"}])

    def test_missing_not_null_column_is_rejected(self):
        snap = _load_snapshot()
        cols = g.column_index(snap, "erp_product_category")
        with self.assertRaises(Exception):
            g.validate_rows("erp_product_category", cols, [{"id": 1}])

    def test_unregistered_table_cannot_allocate_ids(self):
        alloc = g.make_id_allocator(9001)
        with self.assertRaises(Exception):
            alloc("no_such_table", 1)

    def test_row_count_beyond_stride_is_rejected(self):
        alloc = g.make_id_allocator(9001)
        with self.assertRaises(Exception):
            alloc("erp_product", g.ID_TABLE_STRIDE + 1)

    def test_unknown_scale_is_rejected(self):
        snap = _load_snapshot()
        with self.assertRaises(Exception):
            g.build_dataset(snap, 20260928, 9001, "huge")

    def test_missing_snapshot_is_rejected(self):
        with self.assertRaises(Exception):
            g.load_snapshot(REPO_ROOT / "demo-data" / "schema" / "__missing__.json")


class LiteralRenderTest(unittest.TestCase):
    """MySQL 字面量：BIT(1) 必须 b'0'/b'1'，TINYINT 必须是数字。"""

    def test_bit_zero(self):
        self.assertEqual(g.render_value({"name": "deleted", "type": "BIT(1)"}, 0), "b'0'")

    def test_bit_one(self):
        self.assertEqual(g.render_value({"name": "deleted", "type": "BIT(1)"}, 1), "b'1'")

    def test_tinyint_zero(self):
        self.assertEqual(g.render_value({"name": "deleted", "type": "TINYINT(1)"}, 0), "0")

    def test_null(self):
        self.assertEqual(g.render_value({"name": "x", "type": "VARCHAR(10)"}, None), "NULL")

    def test_string_is_quoted_and_escaped(self):
        self.assertEqual(g.render_value({"name": "x", "type": "VARCHAR(10)"}, "a'b"), "'a''b'")

    def test_unsupported_type_is_rejected(self):
        with self.assertRaises(Exception):
            g.render_value({"name": "x", "type": "VARCHAR(10)"}, object())


class IsolationTest(unittest.TestCase):
    """三重隔离：租户号 / DEMO_ 前缀 / creator=demo-seed。"""

    @classmethod
    def setUpClass(cls):
        snap = _load_snapshot()
        cls.dataset = g.build_dataset(snap, 20260928, 9001, "small")
        cls.snapshot = snap

    def test_every_row_belongs_to_demo_tenant(self):
        for item in self.dataset:
            for row in item["rows"]:
                self.assertEqual(row.get("tenant_id"), 9001, item["table"])

    def test_every_row_has_demo_creator(self):
        for item in self.dataset:
            for row in item["rows"]:
                self.assertEqual(row.get("creator"), "demo-seed", item["table"])
                self.assertEqual(row.get("create_time"), g.FIXED_TIME, item["table"])
                self.assertEqual(row.get("update_time"), g.FIXED_TIME, item["table"])

    def test_name_and_code_are_prefixed(self):
        for item in self.dataset:
            for row in item["rows"]:
                for col in ("name", "code", "emp_no", "plate_no"):
                    value = row.get(col)
                    if isinstance(value, str):
                        self.assertTrue(value.startswith("DEMO"),
                                        f"{item['table']}.{col}={value!r} 缺少 DEMO 前缀")

    def test_no_real_pii(self):
        for item in self.dataset:
            for row in item["rows"]:
                email = row.get("email")
                if isinstance(email, str):
                    self.assertTrue(email.endswith("@zhicloud.demo.invalid"),
                                    f"{item['table']} 邮箱域名非保留域: {email}")
                if "id_card" in row:
                    self.assertIsNone(row.get("id_card"), "不得生成证件号")

    def test_not_null_columns_are_all_populated(self):
        for item in self.dataset:
            cols = g.column_index(self.snapshot, item["table"])
            required = [n for n, c in cols.items() if c.get("nullable") is False]
            for row in item["rows"]:
                missing = [n for n in required if row.get(n) is None]
                self.assertEqual(missing, [], f"{item['table']} NOT NULL 列为空: {missing}")

    def test_primary_keys_are_unique_globally(self):
        seen = {}
        for item in self.dataset:
            for row in item["rows"]:
                key = row["id"]
                self.assertNotIn(key, seen, f"主键撞号 {key}: {seen.get(key)} vs {item['table']}")
                seen[key] = item["table"]

    def test_tenant_stride_separates_tenants(self):
        snap = _load_snapshot()
        a = g.build_dataset(snap, 20260928, 9001, "small")
        b = g.build_dataset(snap, 20260928, 9002, "small")
        ids_a = {row["id"] for item in a for row in item["rows"]}
        ids_b = {row["id"] for item in b for row in item["rows"]}
        self.assertEqual(ids_a & ids_b, set(), "跨租户主键区间重叠")
        first_b = min(ids_b)
        first_a = min(ids_a)
        self.assertEqual(first_b - first_a, g.ID_TENANT_STRIDE)

    def test_scale_changes_row_count(self):
        snap = _load_snapshot()
        small = g.build_dataset(snap, 20260928, 9001, "small")
        medium = g.build_dataset(snap, 20260928, 9001, "medium")
        rows_small = sum(len(i["rows"]) for i in small)
        rows_medium = sum(len(i["rows"]) for i in medium)
        self.assertGreater(rows_medium, rows_small)


class SqlArtifactTest(unittest.TestCase):
    """已落盘的 SQL 产物必须自洽（不校验可执行性，只校验静态约定）。"""

    @classmethod
    def setUpClass(cls):
        if not SQL_PATH.is_file():
            raise unittest.SkipTest(f"SQL 产物不存在: {SQL_PATH}")
        cls.text = SQL_PATH.read_text(encoding="utf-8")

    def test_wrapped_in_transaction(self):
        self.assertIn("BEGIN;", self.text)
        self.assertIn("COMMIT;", self.text)

    def test_forbids_production_import_in_header(self):
        self.assertIn("【禁止导入生产】", self.text)
        self.assertIn("【未实库执行】", self.text)

    def test_renders_bit_literal_for_bit_tables(self):
        # erp_product.deleted 是 BIT(1)，必须是 b'0' 而不是 0
        self.assertIn("INSERT INTO `erp_product`", self.text)
        for line in self.text.splitlines():
            if line.startswith("INSERT INTO `erp_product` "):
                self.assertIn("b'0'", line)
                self.assertNotIn("`deleted`) VALUES (0)", line)

    def test_renders_numeric_literal_for_tinyint_tables(self):
        for line in self.text.splitlines():
            if line.startswith("INSERT INTO `wms_location` "):
                # wms_location 的列序以 (.., deleted, tenant_id) 结尾，deleted 为 TINYINT(1) => 数字 0
                self.assertTrue(line.rstrip().endswith(", 0, 9001);"), line[-80:])

    def test_only_known_tables_are_touched(self):
        tables = {line.split("`")[1] for line in self.text.splitlines()
                  if line.startswith("INSERT INTO `")}
        self.assertTrue(tables, "未找到任何 INSERT 语句")
        unknown = sorted(tables - set(g.TABLE_ORDER))
        self.assertEqual(unknown, [], f"SQL 中出现未登记的表: {unknown}")

    def test_manifest_matches_sql_table_set(self):
        if not SUMMARY_PATH.is_file():
            self.skipTest(f"manifest 不存在: {SUMMARY_PATH}")
        import json
        manifest = json.loads(SUMMARY_PATH.read_text(encoding="utf-8"))
        manifest_tables = {t["table"] for t in manifest["tables"]}
        sql_tables = {line.split("`")[1] for line in self.text.splitlines()
                      if line.startswith("INSERT INTO `")}
        self.assertEqual(sql_tables, manifest_tables)
        sql_rows = sum(1 for line in self.text.splitlines() if line.startswith("INSERT INTO `"))
        self.assertEqual(sql_rows, manifest["row_total"])


if __name__ == "__main__":
    unittest.main()