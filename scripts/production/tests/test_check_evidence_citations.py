# -*- coding: utf-8 -*-
"""证据引用一致性门禁（check_evidence_citations.py，R-050）的守卫测试。

规矩是 §3.4 立的："凡被文档引用的证据，引用处必须内联六要素"。这条一直靠人守，
本轮就手工抓到两处失守（一条生成命令按现行 CLI 跑不动、一处把 jar 构建实例哈希当代码身份）。
所以每个判定类别都必须由合成夹具**逼红一次**，而不是拿真树"今天恰好绿"当证据。
"""
from __future__ import annotations

import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
REPO = SCRIPTS.parent.parent
sys.path.insert(0, str(SCRIPTS))

import check_evidence_citations as gate  # noqa: E402

SHA_A = hashlib.sha256(b"payload-a").hexdigest()
SHA_B = hashlib.sha256(b"other").hexdigest()


class Harness(unittest.TestCase):
    def setUp(self):
        self.base = Path(tempfile.mkdtemp(prefix="zc-cite-"))
        self.docs = self.base / "docs"
        self.ev = self.docs / "evidence" / "production"
        self.ev.mkdir(parents=True)
        self.repo = self.base

    def tearDown(self):
        import shutil
        shutil.rmtree(self.base, ignore_errors=True)

    def evidence(self, name, payload=b"payload-a"):
        f = self.ev / name
        f.write_bytes(payload)
        return f

    def doc(self, name, lines):
        f = self.docs / name
        f.write_text("\n".join(lines) + "\n", encoding="utf-8")
        return f

    def evaluate(self, doc_files, tracked=()):
        return gate.evaluate(doc_files, set(tracked), self.docs, self.repo)

    def kinds(self, report):
        return [v["kind"] for v in report["violations"]]


