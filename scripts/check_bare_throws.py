#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
裸抛异常回归门禁：扫描 src/main 下 cn/iocoder/yudao/** 包内是否残留
    throw new RuntimeException(...)
    throw new IllegalStateException(...)
若存在则失败（退出码 1），输出位置清单，防止绕过 ErrorCode 体系。

排除：org/springframework、org/flowable 等「重打包外部类」（与 convert_bare_throws.py 一致）。

豁免机制（@bare-throw-ignore）：
    极少数代码运行在 Spring 容器之外（如 Flyway 迁移 CLI 的 main 方法），
    这类场景硬套 ServiceException 反而有害——ServiceException 必须携带一个注册在全局
    错误码表里的 ErrorCode，而该错误永远不会返回给任何 API 调用方，
    平白污染业务错误码空间。此时应在 throw 所在行或其上方 3 行内写：
        // @bare-throw-ignore <理由>
    豁免必须写明理由、随代码进入 review，且脚本会统计并打印豁免总数，
    避免它们悄悄累积成新的技术债。

用法：
    python3 scripts/check_bare_throws.py
    python3 scripts/check_bare_throws.py --root /path/to/repo
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PKG_PREFIX = 'cn/iocoder/yudao/'
THROW_RE = re.compile(r'throw\s+new\s+(RuntimeException|IllegalStateException)\s*\(', re.DOTALL)
IGNORE_MARK = '@bare-throw-ignore'
# 豁免标记允许写在 throw 所在行，或其上方最多 3 行内（留出写多行理由的空间）
IGNORE_LOOKBACK = 3


def in_comment(text, pos):
    # 块注释：pos 之前最近的 /* 没有匹配的 */ 闭合
    last_block_open = text.rfind('/*', 0, pos)
    if last_block_open != -1:
        last_block_close = text.rfind('*/', 0, pos)
        if last_block_close < last_block_open:
            return True
    # 行注释：同一行内 pos 之前有 //
    line_start = text.rfind('\n', 0, pos) + 1
    if '//' in text[line_start:pos]:
        return True
    return False


def is_ignored(lines, line_no):
    """判断第 line_no 行(1-based)的裸抛是否被 @bare-throw-ignore 显式豁免。

    命中范围：该行本身，或其上方 IGNORE_LOOKBACK 行内的任意一行。
    """
    start = max(0, line_no - 1 - IGNORE_LOOKBACK)
    return any(IGNORE_MARK in ln for ln in lines[start:line_no])


def iter_main_java():
    for dirpath, _dirs, files in os.walk(ROOT):
        for fn in files:
            if not fn.endswith('.java'):
                continue
            full = os.path.join(dirpath, fn)
            rel = os.path.relpath(full, ROOT).replace('\\', '/')
            mi = rel.find('/src/main/java/')
            if mi == -1:
                continue
            pkg_rel = rel[mi + len('/src/main/java/'):]
            if not pkg_rel.startswith(PKG_PREFIX):
                continue
            yield full, rel


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', default=ROOT)
    args = ap.parse_args()

    hits = []
    ignored = []
    for full, rel in iter_main_java():
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

    # 豁免项始终打印，保证它们在 CI 日志里可见、不会悄悄累积
    if ignored:
        print(f'[INFO] {len(ignored)} 处已通过 {IGNORE_MARK} 显式豁免（运行在 Spring 容器之外）：')
        for rel, ln in ignored:
            print(f'  {rel}:{ln}')

    if not hits:
        print('[OK] 未发现裸抛 RuntimeException/IllegalStateException（cn/iocoder/yudao 包）。')
        return 0

    print(f'[FAIL] 发现 {len(hits)} 处裸抛异常（需统一为 ServiceException）：')
    for rel, ln in hits:
        print(f'  {rel}:{ln}')
    return 1


if __name__ == '__main__':
    sys.exit(main())
