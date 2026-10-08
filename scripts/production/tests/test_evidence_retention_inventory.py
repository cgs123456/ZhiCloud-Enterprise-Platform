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

    def test_tracked_baseline_never_a_candidate(self):
        """已入库冻结基线（刻意保留）即使老且无引用也绝不进候选。

        这是 §12.148 的口径：400 个 tracked 文件是 `.gitignore:107-109` 刻意入库的
        审计基线，不是垃圾；把它们当回收候选就是"把入库证据当过期文件删"。
        用 monkeypatch 注入 tracked 集合，避免测试里真去建 git 仓库。
        """
        tracked_rel = "docs/enterprise-platform/evidence/production/old_orphan.json"
        orig = inv.collect_tracked
        inv.collect_tracked = lambda repo_root, ev_dir: {tracked_rel}
        try:
            buf = io.StringIO(); old = sys.stdout; sys.stdout = buf
            try:
                inv.main(["--retention-days", "0", "--repo", str(self.repo), "--json"])
            finally:
                sys.stdout = old
        finally:
            inv.collect_tracked = orig
        rep = json.loads(buf.getvalue())
        self.assertEqual(1, rep["git_tracked_baseline"])
        self.assertFalse(any(c["path"].endswith("old_orphan.json")
                             for c in rep["recycle_candidates"]),
                         "已入库基线不得进回收候选")
        # 未入库的新孤儿仍应候选（tracked 判定只影响被标记的那一个）
        self.assertTrue(any(c["path"].endswith("new_orphan.json")
                            for c in rep["recycle_candidates"]))

    def test_retention_below_age_span_is_flagged(self):
        """保留期短于证据年龄跨度时必须显式告警——防止"任意阈值即结论"。"""
        buf = io.StringIO(); old = sys.stdout; sys.stdout = buf
        try:
            inv.main(["--retention-days", "3", "--repo", str(self.repo), "--json"])
        finally:
            sys.stdout = old
        rep = json.loads(buf.getvalue())
        # 合成数据里最老 30 天、最新 1 天 → 跨度 29 天 > 3
        self.assertTrue(rep["retention_below_age_span"])
        self.assertGreaterEqual(rep["age_span_days"], 3)

    def test_collect_tracked_degrades_on_non_repo(self):
        """非 git 目录不得抛异常（CI 裸检出/临时目录）：返回空集而非崩溃。"""
        self.assertEqual(set(), inv.collect_tracked(self.repo, self.ev))


if __name__ == "__main__":
    unittest.main()
