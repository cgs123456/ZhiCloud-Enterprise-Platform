#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
将业务代码（cn/zhicloud/** 包）中裸抛的
    throw new RuntimeException(...)
    throw new IllegalStateException(...)
统一改造为
    throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, ...)
复用全局错误码 500，保留原始 message 与 cause。

设计要点：
- 仅扫描 */src/main/java 下以 cn/zhicloud/ 开头的包；
  排除 org/springframework、org/flowable 等「重打包外部类」（避免改坏 Spring/Flowable 内部行为）。
- 跳过 check_bare_throws.py 认定的 @bare-throw-ignore 豁免点（throw 行本身或其上方 3 行内）：
  那些是 CLI 入口与刻意 fail-closed 的基础设施信号，改成 ServiceException 会破坏行为与测试。
- 注意：本脚本读写未做行尾保真，对 CRLF/混合行尾文件会整体改写；实跑前先用 --dry-run，
  并确认目标文件行尾与改后一致。
- 括号匹配与参数切分均「字符串字面量感知」，正确处理 "类型(%s)..." 这类含括号的消息。
- 单参为 Throwable 变量 -> (code, e.getMessage(), e)；单参为字符串 -> (code, msg)；
  双参 (msg, e) -> (code, msg, e)。多参(>2) 跳过并告警（需人工 review）。
- 自动补齐 import（ServiceException / GlobalErrorCodeConstants）。

用法：
    python3 scripts/convert_bare_throws.py --dry-run     # 仅预览
    python3 scripts/convert_bare_throws.py               # 执行改写
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPT_DIR not in sys.path:
    sys.path.insert(0, SCRIPT_DIR)

# 豁免判定与注释识别只保留一份实现：此前两个脚本各存一份 3 行回看规则，
# 已经发生漂移——check 认块注释、convert 只认行注释，于是 convert 会改写
# 被 /* */ 注释掉的裸抛。共享实现后，门禁怎么判、改写器就怎么跳过。
import check_bare_throws as gate  # noqa: E402

THROW_RE = gate.THROW_RE
PKG_PREFIX = gate.PKG_PREFIX
IGNORE_MARK = gate.IGNORE_MARK
SERVICE_EXC_IMPORT = 'import cn.zhicloud.framework.common.exception.ServiceException;'
GLOBAL_IMPORT = 'import cn.zhicloud.framework.common.exception.enums.GlobalErrorCodeConstants;'


def find_matching_paren(s, open_pos):
    """从 s[open_pos] 的 '(' 起，返回匹配的 ')' 下标（字符串/字符字面量感知）。"""
    depth = 0
    i = open_pos
    n = len(s)
    in_str = in_chr = in_lc = escape = False
    while i < n:
        c = s[i]
        if in_lc:
            if c == '\n':
                in_lc = False
            i += 1
            continue
        if in_str:
            if escape:
                escape = False
            elif c == '\\':
                escape = True
            elif c == '"':
                in_str = False
            i += 1
            continue
        if in_chr:
            if escape:
                escape = False
            elif c == '\\':
                escape = True
            elif c == "'":
                in_chr = False
            i += 1
            continue
        if c == '"':
            in_str = True
        elif c == "'":
            in_chr = True
        elif c == '/' and i + 1 < n and s[i + 1] == '/':
            in_lc = True
        elif c == '(':
            depth += 1
        elif c == ')':
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def split_args(s):
    parts = []
    depth = 0
    cur = []
    in_str = in_chr = escape = False
    for c in s:
        if in_str:
            cur.append(c)
            if escape:
                escape = False
            elif c == '\\':
                escape = True
            elif c == '"':
                in_str = False
            continue
        if in_chr:
            cur.append(c)
            if escape:
                escape = False
            elif c == '\\':
                escape = True
            elif c == "'":
                in_chr = False
            continue
        if c == '"':
            in_str = True
            cur.append(c)
            continue
        if c == "'":
            in_chr = True
            cur.append(c)
            continue
        if c in '([{':
            depth += 1
            cur.append(c)
            continue
        if c in ')]}':
            depth -= 1
            cur.append(c)
            continue
        if c == ',' and depth == 0:
            parts.append(''.join(cur).strip())
            cur = []
            continue
        cur.append(c)
    if cur:
        parts.append(''.join(cur).strip())
    return parts


def transform_inner(inner):
    args = split_args(inner)
    if len(args) == 1:
        a = args[0]
        if re.fullmatch(r'[A-Za-z_][\w.]*', a):
            return f"{a}.getMessage(), {a}"
        return a
    if len(args) == 2:
        return f"{args[0]}, {args[1]}"
    return None


def in_line_comment(text, pos):
    """粗略判断 pos 是否位于 // 行注释内。"""
    line_start = text.rfind('\n', 0, pos) + 1
    seg = text[line_start:pos]
    return '//' in seg


