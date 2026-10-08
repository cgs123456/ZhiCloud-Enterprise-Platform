# -*- coding: utf-8 -*-
"""evidence_retention_inventory 的只读盘点行为测试（R-G3.5-4 机械部分）。

用合成仓库+临时证据目录，绝不碰真实 evidence/production、也绝不删任何文件——
本脚本的核心契约就是"只读"，测试要能证伪它（若哪天它偷删，这里变红）。
"""
import io
import json
import os
import sys
import tempfile
import unittest
from pathlib import Path
from datetime import datetime, timedelta

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import evidence_retention_inventory as inv  # noqa: E402


def _mk_file(path: Path, *, age_days: int, content="x"):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    t = (datetime.now() - timedelta(days=age_days)).timestamp()
    os.utime(path, (t, t))


class InventoryTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        repo = Path(self.tmp.name)
        self.repo = repo
        ev = repo / "docs" / "enterprise-platform" / "evidence" / "production"
        docs = repo / "docs" / "enterprise-platform"
        self.ev, self.docs = ev, docs
        # 老 + 无引用 → 候选；新 + 无引用 → 未清；被引用（任意年龄）→ 不候选
        _mk_file(ev / "old_orphan.json", age_days=30)
        _mk_file(ev / "new_orphan.json", age_days=1)
        _mk_file(ev / "referenced_old.json", age_days=30)
        _mk_file(ev / "referenced_old.json.sha256", age_days=30)
        # 一份文档引用其中两条（相对 docs 根的路径 token，由 cite 解析）
        doc = docs / "00-README.md"
        doc.write_text(
            "见 evidence/production/referenced_old.json 与 "
            "evidence/production/referenced_old.json.sha256\n", encoding="utf-8")

    def tearDown(self):
        self.tmp.cleanup()

    def test_reads_only_and_classifies(self):
        before = {p.name for p in self.ev.iterdir()}
        # 直接调 main 捕获 stdout
        buf = io.StringIO()
        old = sys.stdout
        sys.stdout = buf
        try:
            rc = inv.main(["--retention-days", "14", "--repo", str(self.repo), "--json"])
        finally:
            sys.stdout = old
        self.assertEqual(0, rc)
        rep = json.loads(buf.getvalue())
        # 只读：文件一个没少
        after = {p.name for p in self.ev.iterdir()}
        self.assertEqual(before, after, "盘点必须只读，不得删除任何证据文件")
        # 老且无引用 = 唯一候选
        cands = {c["path"] for c in rep["recycle_candidates"]}
        self.assertTrue(any(c.endswith("old_orphan.json") for c in cands), cands)
        # 被引用的文件（json + sha256）绝不进候选
        self.assertFalse(any("referenced_old" in c for c in cands), cands)
        self.assertFalse(any(c.endswith("new_orphan.json") for c in cands), "新文件不得进候选")
        self.assertEqual(1, rep["recycle_candidates_count"])
        self.assertEqual(2, rep["referenced_files"])  # json + sha256

    def test_zero_retention_still_never_deletes(self):
        # retention=0 时新文件也算"超期"，但仍无引用才候选；被引用的永不候选
        buf = io.StringIO(); old = sys.stdout; sys.stdout = buf
        try:
            inv.main(["--retention-days", "0", "--repo", str(self.repo), "--json"])
        finally:
            sys.stdout = old
        rep = json.loads(buf.getvalue())
        self.assertTrue(any(c["path"].endswith("new_orphan.json") for c in rep["recycle_candidates"]))
        self.assertFalse(any("referenced" in c["path"] for c in rep["recycle_candidates"]))
        self.assertEqual(4, len(list(self.ev.iterdir())))  # 仍没删


if __name__ == "__main__":
    unittest.main()
