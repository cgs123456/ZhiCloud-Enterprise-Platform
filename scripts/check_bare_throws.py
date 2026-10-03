#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
裸抛异常回归门禁：扫描 src/main 下 cn/zhicloud/** 包内是否残留
    throw new RuntimeException(...)
    throw new IllegalStateException(...)
若存在则失败（退出码 1），输出位置清单，防止绕过 ErrorCode 体系。

排除：org/springframework、org/flowable 等「重打包外部类」（与 convert_bare_throws.py 一致）。

豁免机制（@bare-throw-ignore）：
    只有两类代码可以豁免，且都必须写明理由：
      1) 运行在 Spring 容器之外的入口（如 Flyway 迁移 CLI 的 main 方法）——
         ServiceException 必须携带注册在全局错误码表里的 ErrorCode，而该错误永远不会
         返回给任何 API 调用方，硬套只会污染业务错误码空间；
      2) 刻意 fail-closed 的基础设施信号，其异常类型已被现有测试断言，改造类型会
         同时改变对外语义与测试结果——这类是否应统一，属人工裁决，不由本门禁代替决定。
    写法固定为 throw 语句所在行的行尾注释：
        throw new IllegalStateException("..."); // @bare-throw-ignore <理由>
    标记只在同一行生效、不回看上几行：一个标记只豁免一个裸抛，插不进第二个。
    标记后为空（只有空白）不算豁免；豁免总数超过 MAX_EXEMPTIONS 直接 FAIL，
    避免它们悄悄累积成新的技术债——新增豁免必须是一次有意识、可追溯的修改。

用法：
    python3 scripts/check_bare_throws.py
    python3 scripts/check_bare_throws.py --root /path/to/repo
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PKG_PREFIX = 'cn/zhicloud/'
THROW_RE = re.compile(r'throw\s+new\s+(RuntimeException|IllegalStateException)\s*\(', re.DOTALL)
IGNORE_MARK = '@bare-throw-ignore'
# 豁免标记只能写在 throw 语句所在行的行尾注释里。回看窗曾经允许"上方 3 行"，
# 那会让每个标记额外覆盖其下方 3 行：实测 60 个标记凭空制造出 189 个自动豁免槽位，
# 后来者把新裸抛插在标记附近就被静默放行。收紧为同行后，一个标记只可能豁免一个 throw。
IGNORE_LOOKBACK = 0
# 当前已审校的豁免总数上限。新增豁免必须是一次有意识的修改：先改这个数字并说明理由，
# 否则本门禁 FAIL。降低它代表真的还清了债。
MAX_EXEMPTIONS = 63


def in_comment(text, pos):
    """pos 是否落在真正的注释里。

    旧实现用 `text.rfind('/*')` 与"同一行之前有没有 //"判断，两者都不看字符串字面量：
    只要同一行更靠前的位置有个写在字符串里的 `//`（例如 `String s = "//";`），
    该行后面真正的裸抛就会被当成注释整体吞掉——实测这就是第 6 条绕过路径。
    现在改为从文件开头做一次带状态的扫描：字符串/字符字面量、转义、块注释、行注释分别建模。
    只对正则命中的位置调用，代价可接受。
    """
    i = 0
    n = min(pos, len(text))
    in_block = False
    quote = None
    escaped = False
    while i < n:
        ch = text[i]
        if in_block:
            if ch == '*' and i + 1 < len(text) and text[i + 1] == '/':
                in_block = False
                i += 2
                continue
            i += 1
            continue
        if quote:
            if escaped:
                escaped = False
            elif ch == '\\':
                escaped = True
            elif ch == quote:
                quote = None
            i += 1
            continue
        if ch in ('"', "'"):
            quote = ch
            i += 1
            continue
        if ch == '/' and i + 1 < len(text):
            nxt = text[i + 1]
            if nxt == '/':
                nl = text.find('\n', i)
                return pos < (len(text) if nl == -1 else nl)
            if nxt == '*':
                in_block = True
                i += 2
                continue
        i += 1
    # 扫到 pos 才结束：此刻仍在块注释或字面量里，说明这个位置不是可执行代码。
    # 早退版只 return False，会把 `/* throw new RuntimeException(...) */` 误报成裸抛。
    return in_block or quote is not None


def line_comment_index(line):
    """返回该行行注释 "//" 的起始下标，不在注释内则返回 -1。

    必须跳过字符串/字符字面量：`String s = "// x";` 里的 // 不是注释起点，
    否则任何含 marker 的字面量都会被当成豁免注释。
    """
    in_str = None
    escaped = False
    i = 0
    while i < len(line):
        ch = line[i]
        if in_str:
            if escaped:
                escaped = False
            elif ch == '\\':
                escaped = True
            elif ch == in_str:
                in_str = None
        else:
            if ch in ('"', "'"):
                in_str = ch
            elif ch == '/' and i + 1 < len(line) and line[i + 1] == '/':
                return i
        i += 1
    return -1


def is_ignored(lines, line_no):
    """判断第 line_no 行(1-based)的裸抛是否被显式豁免。

    三个条件缺一不可：标记必须位于该行（不回看上几行）、必须落在真正的行注释内
    （不在字符串字面量里）、标记后必须带非空理由（只写空标记等于把裸抛藏起来）。
    """
    idx = line_no - 1
    if idx < 0 or idx >= len(lines):
        return False
    ln = lines[idx]
    cidx = line_comment_index(ln)
    if cidx == -1:
        return False
    at = ln.find(IGNORE_MARK, cidx)
    if at == -1:
        return False
    return bool(ln[at + len(IGNORE_MARK):].strip())


def iter_main_java(root=None):
    scan_root = os.path.abspath(root or ROOT)
    for dirpath, _dirs, files in os.walk(scan_root):
        for fn in files:
            if not fn.endswith('.java'):
                continue
            full = os.path.join(dirpath, fn)
            # rel 必须相对实际扫描根：否则 --root 指向别处时输出会变成
            # ../../../.../Holder.java:5 这种没法定位的路径，报告不可用。
            rel = os.path.relpath(full, scan_root).replace('\\', '/')
            mi = rel.find('/src/main/java/')
            if mi == -1:
                continue
            pkg_rel = rel[mi + len('/src/main/java/'):]
            if not pkg_rel.startswith(PKG_PREFIX):
                continue
            yield full, rel


def main(argv=None):
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', default=ROOT)
    args = ap.parse_args(argv)

    hits = []
    ignored = []
    scanned = 0
    for full, rel in iter_main_java(args.root):
        scanned += 1
        with open(full, 'r', encoding='utf-8', errors='ignore') as f:
            text = f.read()
        lines = text.splitlines()
        for m in THROW_RE.finditer(text):
            if in_comment(text, m.start()):
                continue
            line_no = text.count('\n', 0, m.start()) + 1
            if is_ignored(lines, line_no):
                ignored.append((rel, line_no))
                continue
            hits.append((rel, line_no))

    # 自证扫描到过东西：前缀写错会让本门禁退化成永远 [OK] 的空转守卫。
    if scanned == 0:
        print(f'[FAIL] 未扫描到任何 src/main/java 下 {PKG_PREFIX} 包的源文件；'
              f'PKG_PREFIX 或 --root 配置错误，门禁正在空转。')
        return 1
    print(f'[INFO] 扫描 {scanned} 个 {PKG_PREFIX} 包内的 src/main/java 源文件。')

    # 豁免项始终打印，保证它们在 CI 日志里可见、不会悄悄累积
    if ignored:
        print(f'[INFO] {len(ignored)} 处已通过 {IGNORE_MARK} 显式豁免（每条均带理由）：')
        for rel, ln in ignored:
            print(f'  {rel}:{ln}')

    if len(ignored) > MAX_EXEMPTIONS:
        print(f'[FAIL] 豁免总数 {len(ignored)} 已超过已审校上限 {MAX_EXEMPTIONS}：'
              f'新增豁免必须是一次有意识的修改，先审校理由并同步提高 MAX_EXEMPTIONS。')
        return 1

    if not hits:
        print('[OK] 未发现裸抛 RuntimeException/IllegalStateException（cn/zhicloud 包）。')
        return 0

    print(f'[FAIL] 发现 {len(hits)} 处裸抛异常（需统一为 ServiceException）：')
    for rel, ln in hits:
        print(f'  {rel}:{ln}')
    return 1


if __name__ == '__main__':
    sys.exit(main())