class CategoryTests(Harness):
    def test_matching_citation_is_clean(self):
        self.evidence("a.json")
        d = self.doc("00.md", [f"证据 `evidence/production/a.json`（SHA-256 `{SHA_A}`，已在版本库）"])
        report = self.evaluate([d], tracked={f.relative_to(self.repo).as_posix()
                                             for f in (self.ev / "a.json",)})
        rel = "docs/evidence/production/a.json"
        self.assertEqual([], [v for v in report["violations"] + report["advisories"]
                             if v.get("path") == rel],
                         json_dumps(report["violations"]))
        self.assertEqual(1, report["coverage"]["hash_citations_verified_match"])

    def test_missing_path_is_reported(self):
        d = self.doc("00.md", ["引用 `evidence/production/gone.json` 但文件不存在"])
        report = self.evaluate([d])
        self.assertIn("missing-path", self.kinds(report), json_dumps(report["violations"]))

    def test_unattributable_hash_is_reported_with_identity_variants(self):
        f = self.evidence("b.json", payload=b"changed-by-regeneration")
        d = self.doc("00.md", [f"`evidence/production/b.json` SHA-256 `{SHA_A}`"])
        report = self.evaluate([d], tracked={f.relative_to(self.repo).as_posix()})
        drift = [v for v in report["violations"] if v["kind"] == "hash-unattributable"]
        self.assertEqual(1, len(drift), json_dumps(report["violations"])[:300])
        # 违规条目里的身份哈希是按 12 位前缀展示的（避免把长哈希塞满日志），比较时同样取前缀
        self.assertEqual(hashlib.sha256(f.read_bytes()).hexdigest()[:12],
                         drift[0]["identities"]["bytes_sha256"])
        self.assertIn(SHA_A, drift[0]["cited"])

    def test_untracked_evidence_is_reported_as_dangling_for_others(self):
        # §3.4 的已知代价：证据不入库，所以干净检出的人拿不到被引用的文件。
        # 这不是"我错了"，但必须**量出来**，不能停留在文档里的一句说明。
        f = self.evidence("c.json")
        d = self.doc("00.md", [f"`evidence/production/c.json`（SHA-256 `{SHA_A}`）"])
        report = self.evaluate([d])   # tracked 里故意不放
        hit = [v for v in report["advisories"] if v["kind"] == "untracked-but-present"]
        self.assertEqual(1, len(hit), json_dumps(report["advisories"])[:300])
        self.assertTrue(hit[0]["path"].endswith("c.json"))
        # 默认必须是 advisory：证据不入库是 §3.4 的既定代价，若算作违规，门禁永远红、红到没人看，
        # 那就等于没有门禁。而 --strict 时必须升格为阻断（提交冻结阶段的真实缺陷）。
        self.assertNotIn("untracked-but-present", [v["kind"] for v in report["violations"]])
        strict = gate.evaluate([d], set(), self.docs, self.repo, True)
        self.assertIn("untracked-but-present", [v["kind"] for v in strict["violations"]],
                      json_dumps(strict["violations"])[:300])

    def test_two_citations_on_one_line_pair_to_the_nearest_path(self):
        # 交叉错配会把"各自都对"的两条引用判成两条 drift —— 第一版就是这样，真树误报 6 条里有多条属此类。
        f1 = self.evidence("one.json")
        self.evidence("two.json", payload=b"other")
        d = self.doc("00.md", [
            f"见 `evidence/production/one.json`（`{SHA_A}`）与 `evidence/production/two.json`（`{SHA_B}`）"])
        report = self.evaluate([d], tracked={"docs/evidence/production/one.json",
                                             "docs/evidence/production/two.json"})
        self.assertNotIn("hash-unattributable", self.kinds(report), json_dumps(report["violations"])[:400])

    def test_directory_mentions_are_not_file_citations(self):
        d = self.doc("00.md", ["`evidence/production/**` 下的执行证据不入版本库"])
        report = self.evaluate([d])
        self.assertNotIn("missing-path", self.kinds(report), json_dumps(report["violations"])[:300])
        self.assertEqual(0, report["coverage"]["directory_mentions"],
                         "带通配的碎片既不该记成目录引用，也不该进可核对分母")

    def test_coverage_floor_fails_loudly_when_the_doc_face_shrinks(self):
        # 只喂一份文档：门槛判定必须响，否则"什么都没扫到"会被读成"全部一致"。
        d = self.doc("00.md", ["x"])
        report = self.evaluate([d])
        self.assertIn("coverage-too-small", self.kinds(report), json_dumps(report["violations"])[:300])

    def test_payload_hash_and_sibling_owner_are_not_called_drift(self):
        """本项目一份证据可以同时有"字节身份"和"内容身份"两个 SHA（README 明确分开记录）。

        第一版只比字节哈希，于是把 `payload_sha256` 的正确引用、以及"哈希其实属于同一行/同目录
        另一份产物"的省略式写法全部判成 drift —— 6 条里有 4 条是这种误判（真树实测）。
        """
        payload = json.dumps({"payload_sha256": SHA_A, "rows": 3}).encode()
        f = self.evidence("snap.json", payload=payload)
        d = self.doc("00.md", [f"快照 `evidence/production/snap.json`（payload_sha256 `{SHA_A}`）"])
        report = self.evaluate([d], tracked={"docs/evidence/production/snap.json"})
        self.assertNotIn("hash-unattributable", self.kinds(report), json_dumps(report["violations"])[:300])

        self.evidence("note.md", payload=b"other")
        self.evidence("note.json", payload=b"payload-a")
        d2 = self.doc("01.md", [f"读 `evidence/production/note.md`（SHA-256 `{SHA_A}`，即 JSON 的字节哈希）"])
        report2 = self.evaluate([d2], tracked={"docs/evidence/production/note.md",
                                               "docs/evidence/production/note.json"})
        kinds2 = [v["kind"] for v in report2["advisories"]]
        self.assertIn("sibling-hash-mismatch", kinds2, json_dumps(report2["advisories"])[:300])
        self.assertNotIn("hash-unattributable", [v["kind"] for v in report2["violations"]],
                         "兄弟产物命中却判违规：把我方写法判成缺陷，门禁会被人关掉")

    def test_template_paths_with_angle_brackets_are_not_files(self):
        d = self.doc("02.md", ["命令样例 `evidence/production/security-baseline-<时间戳>.json --summary`"])
        report = self.evaluate([d])
        self.assertNotIn("missing-path", self.kinds(report), json_dumps(report["violations"])[:300])


