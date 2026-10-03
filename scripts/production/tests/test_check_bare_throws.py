import io
import os
import re
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parents[2]
REPO_ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(SCRIPTS_DIR))

import check_bare_throws as cbt  # noqa: E402

BARE = 'throw new IllegalStateException("boom"); // @bare-throw-ignore CLI 入口，不在 Spring 容器内'
BARE_PLAIN = 'throw new IllegalStateException("boom");'
MARK = '@bare-throw-ignore'


class IgnitionRuleTests(unittest.TestCase):
    """豁免判定的四条边界，每条都对应一个真实被利用过的漏洞。"""

    def test_line_comment_marker_exempts(self):
        lines = ['    void m() {', '        ' + BARE, '    }']
        self.assertTrue(cbt.is_ignored(lines, 2))

    def test_marker_without_reason_does_not_exempt(self):
        lines = ['    void m() {', '        throw new IllegalStateException("x"); // ' + MARK, '    }']
        self.assertFalse(cbt.is_ignored(lines, 2))

    def test_previous_line_marker_does_not_exempt(self):
        """回归：回看窗允许"上方 3 行"时，60 个标记制造了 189 个自动豁免槽位。"""
        lines = ['        // ' + MARK + ' 理由', '        ' + BARE_PLAIN, '    }']
        self.assertFalse(cbt.is_ignored(lines, 2))

    def test_marker_inside_string_literal_does_not_exempt(self):
        """回归：标记只要出现在行内任意位置就被当成注释，字符串字面量可静默豁免。"""
        lines = ['    void m() {',
                 '        String s = "// ' + MARK + ' 这不是注释";',
                 '        ' + BARE_PLAIN, '    }']
        self.assertFalse(cbt.is_ignored(lines, 3))
        self.assertEqual(-1, cbt.line_comment_index(lines[1]))

    def test_comment_detector_skips_escaped_quote(self):
        line = '        String s = "he said \\" // not-a-comment"; // ' + MARK + ' 真注释'
        idx = cbt.line_comment_index(line)
        self.assertEqual(line.rindex('// ' + MARK), idx)
        self.assertTrue(cbt.is_ignored([line], 1))

    def test_comment_detector_returns_minus_one_without_comment(self):
        self.assertEqual(-1, cbt.line_comment_index('        ' + BARE_PLAIN))
        self.assertEqual(-1, cbt.line_comment_index('        String s = "// still a string";'))


class RealRepoGuardTests(unittest.TestCase):
    """守卫必须自证它真的扫到了东西——前缀写错会让它退化成永远 [OK] 的空转检查。"""

    def test_real_repo_scans_actual_packages(self):
        files = sorted(rel for _full, rel in cbt.iter_main_java(str(REPO_ROOT)))
        self.assertTrue(files, "真实仓库扫描结果为空，PKG_PREFIX 已失配")
        self.assertTrue(any(rel.endswith("server/DatabaseMigrationRunner.java") for rel in files))

    def test_prefix_matches_two_layer_package_roots(self):
        """前缀必须是包根 `cn/zhicloud/`。历史上写成 `cn/zhicloud/zhicloud/` 时门禁扫到 0 个文件
        却恒输出 OK，所以这里断言真实存在的一层目录结构，而不是把常量再抄一遍。"""
        self.assertEqual('cn/zhicloud/', cbt.PKG_PREFIX)
        rels = [rel.split('/src/main/java/')[1] for _f, rel in cbt.iter_main_java(str(REPO_ROOT))]
        self.assertTrue(rels)
        second_layer = {rel.split('/')[1] for rel in rels}
        self.assertEqual({'zhicloud'}, second_layer)
        third_layer = {rel.split('/')[2] for rel in rels}
        self.assertTrue({'module', 'framework', 'server'} & third_layer,
                        "包第三层不含已知的 module/framework/server，前缀可能又漂了")

    def test_real_repo_is_green_and_reports_scanned_count(self):
        out = io.StringIO()
        with redirect_stdout(out):
            code = cbt.main([])
        text = out.getvalue()
        self.assertEqual(0, code, text)
        self.assertIn("[INFO] 扫描", text)
        self.assertNotIn("空转", text)

    def test_every_exemption_sits_on_a_throw_line(self):
        """一个标记只豁免它所在那一行的裸抛：标记行若不与 throw 同行，就是死标记。"""
        marker_lines = 0
        for full, _rel in cbt.iter_main_java(str(REPO_ROOT)):
            with open(full, encoding='utf-8', errors='ignore') as f:
                for line in f:
                    if MARK in line:
                        marker_lines += 1
                        self.assertTrue(
                            re.search(r'throw\s+new\s+(RuntimeException|IllegalStateException)\s*\(', line),
                            "标记未落在 throw 行尾：%s" % line.strip()[:120])
        out = io.StringIO()
        with redirect_stdout(out):
            cbt.main([])
        exempted = int(re.search(r'\[INFO\] (\d+) 处已通过', out.getvalue()).group(1))
        self.assertEqual(marker_lines, exempted, "标记数与豁免数不一致，存在未生效或重复标记")


