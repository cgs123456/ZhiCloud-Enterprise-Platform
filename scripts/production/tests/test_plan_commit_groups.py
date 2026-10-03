# -*- coding: utf-8 -*-
"""分组提交门禁（plan_commit_groups.py）的守卫测试。

判定点都写成"喂一份合成工作树 → 必须报出对应违规"，因为真工作树恰好是绿的
并不能证明判定会响。真树只用来证明两件事：枚举不是空的、当前顺序自洽。
"""
from __future__ import annotations

import json
import subprocess
import sys
import unittest
from pathlib import Path

SCRIPTS = Path(__file__).resolve().parents[1]
REPO = SCRIPTS.parent.parent
sys.path.insert(0, str(SCRIPTS))

import plan_commit_groups as gate  # noqa: E402

EV_FILE = "docs/enterprise-platform/evidence/production/x.json"
CONTRACT = "scripts/production/migration-contract.json"
JAVA_MAIN = ("zhicloud-framework/zhicloud-spring-boot-starter-biz-integration/src/main/java/"
             "cn/zhicloud/framework/integration/dal/dataobject/EventInboxDO.java")
SQL_V119 = "zhicloud-server/src/main/resources/db/migration/V119__x.sql"
PS1 = "scripts/production/mysql-sandbox.ps1"
TEST_PY = "scripts/production/tests/test_native_mysqld_sandbox.py"
GATE_PY = "scripts/production/check_migration_contract.py"


def plan(items, texts):
    return gate.analyze(items, set(items), lambda p: texts.get(p, ""), {"fixture": True})


class ViolationTests(unittest.TestCase):
    def test_forward_reference_is_reported(self):
        # 组3（migration-contract）引用了组4（business-code）里的未跟踪文件：
        # 单独 checkout 组3 时那笔提交必然缺工件 —— 这正是 T2 修过的形状，必须判红。
        items = [CONTRACT, JAVA_MAIN]
        texts = {CONTRACT: json.dumps({"files": [{"path": JAVA_MAIN}]})}
        out = plan(items, texts)
        kinds = [v["rule"] for v in out["violations"]]
        self.assertIn("reference-order", kinds, out["violations"])
        hit = [v for v in out["violations"] if v["rule"] == "reference-order"][0]
        self.assertEqual("migration-contract", hit["group"])
        self.assertEqual("business-code", hit["referenced_group"])

    def test_mutual_reference_becomes_must_merge_not_two_order_errors(self):
        items = [CONTRACT, JAVA_MAIN]
        texts = {CONTRACT: "EventInboxDO.java", JAVA_MAIN: "migration-contract.json"}
        out = plan(items, texts)
        by = {}
        for v in out["violations"]:
            by.setdefault(v["rule"], []).append(v)
        self.assertIn("groups-must-merge", by, out["violations"])
        self.assertEqual({"migration-contract", "business-code"}, set(by["groups-must-merge"][0]["groups"]))
        # 环已经用"并成一次提交"表达，就不该再各报一条先后顺序（同一条缺陷报两遍会淹没真信号）
        self.assertNotIn("reference-order", by, json.dumps(by, ensure_ascii=False)[:400])

    def test_evidence_leaks_only_when_a_rule_can_swallow_it(self):
        # classify() 会跳过证据路径，所以"分组里没有证据"是构造出来的真，不构成判定。
        # 真正的判定是：**没有任何一条规则能捞到证据路径**。这里把组2 的规则放宽到 docs/ 再判。
        items = [EV_FILE, CONTRACT, JAVA_MAIN]
        original = list(gate.GROUP_RULES)
        widened = [(no, name, (lambda p: p.startswith("docs/")) if name == "docs-platform" else rule, why)
                   for no, name, rule, why in original]
        gate.GROUP_RULES[:] = widened
        try:
            out = plan(items, {})
            self.assertTrue(any(v["rule"] == "evidence-must-not-be-committed" for v in out["violations"]),
                            "规则能捞到证据文件却没判红：这条判定是空转的")
        finally:
            gate.GROUP_RULES[:] = original
        clean = plan(items, {})
        self.assertFalse(any(v["rule"] == "evidence-must-not-be-committed" for v in clean["violations"]),
                         "还原后仍报证据泄漏：说明判定写死在夹具上")

    def test_unclassified_item_is_reported(self):
        out = plan([EV_FILE, "tools/odd.txt"], {})
        orphans = [v for v in out["violations"] if v["rule"] == "exactly-one-group"]
        self.assertTrue(orphans, out["violations"])
        self.assertIn("tools/odd.txt", orphans[0]["paths"])

    def test_dead_group_rule_is_reported(self):
        # 一条规则命中 0 个文件 = 分组表已经失真（工件删了、规则留着），必须响。
        out = plan([CONTRACT], {})
        self.assertTrue(any(v["rule"] == "dead-group-rule" for v in out["violations"]), out["violations"])

    def test_version_only_migration_mention_resolves_to_the_sql_file(self):
        # 代码里常只写 `V119`，不写完整文件名；引用面必须能把它落到具体 SQL 上，
        # 否则"契约与迁移同组"这条靠人自觉。
        items = [JAVA_MAIN, SQL_V119, CONTRACT]
        texts = {JAVA_MAIN: "-- schema as of V119__x.sql applied by Flyway"}
        out = plan(items, texts)
        edges = {(e["from"], e["to"]) for e in out["group_edges"]}
        self.assertIn(("business-code", "migration-contract"), edges, out["group_edges"])


class RealTreeTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.plan = gate.build_plan()

    def test_current_plan_is_self_consistent_and_not_empty(self):
        self.assertEqual([], self.plan["violations"],
                         json.dumps(self.plan["violations"], ensure_ascii=False)[:800])
        w = self.plan["worktree"]
        # 枚举为空就等于一切"通过"：必须证明它真的量到了工作树。
        # 注意这里钉的是"非空"而不是某批的绝对数量：上一批 242 个路径提交完之后，
        # 可提交面本来就该变小，把 >100 当常量会让工具在每次成功提交后自红。
        self.assertGreater(w["items_seen"], 0, f"枚举为空，门禁在空转：{w}")
        self.assertGreater(w["evidence_untracked"], 0,
                           f"证据面异常小（本仓库按约定始终有未入库证据）：{w}")
        pending_non_evidence = (w["untracked_total"] - w["evidence_untracked"]) + w["modified_tracked"]
        if pending_non_evidence:
            # 有真实待提交项却一个都没分类出去 = 规则集体失灵；
            # 这里刻意不钉">100"这种某批次的绝对数量：上一批 242 个路径提交完之后
            # 可提交面本来就该只剩几条，钉绝对值会让工具在每次成功提交后自红。
            self.assertGreaterEqual(w["committable_items"], 1,
                                    f"待提交 {pending_non_evidence} 项却分类为空：{w}")
        self.assertEqual(w["committable_items"], sum(g["count"] for g in self.plan["groups"]))
        self.assertEqual(w["git_status_uall_lines"],
                         w["untracked_total"] + w["modified_tracked"])

    def test_scripts_and_tests_are_one_atomic_group(self):
        """本轮实测出的结论：.ps1、门禁 .py、守卫测试 .py 互相引用，分不开。

        钉在合成输入上而不是真实工作树：那三个文件一旦提交，真树上就没有可判对象了，
        而这条结论本身与"今天是否脏"无关（钉在真树上会在每次成功提交后自红）。
        """
        items = [PS1, TEST_PY, GATE_PY]
        out = plan(items, {})
        groups = {g["name"]: set(g["paths"]) for g in out["groups"]}
        tooling = groups["tooling"]
        self.assertEqual(set(items), tooling, out["groups"])
        for path in items:
            in_others = [n for n, s in groups.items() if n != "tooling" and path in s]
            self.assertEqual([], in_others, f"{path} 被两个组同时认领")
        # 分组的"为什么"必须是实测出来的环（互相引用），而不是我的偏好
        self.assertIn("强连通", next(g["why"] for g in out["groups"] if g["name"] == "tooling"))

    def test_cli_json_is_strict_utf8_and_machine_readable(self):
        # 上一轮真踩：脚本用控制台编码(cp936)打印中文说明，我按 UTF-8 读回得到 U+FFFD，
        # JSON 解析直接崩。一份"给人看还行、给机器读会坏"的证据面不算证据。
        proc = subprocess.run([sys.executable, str(SCRIPTS / "plan_commit_groups.py"), "--json"],
                              capture_output=True, cwd=str(REPO), timeout=300)
        raw = proc.stdout
        self.assertEqual(0, proc.returncode, proc.stderr.decode("utf-8", "replace")[:400])
        payload = json.loads(raw.decode("utf-8"))  # strict：有非 UTF-8 字节或多余输出都会在这里崩
        self.assertIn("groups", payload)
        self.assertNotIn("\ufffd", raw.decode("utf-8"))


if __name__ == "__main__":
    unittest.main()