class RealTreeTests(unittest.TestCase):
    def test_repo_docs_are_scanned_and_findings_are_classified(self):
        if not gate.DOCS_ROOT.is_dir():
            self.fail("文档根不存在：扫不到不等于都对")
        docs = sorted(p for p in gate.DOCS_ROOT.rglob("*.md") if p.is_file())
        tracked = {l.replace("\\", "/") for l in gate.git_lines("ls-files")}
        report = gate.evaluate(docs, tracked, gate.DOCS_ROOT, gate.REPO)
        c = report["coverage"]
        self.assertGreaterEqual(c["docs_scanned"], gate.MIN_DOCS, c)
        self.assertGreaterEqual(c["path_citations"], gate.MIN_PATH_CITATIONS, c)
        # 判定必须真的在算，而不是把所有引用都塞进某一类
        self.assertGreater(c["hash_citations_verified_match"], 20, c)
        # 恒等式：可核对引用 = 解析成功 + 报缺失 + 目录级剔除。
        # 少了最后一项就会出现"引用被静默丢掉、门禁变绿"的情况（本轮真实踩过）。
        self.assertEqual(c["path_citations"], c["paths_resolved"]
                         + len([v for v in report["violations"] if v["kind"] == "missing-path"])
                         + c.get("directory_citations_dropped", 0),
                         "路径计数不闭合：有引用既没解析成功、也没报缺失、也没记成目录剔除")


    def test_coverage_shrink_guard_trips_on_baseline_drop_and_clears_when_restored(self):
        """绝对基线检的是"判定面自己缩水"，比例式恒等式检不到（上一轮真的缩了 29 条而全绿）。

        两个方向都要断言：抬高基线必须响；恢复后必须不响 —— 后者防止判定被写成恒真。
        """
        docs = sorted(p for p in gate.DOCS_ROOT.rglob("*.md") if p.is_file())
        tracked = {l.replace("\\", "/") for l in gate.git_lines("ls-files")}
        original = dict(gate.BASELINE)
        try:
            gate.BASELINE.update({"docs_scanned": original["docs_scanned"] * 100,
                                  "path_citations": original["path_citations"] * 100,
                                  "dangling_advisories": original["dangling_advisories"] * 100})
            tripped = gate.evaluate(docs, tracked, gate.DOCS_ROOT, gate.REPO)
            metrics = sorted({v["metric"] for v in tripped["violations"] if v["kind"] == "coverage-shrink"})
            self.assertEqual(["dangling_advisories", "docs_scanned", "path_citations"], metrics,
                             json_dumps(tripped["violations"])[:400])
        finally:
            gate.BASELINE.clear()
            gate.BASELINE.update(original)
        clean = gate.evaluate(docs, tracked, gate.DOCS_ROOT, gate.REPO)
        self.assertEqual([], [v for v in clean["violations"] if v["kind"] == "coverage-shrink"],
                         "恢复基线后仍报缩水：这条判定与实际数据无关，是恒真的")
        self.assertEqual(original, clean["coverage"]["baseline"], "报告没回显基线，读者无法判断以哪个数为准")

    def test_coverage_shrink_is_measured_not_ratio_self_reference(self):
        """恒等式在分母塌陷时照样成立 —— 这条把这个事实钉成断言，免得以后再误以为它够用。"""
        c = {"docs_scanned": 1, "path_citations": 0, "paths_resolved": 0,
             "directory_citations_dropped": 0, "dangling_reference_advisories": 0}
        self.assertEqual(c["path_citations"],
                         c["paths_resolved"] + c["directory_citations_dropped"])
        shrunk = gate.coverage_shrinkage(dict(c, docs_scanned=1, path_citations=0), 0)
        self.assertTrue(shrunk, "分母归零却没报缩水：说明基线检没接上")

def json_dumps(obj):
    import json
    return json.dumps(obj, ensure_ascii=False)


if __name__ == "__main__":
    unittest.main()
