# -*- coding: utf-8 -*-
"""证据面凭据门禁（check_evidence_credentials.py，R-046）的守卫测试。

本仓的证据文件按 §3.4 **不入库**，所以 `security_baseline_scan.py`（只扫已跟踪文件）对这一面是零覆盖；
R-044 又实测到历史 `.command.txt` 里存着明文库口令。这里要的是"下一次再有人往证据里塞口令会报警"，
因此每个判定都必须被一份合成夹具**逼红过一次**才算存在，而不是靠"真树今天恰好是 0"。
"""
from __future__ import annotations

import hashlib
import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
REPO = SCRIPTS.parent.parent
sys.path.insert(0, str(SCRIPTS))

import check_evidence_credentials as gate  # noqa: E402

# 历史 R-044 的形状：10 位混合 alnum 随机口令
LEGACY_SECRET = "Ab3dEf5gHi"
# native 沙箱实际生成的形状：zc + 24 hex
NATIVE_SECRET = "zc" + "0123456789abcdef01234567"


class Fixture(unittest.TestCase):
    def setUp(self):
        self.base = Path(tempfile.mkdtemp(prefix="zc-credgate-"))
        self.ev = self.base / "evidence"
        (self.ev / "production").mkdir(parents=True)
        self.cred = self.base / "temp"
        self.cred.mkdir()

    def tearDown(self):
        shutil.rmtree(self.base, ignore_errors=True)

    def write(self, rel, text):
        target = self.ev / "production" / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")
        return target

    def run_gate(self, allowlist=()):
        al = self.base / "allow.json"
        al.write_text(json.dumps(list(allowlist), ensure_ascii=False), encoding="utf-8")
        return gate.evaluate(self.ev, self.cred, gate.load_allowlist(al))

    def layers_for(self, report, path):
        # 聚合后的违规条目用 "layers"（列表），空条目/漂移用 "layer"（单值），两种都要能读。
        out = []
        for v in report["violations"]:
            if v.get("path") != path:
                continue
            out.extend(v.get("layers") or [v.get("layer", "")])
        return sorted(set(out))


class DetectorTests(Fixture):
    def test_historical_command_line_pwd_shape_is_caught(self):
        # R-044 的实际形状：口令在命令行中间，既不是行首也没有引号。
        self.write("a.command.txt",
                   f"docker exec -e MYSQL_PWD={LEGACY_SECRET} c-mysql mysql -uroot --batch -e 'SELECT 1'\n")
        report = self.run_gate()
        self.assertIn("key-value", self.layers_for(report, "production/a.command.txt"),
                      json.dumps(report["violations"], ensure_ascii=False)[:400])

    def test_bare_assignment_and_mysql_dash_p_shapes_are_caught(self):
        self.write("b.env", f"MYSQL_ROOT_PASSWORD={NATIVE_SECRET}\n")
        self.write("c.log", f"$ mysql -h 127.0.0.1 -uroot -p{NATIVE_SECRET} -e 'SHOW DATABASES'\n")
        report = self.run_gate()
        self.assertIn("bare-assign", self.layers_for(report, "production/b.env"))
        self.assertIn("mysql-dash-p", self.layers_for(report, "production/c.log"))

    def test_arbitrary_sandbox_dir_name_is_still_discovered(self):
        """凭据落点必须容忍调用方给 BaseDir 随便取名。

        2026-10-02 用真活实例试出来的缺陷：`native_mysqld_sandbox.ps1` 的 BaseDir 是参数，
        当轮取名 `zc-livecred-<date>` 目录里的 `.pw`，而默认 glob 只有 `zhicloud-native-*/.pw`、`zc-native-*/.pw`
        两族 —— 机器上明明有活口令，默认 TEMP 根却探到 0 个凭据，
        于是第四层报 not-exercised、门禁 exit 0，恰好在它最该生效的场景下假绿。
        修法是补深度 1 的 `*/.pw`、`*/state.json`；这条把它钉住。
        """
        odd = self.cred / "any-name-I-wanted-20261002"
        odd.mkdir()
        (odd / ".pw").write_text(LEGACY_SECRET, encoding="ascii", newline="")
        found = [p.name for p in gate.credential_candidates(self.cred)]
        self.assertIn(".pw", found, [str(p) for p in gate.credential_candidates(self.cred)])
        loaded, probed = gate.collect_live_credentials(self.cred)
        self.assertGreaterEqual(probed, 1)
        self.assertTrue(any(c["_value"] == LEGACY_SECRET for c in loaded),
                        "探到了文件却没读出凭据：collect_live_credentials 的形状判定失效")

    def test_live_credential_is_matched_even_without_any_key_name(self):
        # 第四层不依赖形状：从沙箱凭据面读出真实值，再逐字节找。
        state_dir = self.cred / "zhicloud-mysql-sandbox"
        state_dir.mkdir()
        (state_dir / "state.json").write_text(
            json.dumps({"container_name": "zhicloud-mysql-sandbox-deadbeef",
                        "root_password": LEGACY_SECRET}), encoding="utf-8")
        self.write("d.txt", f"tail of a log … connection string was {LEGACY_SECRET} and then it ended\n")
        report = self.run_gate()
        self.assertEqual(1, report["coverage"]["live_credentials_loaded"], report["coverage"])
        self.assertIn("live-credential", self.layers_for(report, "production/d.txt"))
        # 报告里绝不能反过来抄一份明文
        blob = json.dumps(report, ensure_ascii=False)
        self.assertNotIn(LEGACY_SECRET, blob, "报告里出现了口令明文")

    def test_placeholders_and_references_are_not_reported(self):
        # 这一条反过来证明判定不是"见到 password 就红"：那样第一版在真树上会报 65 份。
        digest = hashlib.sha256(b"1").hexdigest()
        self.write("e.json", json.dumps({
            "root_password": "<redacted>",
            "note": "password: ${NACOS_PASSWORD} is injected by the chart",
            "weak_example": "MYSQL_PASSWORD=123456 is a documented weak demo value, not a sandbox secret",
        }))
        self.write("f.yaml", f"imagePullSecrets:\n  - name: zhicloud-app-secret\nsecret: {digest}\n")
        report = self.run_gate()
        self.assertEqual([], report["violations"], json.dumps(report["violations"], ensure_ascii=False)[:500])
        self.assertGreaterEqual(report["coverage"]["files_scanned"], 2)