class EmptyScanGuardTests(unittest.TestCase):
    def test_scanning_nothing_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            out = io.StringIO()
            with redirect_stdout(out):
                code = cbt.main(["--root", tmp])
            self.assertEqual(1, code)
            self.assertIn("空转", out.getvalue())

    def test_exemption_ratchet_blocks_unreviewed_growth(self):
        """豁免总数超过已审校上限必须 FAIL——否则 docstring 承诺的"不会悄悄累积"是空话。"""
        original = cbt.MAX_EXEMPTIONS
        cbt.MAX_EXEMPTIONS = 1
        try:
            out = io.StringIO()
            with redirect_stdout(out):
                code = cbt.main([])
            self.assertEqual(1, code)
            self.assertIn("超过已审校上限", out.getvalue())
        finally:
            cbt.MAX_EXEMPTIONS = original


class TempTreeBehaviourTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def write(self, body, pkg="cn.zhicloud.module.x"):
        path = self.repo / "zhicloud-module-x" / "src" / "main" / "java"
        for part in pkg.split("."):
            path = path / part
        path.mkdir(parents=True, exist_ok=True)
        target = path / "Holder.java"
        text = "package %s;\n\npublic class Holder {\n    void m() {\n%s\n    }\n}\n" % (pkg, body)
        target.write_text(text, encoding="utf-8", newline="\n")
        return target

    def run_gate(self):
        out = io.StringIO()
        with redirect_stdout(out):
            code = cbt.main(["--root", str(self.repo)])
        return code, out.getvalue()

    def test_bare_throw_is_reported(self):
        self.write("        " + BARE_PLAIN)
        code, text = self.run_gate()
        self.assertEqual(1, code)
        self.assertIn("Holder.java:5", text)

    def test_line_end_marker_exempts_and_is_listed(self):
        self.write("        " + BARE)
        code, text = self.run_gate()
        self.assertEqual(0, code, text)
        self.assertIn("1 处已通过", text)

    def test_marker_must_not_cover_a_second_throw(self):
        """紧接在已豁免行之后的裸抛必须被报出。"""
        self.write("        %s\n        %s" % (BARE, BARE_PLAIN))
        code, text = self.run_gate()
        self.assertEqual(1, code)
        self.assertIn("Holder.java:6", text)

    def test_marker_without_reason_is_still_a_finding(self):
        self.write('        throw new IllegalStateException("x"); // ' + MARK)
        code, _text = self.run_gate()
        self.assertEqual(1, code)

    def test_throw_inside_comment_is_not_counted(self):
        self.write("        // " + BARE_PLAIN)
        code, text = self.run_gate()
        self.assertEqual(0, code, text)
        self.assertIn("扫描 1 个", text)
        self.assertNotIn("Holder.java:5", text)

    def test_outside_prefix_is_skipped_while_inside_is_reported(self):
        """同树同时放两个文件：prefix 外的裸抛被忽略，prefix 内的必须被报出。"""
        self.write(BARE_PLAIN, pkg="org.springframework.bridge")
        self.write(BARE_PLAIN, pkg="cn.zhicloud.module.y")
        code, text = self.run_gate()
        self.assertEqual(1, code)
        self.assertIn("Holder.java:5", text)
        self.assertNotIn("springframework", text)
        self.assertIn("扫描 1 个", text)


