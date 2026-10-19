"""schema_snapshot.py 重放语义守卫（合成夹具，不连数据库）。

背景（§12.280 对齐项）：解析器已重构为「语句级顺序回放 + DELIMITER 过程捕获 +
CALL 过程体符号执行」；曾发生四类静默缺陷，本文件各以合成夹具钉死，防止回潮：

  1. 过程参数头截取（decl-slice）——BEGIN 首字符曾被拼进参数头导致 `\)\s*$` 锚定失败，
     CONCAT 实参绑不上 → 参数化 ADD COLUMN 全部静默丢失；
  2. CONCAT 求值内层 strip()——字面量首尾（`` '` `'``、`` ' COMMENT '`）空格被剥，
     合成语句形如 ``tADD COLUMN``、``INT COMMENT`` → 解析错乱；
  3. 文件首 BOM——utf-8-sig 未剥离时 `\ufeffSET` 使 SET-派发门正则失配，首语句被静默吞掉；
  4. SET 内嵌字面量的 `''` 成对转义——V44/V205 风格 `'ALTER ... COMMENT ''...'''` 曾被
     在 COMMENT 后截断，同语句后续 ALTER 片段丢失。

另含回放契约守卫：DROP 后防复活合并、裸 DROP 缺表告警、过程名字族派发、
融合 IF 体 carve。全部只跑 DDL 文本，无 DB 依赖。
"""

import os
import shutil
import sys
import tempfile
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
REPO_ROOT = TESTS_DIR.parents[2]
GENERATORS_DIR = REPO_ROOT / "demo-data" / "generators"
sys.path.insert(0, str(GENERATORS_DIR))
# 先红后绿验证通道：ZC_SNAP_GEN_DIR 指向打了已知缺陷的临时副本目录，必须后插保证压过正式目录
_GEN_DIR_OVERRIDE = os.environ.get("ZC_SNAP_GEN_DIR")
if _GEN_DIR_OVERRIDE:
    sys.path.insert(0, _GEN_DIR_OVERRIDE)

import schema_snapshot as g  # noqa: E402


class TempDdlCase(unittest.TestCase):
    """在临时目录里生成合成迁移链，直接驱动 build_snapshot。"""

    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp(prefix="zc_snap_guard_")).resolve()
        chain = self.tmp / "chain"
        chain.mkdir()
        self.extra = self.tmp / "extra"
        self.extra.mkdir()
        self._dirs = [chain, self.extra]
        self._chain = chain
        self.snap_target = {}
        self.addCleanup(shutil.rmtree, self.tmp, ignore_errors=True)

    def write(self, name: str, text: str, bom: bool = False, extra: bool = False):
        target = self.extra if extra else self._chain
        p = target / name
        raw = ("﻿" if bom else "") + text
        p.write_bytes(raw.encode("utf-8"))
        return p

    def build(self):
        # build_snapshot 以模块级 REPO 计算 rel 溯源；夹具目录在仓库外，临时指向夹具根
        old_repo = getattr(g, "REPO", None)
        g.REPO = self.tmp.resolve()
        try:
            snap = g.build_snapshot(self._dirs)
        finally:
            if old_repo is not None:
                g.REPO = old_repo
        return snap, snap["tables"], snap["warnings"]


class ProcParamDeclTest(TempDdlCase):
    """守卫 1/2：CALL 过程体符号执行——参数绑定 + CONCAT 字面量空格保真。"""

    PROC = """\
-- 守卫夹具：参数化 ADD COLUMN 复合过程
DELIMITER $$
CREATE PROCEDURE p_guard_add_col(IN tableName VARCHAR(128), IN columnName VARCHAR(64),
                                 IN columnDef TEXT, IN columnComment VARCHAR(255))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = tableName)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = tableName
                         AND column_name = columnName) THEN
        SET @sql = CONCAT('ALTER TABLE `', tableName, '` ADD COLUMN `', columnName, '` ', columnDef,
                         ' COMMENT ''', columnComment, '''');
        PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;
"""

    def test_parameterized_add_column_lands_with_comment_and_spaces(self):
        self.write("V1__base.sql", "CREATE TABLE `t_guard` (`id` BIGINT NOT NULL AUTO_INCREMENT, PRIMARY KEY (`id`));")
        # 过程定义与 CALL 同文件：procs 按文件重置（真实迁移链 V22/V98 正是同文件形态）
        self.write(
            "V2__proc_and_call.sql",
            self.PROC + "\n"
            "CALL p_guard_add_col('t_guard', 'amount', 'DECIMAL(20,4) NULL DEFAULT 0', '金额（演示含空格字面量）');")
        _, tables, warnings = self.build()
        cols = tables["t_guard"]["columns"]
        names = [c["name"] for c in cols]
        self.assertIn("amount", names)
        amount = next(c for c in cols if c["name"] == "amount")
        self.assertIn("DECIMAL", (amount.get("type") or "").upper())
        self.assertTrue(amount.get("nullable", True))
        # 夹具故意让任一环节丢空格都会丢列/丢注释——以列存在为红绿判据
        self.assertEqual([w for w in warnings if "t_guard" in w], [])