class AllowlistTests(Fixture):
    def setUp(self):
        super().setUp()
        self.target = self.write("g.command.txt", f"export MYSQL_PWD={LEGACY_SECRET}\n")
        self.sha = hashlib.sha256(self.target.read_bytes()).hexdigest()

    def _entry(self, sha):
        return {"path": "production/g.command.txt", "sha256": sha,
                "reason": "历史证据，沙箱已销毁，台账锚定不回改", "decided_by": "责任人"}

    def test_registered_entry_with_matching_hash_accepts_the_hit(self):
        report = self.run_gate([self._entry(self.sha)])
        self.assertEqual([], report["violations"], json.dumps(report["violations"])[:300])
        self.assertEqual(1, report["totals"]["accepted"])

    def test_hash_drift_is_a_violation_not_a_silent_accept(self):
        # 登记之后文件又被改过 → 锚点失效，必须重新判定而不是沿用旧授权。
        self.target.write_text(f"export MYSQL_PWD={NATIVE_SECRET}\n", encoding="utf-8")
        report = self.run_gate([self._entry(self.sha)])
        kinds = [v["layer"] for v in report["violations"]]
        self.assertIn("allowlist-drift", kinds, json.dumps(report["violations"])[:300])

    def test_dead_entry_is_a_violation(self):
        # 登记了但当前没有命中：要么文件没了，要么判定变了 —— 空条目必须响。
        report = self.run_gate([{
            "path": "production/gone-long-ago.command.txt",
            "sha256": hashlib.sha256(b"deleted").hexdigest(),
            "reason": "登记指向一个已经不存在的文件", "decided_by": "责任人"}])
        layers = [v.get("layer") for v in report["violations"]]
        self.assertIn("dead-allowlist-entry", layers, json.dumps(report["violations"])[:300])
        # 同时 g.command.txt 的真实命中仍在（没被那条空登记顺手盖掉）
        self.assertIn("key-value", self.layers_for(report, "production/g.command.txt"))


class RealTreeTests(unittest.TestCase):
    def test_current_evidence_tree_is_scanned_not_skipped(self):
        evidence = REPO / "docs" / "enterprise-platform" / "evidence"
        if not evidence.is_dir():
            self.fail("证据根目录不存在：扫不到不等于干净，本测试不允许跳过")
        report = gate.evaluate(evidence, gate.DEFAULT_CREDENTIALS, [])
        self.assertGreater(report["coverage"]["files_present"], 1000, report["coverage"])
        self.assertEqual(report["coverage"]["files_present"], report["coverage"]["files_scanned"],
                         "有证据文件没被扫到")
        self.assertEqual([], report["violations"], json.dumps(report["violations"], ensure_ascii=False)[:600])
        # 活口令层没料可用时必须**自曝**，不能悄悄算过：
        # 这台机器当前没有活沙箱，所以报告必须写 not-exercised 而不是留一个空字段。
        if report["coverage"]["live_credentials_loaded"] == 0:
            self.assertIn("not-exercised", report["coverage"]["live_layer"])
        else:
            self.assertEqual("exercised", report["coverage"]["live_layer"])
        self.assertTrue(report["coverage"]["credential_globs"], "凭据面落点清单为空 = 第四层什么都没探")

    def test_cli_json_is_pure_and_utf8(self):
        proc = subprocess.run([sys.executable, str(SCRIPTS / "check_evidence_credentials.py"), "--json"],
                              capture_output=True, cwd=str(REPO), timeout=600)
        self.assertEqual(0, proc.returncode, proc.stderr.decode("utf-8", "replace")[:300])
        raw = proc.stdout
        self.assertNotIn(b"\xd6\xa4", raw, "stdout 混进了 GBK 编码的人读摘要：--json 必须是纯 JSON")
        payload = json.loads(raw.decode("utf-8"))
        self.assertIn("coverage", payload)


if __name__ == "__main__":
    unittest.main()