class CommandLineTests(unittest.TestCase):
    def test_root_option_is_honoured_and_paths_are_relative_to_it(self):
        with tempfile.TemporaryDirectory() as tmp:
            target = Path(tmp) / "mod" / "src" / "main" / "java" / "cn" / "zhicloud" / "x"
            target.mkdir(parents=True)
            (target / "Holder.java").write_text(
                "package cn.zhicloud.x;\npublic class Holder {\n    void m() {\n        %s\n    }\n}\n"
                % BARE_PLAIN, encoding="utf-8", newline="\n")
            out = io.StringIO()
            with redirect_stdout(out):
                code = cbt.main(["--root", tmp])
            text = out.getvalue()
            self.assertEqual(1, code)
            self.assertNotIn("..", text, "--root 下仍输出穿越绝对路径，报告不可用")
            self.assertIn("Holder.java", text)


class CommentDetectionTests(unittest.TestCase):
    """`in_comment()` 决定一条 throw 算不算"真代码"，它自己不能是绕过路径。

    旧实现用 rfind('/*') 和"同行之前有没有 //"判断，两者都不看字符串字面量：
    `String s = "//"; throw new IllegalStateException(...);` 会因为字符串里的 //
    被整行当成注释而静默放行——实测这是第 6 条绕过路径。
    """

    def _pos(self, text, needle):
        at = text.index(needle)
        return at

    def test_marker_like_slashes_inside_string_do_not_hide_the_throw(self):
        for literal in ('"//"', '"/*"', '"a\\"//"', "'/'"):
            text = 'class T { void a() { String s = %s; throw new IllegalStateException("x"); } }' % literal
            with self.subTest(literal=literal):
                self.assertFalse(cbt.in_comment(text, self._pos(text, 'throw new')),
                                 '字面量里的 // 与 /* 不得让后面的真裸抛消失')

    def test_genuine_block_comment_throw_is_not_a_hit(self):
        text = 'class T { void a() {\n /* throw new RuntimeException("x"); */\n int y = 1;\n} }'
        self.assertTrue(cbt.in_comment(text, self._pos(text, 'throw new RuntimeException')),
                        '块注释里的 throw 不是代码，误报会让门禁因文档注释而红')

    def test_genuine_line_comment_throw_is_not_a_hit(self):
        text = 'class T { void a() {\n // throw new RuntimeException("x");\n int y = 1;\n} }'
        self.assertTrue(cbt.in_comment(text, self._pos(text, 'throw new RuntimeException')))

    def test_code_after_a_closed_comment_is_visible_again(self):
        text = 'class T { /* c */ void a() { throw new IllegalStateException("x"); } }'
        self.assertFalse(cbt.in_comment(text, self._pos(text, 'throw new IllegalStateException')))

    def test_string_containing_a_throw_is_not_a_hit(self):
        text = 'class T { void a() { String s = "throw new RuntimeException(x)"; int y = 1; } }'
        self.assertTrue(cbt.in_comment(text, self._pos(text, 'throw new RuntimeException')),
                        '字符串里的伪 throw 不该被当成裸抛')

    def test_scan_state_survives_an_unterminated_construct(self):
        text = 'class T { void a() { String s = "未闭合 ; throw new IllegalStateException("x"); } }'
        # 未闭合字面量时保守地跳过（宁可不报，也不把语法错误的文件当成一堆裸抛）
        self.assertTrue(cbt.in_comment(text, self._pos(text, 'throw new IllegalStateException')))


if __name__ == "__main__":
    unittest.main()