class BomEmbeddedDdlTest(TempDdlCase):
    """守卫 3：BOM 头文件的首条 SET 内嵌 DDL 不被吞。"""

    def test_bom_head_set_embedded_alter_replays(self):
        stmt = (
            "SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS\n"
            "                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_bom' AND COLUMN_NAME = 'kind'),\n"
            "                  'DO 0',\n"
            "                  'ALTER TABLE `t_bom` ADD COLUMN `kind` TINYINT DEFAULT 2 COMMENT ''类型说明'' AFTER status');\n"
            "PREPARE zc_stmt FROM @zc_sql;\n"
            "EXECUTE zc_stmt;\n"
            "DEALLOCATE PREPARE zc_stmt;\n"
        )
        self.write("V1__base.sql", "CREATE TABLE `t_bom` (`status` INT NULL);")
        self.write("V2__ext.sql", stmt, bom=True)
        _, tables, warnings = self.build()
        names = [c["name"] for c in tables["t_bom"]["columns"]]
        self.assertIn("kind", names)
        self.assertEqual([w for w in warnings if "t_bom" in w], [])


class DoubledQuoteEmbeddedDdlTest(TempDdlCase):
    """守卫 4：SET 内嵌 ALTER 带 '' 成对转义注释时，同语句的第二个 ADD 片段不丢。"""

    def test_doubled_quote_comment_keeps_second_fragment(self):
        # v205/v44 真实形态：单字面量含 '' 转义；另一处相邻字面量拼接（仅空白相连）
        # 由 _embedded_ddl 的相邻拼接语义并入同一语句，两个 ADD 片段都落地
        stmt = (
            "SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.COLUMNS\n"
            "                  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_dq' AND COLUMN_NAME = 'a1'),\n"
            "                  'DO 0',\n"
            "                  'ALTER TABLE `t_dq` ADD COLUMN `a1` TINYINT DEFAULT 2 COMMENT ''类型（1 暗 / 2 明）'' AFTER status, '\n"
            "                  'ADD COLUMN `a2` INT NULL');\n"
            "PREPARE zc_stmt FROM @zc_sql;\n"
            "EXECUTE zc_stmt;\n"
            "DEALLOCATE PREPARE zc_stmt;\n"
        )
        self.write("V1__base.sql", "CREATE TABLE `t_dq` (`status` INT NULL);")
        self.write("V2__ext.sql", stmt)
        _, tables, warnings = self.build()
        names = [c["name"] for c in tables["t_dq"]["columns"]]
        self.assertIn("a1", names)
        self.assertIn("a2", names)
        # doubling 语义直证：注释里的 '' 必须解回单引号并保留为带引号原文
        # （丢失 doubling 时 raw 呈 COMMENT 类型（…） 无引号形态）
        a1 = next(c for c in tables["t_dq"]["columns"] if c["name"] == "a1")
        self.assertIn("COMMENT '类型（1 暗 / 2 明）'", a1["raw"], a1["raw"])
        self.assertEqual([w for w in warnings if "t_dq" in w], [])


class DropReplayMergeTest(TempDdlCase):
    """回放契约：DROP 感知合并——链条 DROP 后安装侧同名建表不得复活。"""

    def test_dropped_table_is_not_resurrected_by_install_dir(self):
        self.write("V1__make.sql",
                   "CREATE TABLE `t_drop` (`id` BIGINT, `legacy` INT NULL);")
        self.write("V2__retire.sql", "DROP TABLE IF EXISTS `t_drop`;")
        self.write("zz_install.sql", "CREATE TABLE `t_drop` (`id` BIGINT, `fresh1` INT NULL, `fresh2` INT NULL);",
                   extra=True)
        # main() 的真实用法：两个目录分建成 primary/secondary 再 merge，复活阻断由 merge 承担
        old_repo = getattr(g, "REPO", None)
        g.REPO = self.tmp
        try:
            primary = g.build_snapshot([self._chain])
            secondary = g.build_snapshot([self.extra])
        finally:
            if old_repo is not None:
                g.REPO = old_repo
        snap = g.merge_snapshots(primary, secondary)
        tables, warnings = snap["tables"], snap["warnings"]
        self.assertNotIn("t_drop", tables)
        # 复活阻断必须留痕（merged-skip 族），有利于排查任何“诈尸”
        self.assertTrue(any("t_drop" in w for w in warnings))

    def test_bare_drop_of_unknown_table_warns_fail_closed(self):
        self.write("V1__drop_unknown.sql", "DROP TABLE `t_ghost`;")
        _, _tables, warnings = self.build()
        self.assertTrue(any("t_ghost" in w for w in warnings))


