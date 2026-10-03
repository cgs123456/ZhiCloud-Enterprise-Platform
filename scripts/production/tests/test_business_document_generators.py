"""业务单据生成器的回归测试。

边界：只读 schema/主数据/静态产物，不连接数据库；所有结果仍为 STATIC_ONLY。
"""

import json
import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
REPO_ROOT = TESTS_DIR.parents[2]
GENERATORS_DIR = REPO_ROOT / "demo-data" / "generators"
sys.path.insert(0, str(GENERATORS_DIR))

import generate_business_documents as g  # noqa: E402

SNAPSHOT_PATH = REPO_ROOT / "demo-data" / "schema" / "schema-snapshot.json"
MASTER_PATH = REPO_ROOT / "demo-data" / "schema" / "demo_master_data.json"
SQL_PATH = REPO_ROOT / "demo-data" / "sql" / "mysql" / "demo_business_documents.sql"
JSON_PATH = REPO_ROOT / "demo-data" / "schema" / "demo_business_documents.json"
SUMMARY_PATH = REPO_ROOT / "demo-data" / "schema" / "demo_business_documents_summary.json"


def _load_inputs():
    if not SNAPSHOT_PATH.is_file():
        raise unittest.SkipTest(f"schema 快照不存在: {SNAPSHOT_PATH}")
    if not MASTER_PATH.is_file():
        raise unittest.SkipTest(f"主数据 JSON 不存在: {MASTER_PATH}")
    snapshot = g.load_snapshot(SNAPSHOT_PATH)
    master = g.MasterData(MASTER_PATH)
    return snapshot, master


class FailClosedInputTest(unittest.TestCase):
    def test_master_tenant_mismatch_is_rejected(self):
        snapshot, master = _load_inputs()
        with self.assertRaises(g.FailClosedError):
            g.build_dataset(snapshot, g.DEFAULT_SEED, 9002, "small", master)

    def test_unknown_scale_is_rejected_as_fail_closed(self):
        snapshot, master = _load_inputs()
        with self.assertRaises(g.FailClosedError):
            g.build_dataset(snapshot, g.DEFAULT_SEED, g.DEFAULT_TENANT_ID, "huge", master)

    def test_not_null_none_is_rejected(self):
        snapshot, _ = _load_inputs()
        cols = g.column_index(snapshot, "erp_product")
        row = {name: None for name, col in cols.items() if col.get("nullable") is False}
        with self.assertRaises(g.FailClosedError):
            g.validate_rows("erp_product", cols, [row])


    def test_varchar_overflow_is_rejected(self):
        snapshot, _ = _load_inputs()
        cols = g.column_index(snapshot, "erp_product")
        row = {
            "id": 1,
            "category_id": 1,
            "unit_id": 1,
            "code": "X" * 65,
            "name": "DEMO_产品",
        }
        with self.assertRaises(g.FailClosedError):
            g.validate_rows("erp_product", cols, [row])

    def test_wrong_scalar_type_is_rejected(self):
        snapshot, _ = _load_inputs()
        cols = g.column_index(snapshot, "erp_product")
        row = {
            "id": 1,
            "category_id": 1,
            "unit_id": 1,
            "code": 123,
            "name": "DEMO_产品",
        }
        with self.assertRaises(g.FailClosedError):
            g.validate_rows("erp_product", cols, [row])

    def test_default_dataset_builds_without_contract_regression(self):
        snapshot, master = _load_inputs()
        dataset = g.build_dataset(snapshot, g.DEFAULT_SEED, g.DEFAULT_TENANT_ID, "small", master)
        self.assertEqual(99, len(dataset))
        self.assertEqual(2458, sum(len(item["rows"]) for item in dataset))


if __name__ == "__main__":
    unittest.main()