def add_imports(text):
    if SERVICE_EXC_IMPORT not in text:
        text = _insert_after_package(text, SERVICE_EXC_IMPORT)
    if GLOBAL_IMPORT not in text:
        text = _insert_after_package(text, GLOBAL_IMPORT)
    return text


def _insert_after_package(text, import_line):
    m = re.search(r'^\s*package\s+[\w.]+;\s*\n', text, re.MULTILINE)
    if m:
        idx = m.end()
        return text[:idx] + import_line + '\n' + text[idx:]
    # 无 package 行则插到开头
    return import_line + '\n' + text


def iter_main_java(root=None):
    scan_root = os.path.abspath(root or ROOT)
    for dirpath, _dirs, files in os.walk(scan_root):
        rel_dir = os.path.relpath(dirpath, scan_root).replace('\\', '/')
        if not rel_dir.endswith('src/main/java'):
            # 仅处理 .../src/main/java 目录树
            if '/src/main/java/' not in ('/' + rel_dir + '/'):
                # 继续深入，但快速跳过非 main 的
                pass
        for fn in files:
            if not fn.endswith('.java'):
                continue
            full = os.path.join(dirpath, fn)
            rel = os.path.relpath(full, scan_root).replace('\\', '/')
            # 必须是 src/main/java 下
            marker = '/src/main/java/'
            mi = rel.find(marker)
            if mi == -1:
                continue
            pkg_rel = rel[mi + len(marker):]
            if not pkg_rel.startswith(PKG_PREFIX):
                continue
            if fn in ('ServiceException.java', 'GlobalErrorCodeConstants.java'):
                continue
            yield full, rel


def is_exempted(lines, line_no):
    """直接复用门禁判定：标记必须在 throw 同行、必须在真注释内、必须带非空理由。"""
    return gate.is_ignored(lines, line_no)


def process_file(full, rel, dry_run, stats):
    with open(full, 'r', encoding='utf-8') as f:
        text = f.read()
    if 'throw new RuntimeException(' not in text and 'throw new IllegalStateException(' not in text:
        return False

    lines = text.splitlines()
    changes = []
    for m in THROW_RE.finditer(text):
        if gate.in_comment(text, m.start()):
            continue
        line_no = text.count('\n', 0, m.start()) + 1
        if is_exempted(lines, line_no):
            stats['exempted'].append((rel, line_no))
            continue
        open_pos = text.index('(', m.start())
        close = find_matching_paren(text, open_pos)
        if close == -1:
            stats['warn'].append((rel, 'unbalanced parens'))
            continue
        inner = text[open_pos + 1:close]
        newargs = transform_inner(inner)
        if newargs is None:
            stats['warn'].append((rel, f'multi-arg skip: ({inner[:80]})'))
            continue
        old_span = text[m.start():close + 1]
        new_span = f"throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, {newargs})"
        changes.append((m.start(), close + 1, old_span, new_span, line_no))

    if not changes:
        return False

    if dry_run:
        for _s, _e, old, new, ln in changes:
            stats['preview'].append((rel, ln, old, new))
        stats['files'] += 1
        return False

    # 逆序替换，避免下标漂移
    new_text = text
    for s, e, _old, new, _ln in sorted(changes, key=lambda x: x[0], reverse=True):
        new_text = new_text[:s] + new + new_text[e:]
    new_text = add_imports(new_text)
    with open(full, 'w', encoding='utf-8', newline='') as f:
        f.write(new_text)
    stats['converted'] += len(changes)
    stats['files'] += 1
    return True


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--dry-run', action='store_true', help='仅预览，不写文件')
    ap.add_argument('--root', default=ROOT)
    args = ap.parse_args()

    stats = {'files': 0, 'converted': 0, 'warn': [], 'preview': [], 'exempted': []}
    for full, rel in iter_main_java(args.root):
        process_file(full, rel, args.dry_run, stats)

    print(f'[INFO] {len(stats["exempted"])} 处 @bare-throw-ignore 豁免点已跳过（不改写）。')

    if args.dry_run:
        print(f'[DRY-RUN] 命中文件 {stats["files"]} 处潜在改写（未写入）：')
        for rel, ln, old, new in stats['preview']:
            print(f'  {rel}:{ln}')
            print(f'    - {old}')
            print(f'    + {new}')
        if stats['warn']:
            print('[WARN] 需人工 review：')
            for rel, msg in stats['warn']:
                print(f'  {rel}: {msg}')
        print(f'[DRY-RUN] 合计 {len(stats["preview"])} 处改写、{len(stats["warn"])} 处告警。')
        return 0

    print(f'[DONE] 改写文件 {stats["files"]} 个，转换裸抛 {stats["converted"]} 处。')
    if stats['warn']:
        print('[WARN] 以下跳过，需人工 review：')
        for rel, msg in stats['warn']:
            print(f'  {rel}: {msg}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