class CallFamilyDispatchTest(TempDdlCase):
    """守卫 5：p_xxx_add_column 名字族派发按 CALL 实参落地；缺失过程定义时告警。"""

    PROC = """\
DELIMITER $$
CREATE PROCEDURE p_g2_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(500))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column) THEN
        SET @g2_sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE g2_stmt FROM @g2_sql;
        EXECUTE g2_stmt;
        DEALLOCATE PREPARE g2_stmt;
    END IF;
END$$
DELIMITER ;
"""

    def test_call_family_adds_column(self):
        self.write("V1__base.sql", "CREATE TABLE `t_fam` (`id` BIGINT NOT NULL, PRIMARY KEY (`id`));")
        self.write("V2__proc.sql", self.PROC)
        self.write("V3__call.sql", "CALL p_g2_add_column('t_fam', 'flag', 'TINYINT NOT NULL DEFAULT 1');")
        _, tables, warnings = self.build()
        names = [c["name"] for c in tables["t_fam"]["columns"]]
        self.assertIn("flag", names)
        self.assertEqual([w for w in warnings if "t_fam" in w], [])

    def test_call_to_unknown_ddl_suffixed_proc_warns_and_data_helper_is_silent(self):
        self.write("V1__base.sql", "CREATE TABLE `t_fam2` (`id` BIGINT NOT NULL, PRIMARY KEY (`id`));")
        # 契约：未定义且名字疑似列动作的 CALL 必须告警；数据型 helper（无 DDL 后缀）静默
        # （add_column/make_nullable/widen_column 有完整 args 时免定义直接生效；缺参才走告警）
        self.write("V2__calls.sql",
                   "CALL p_ghost_make_nullable('t_fam2');\n"
                   "CALL p_mystery_data_helper('t_fam2');")
        _, _tables, warnings = self.build()
        self.assertTrue(any("p_ghost_make_nullable" in w for w in warnings), list(warnings))
        self.assertFalse(any("p_mystery" in w for w in warnings), list(warnings))


class FusedIfCarveTest(TempDdlCase):
    """守卫 6：融合 IF 体里顶层 DDL 子句经 carve 抽取（V116 形态）。"""

    def test_fused_if_body_alter_is_extracted(self):
        stmt = (
            "IF EXISTS (SELECT 1 FROM information_schema.tables\n"
            "           WHERE table_schema = DATABASE() AND table_name = 't_if') THEN\n"
            "    ALTER TABLE `t_if` ADD COLUMN `note_tag` VARCHAR(32)  NOT NULL DEFAULT 'DEMO' COMMENT '标签';\n"
            "END IF;\n"
        )
        self.write("V1__base.sql", "CREATE TABLE `t_if` (`id` BIGINT NOT NULL, PRIMARY KEY (`id`));")
        # 同文件：过程捕获与 CALL 在一个文件，procs 按文件重置的契约短路跨文件体
        self.write("V2__proc_and_call.sql",
                   "DELIMITER $$\nCREATE PROCEDURE p_g3_body()\nBEGIN\n" + stmt + "END$$\nDELIMITER ;\n"
                   "CALL p_g3_body();")
        _, tables, warnings = self.build()
        names = [c["name"] for c in tables["t_if"]["columns"]]
        self.assertIn("note_tag", names)
        self.assertEqual([w for w in warnings if "t_if" in w], [])


class ImmutableSnapshotMetaTest(unittest.TestCase):
    """repo 快照元数据守卫：死 HR 表不得回归；payload/file 摘要自洽。"""

    def test_repo_snapshot_matches_retirement_and_digest(self):
        import hashlib
        import json
        snap_path = REPO_ROOT / "demo-data" / "schema" / "schema-snapshot.json"
        if not snap_path.is_file():
            self.skipTest("schema 快照不存在")
        raw = snap_path.read_bytes()
        snap = json.loads(raw.decode("utf-8"))
        tables = snap["tables"]
        for dead in ("hr_attendance", "hr_leave_balance", "hr_salary", "hr_performance", "hr_contract"):
            self.assertNotIn(dead, tables, "V208 已清退 %s，快照不得复活" % dead)
        # DB 真值锚（列级全量 diff 清零时同步核过）：防解析回潮
        self.assertEqual(48, len(tables["hr_employee"]["columns"]))
        self.assertEqual(29, len(tables["erp_sale_order"]["columns"]))
        meta = dict(snap)
        meta.pop("payload_sha256", None)
        payload_json = json.dumps(meta, ensure_ascii=False, indent=2, sort_keys=False)
        self.assertEqual(hashlib.sha256(payload_json.encode("utf-8")).hexdigest(),
                         snap["payload_sha256"],
                         "快照 payload_sha256 与内容不自洽（快照被手改或重生成漏跑）")


if __name__ == "__main__":
    unittest.main()
