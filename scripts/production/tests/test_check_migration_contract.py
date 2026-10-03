"""迁移契约 CI 门禁的回归测试。

契约目标：V90+ 每个已审核迁移与只读核验 SQL 的内容哈希必须固定；
任何未登记、重复版本、内容漂移或核验文件缺失都必须在 CI 失败。
"""

import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import check_migration_contract as contract  # noqa: E402


def write(path: Path, text: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")
    return path


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class MigrationContractTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.migrations = self.root / "migrations"
        self.verification = self.root / "verification"
        self.migration = write(
            self.migrations / "V90__demo.sql",
            "SELECT 1;\n",
        )
        self.verify_sql = write(
            self.verification / "V90_verify.sql",
            "SELECT 1 AS ok;\n",
        )
        self.manifest = write(
            self.root / "contract.json",
            json.dumps({
                "schema_version": 1,
                "contract_start_version": 90,
                "migrations": [{
                    "version": 90,
                    "file": "V90__demo.sql",
                    "sha256": sha256(self.migration),
                    "verification_file": "V90_verify.sql",
                    "verification_sha256": sha256(self.verify_sql),
                }],
            }, ensure_ascii=False, indent=2) + "\n",
        )

    def tearDown(self):
        self.tmp.cleanup()

    def report(self):
        return contract.build_report(
            self.migrations, self.verification, self.manifest,
        )

    def test_matching_contract_passes(self):
        report = self.report()
        self.assertEqual(report["status"], "passed", report["findings"])
        self.assertEqual(report["tracked_migrations"], 1)

    def test_crlf_verification_matches_lf_contract(self):
        self.verify_sql.write_bytes(b"SELECT 1 AS ok;\r\n")
        report = self.report()
        self.assertEqual(report["status"], "passed", report["findings"])

    def test_migration_hash_remains_byte_exact(self):
        self.migration.write_bytes(b"SELECT 1;\r\n")
        report = self.report()
        self.assertEqual(report["status"], "failed")
        self.assertIn(
            "C1",
            {finding["rule"] for finding in report["findings"]},
        )

    def test_modified_migration_fails_checksum_contract(self):
        write(self.migration, "SELECT 2;\n")
        report = self.report()
        self.assertEqual(report["status"], "failed")
        self.assertIn(
            "C1",
            {finding["rule"] for finding in report["findings"]},
        )

    def test_untracked_migration_fails(self):
        write(self.migrations / "V91__new.sql", "SELECT 1;\n")
        report = self.report()
        self.assertEqual(report["status"], "failed")
        self.assertIn(
            "C2",
            {finding["rule"] for finding in report["findings"]},
        )

    def test_duplicate_version_fails(self):
        write(self.migrations / "V90__other.sql", "SELECT 1;\n")
        report = self.report()
        self.assertEqual(report["status"], "failed")
        self.assertIn(
            "C3",
            {finding["rule"] for finding in report["findings"]},
        )

    def test_manifest_version_must_match_filename(self):
        data = json.loads(self.manifest.read_text(encoding="utf-8"))
        data["migrations"][0]["version"] = 91
        self.manifest.write_text(
            json.dumps(data, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        report = self.report()
        self.assertEqual(report["status"], "failed")
        self.assertIn(
            "C6",
            {finding["rule"] for finding in report["findings"]},
        )
    def test_tracked_migration_must_pass_lint(self):
        write(self.migration, "SELECT 1;")
        data = json.loads(self.manifest.read_text(encoding="utf-8"))
        data["migrations"][0]["sha256"] = sha256(self.migration)
        self.manifest.write_text(
            json.dumps(data, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        report = self.report()
        self.assertEqual(report["status"], "failed")
        self.assertIn(
            "L1",
            {finding["rule"] for finding in report["findings"]},
        )
    def test_duplicate_dotted_version_fails(self):
        write(self.migrations / "V1.1__first.sql", "SELECT 1;\n")
        write(self.migrations / "V1.1__second.sql", "SELECT 1;\n")
        report = self.report()
        self.assertEqual(report["status"], "failed")
        self.assertIn(
            "C3",
            {finding["rule"] for finding in report["findings"]},
        )
    def test_missing_verification_sql_fails(self):
        self.verify_sql.unlink()
        report = self.report()
        self.assertEqual(report["status"], "failed")
        self.assertIn(
            "C4",
            {finding["rule"] for finding in report["findings"]},
        )


if __name__ == "__main__":
    unittest.main()