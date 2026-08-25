#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
电子签名管控点存活门禁（21 CFR Part 11）

背景（这个脚本存在的原因）：
    zhicloud-module-qms 里 @ElectronicSignature 注解 + ElectronicSignatureAspect 切面
    实现得很完整——fail-closed、审计留痕、理由校验一应俱全，代码评审也挑不出毛病。
    但全仓 grep 下来，这个注解的<实际使用次数是 0>：切面从未被触发，
    是一段彻头彻尾的死代码。也就是说 21 CFR Part 11 要求的电子签名管控点
    在运行时根本不存在，而静态检查、单测、人工评审全都没有发现——
    因为大家看的都是「实现对不对」，没人问「它到底有没有被挂上去」。

    这类「设施齐备但未接线」的缺陷，是合规审计里最典型也最致命的一种：
    上线前所有文档都能自证合规，上线后第一次 FDA/客户审计就会被当场击穿。

门禁策略：
    1. 注解使用数不得低于基线（--min-usages），防止有人「顺手删掉」重新变回死代码；
    2. 关键管控点清单（REQUIRED_SIGNATURE_POINTS）逐一核验，缺一即失败。
       清单里的方法是质量体系中不可抵赖的终态操作：文件批准/发布、变更批准、
       NCR 关闭、CAPA 关闭、8D 关闭、审核不符合项验证与关闭。

用法：
    python3 scripts/check_electronic_signature.py
    python3 scripts/check_electronic_signature.py --min-usages 7
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
QMS_SRC = os.path.join('zhicloud-module-qms', 'src', 'main', 'java')

ANNOTATION_RE = re.compile(r'@ElectronicSignature\s*\(')
# 注解定义文件本身不算「使用」
ANNOTATION_DEF = 'framework/electronicsignature/ElectronicSignature.java'
ASPECT_FILE = 'framework/electronicsignature/ElectronicSignatureAspect.java'

# 必须挂签名的关键管控点：(Controller 相对路径片段, 方法名)
REQUIRED_SIGNATURE_POINTS = [
    ('controller/admin/document/QmsDocumentController.java', 'approveDocument'),
    ('controller/admin/document/QmsDocumentController.java', 'rejectDocument'),
    ('controller/admin/document/QmsDocumentChangeRequestController.java', 'approveChangeRequest'),
    ('controller/admin/ncr/NcrDocumentController.java', 'closeNcrDocument'),
    ('controller/admin/capa/CAPADocumentController.java', 'closeCAPADocument'),
    ('controller/admin/eightd/EightDReportController.java', 'closeEightDReport'),
    ('controller/admin/audit/QmsAuditNonconformityController.java', 'verifyNonconformity'),
    ('controller/admin/audit/QmsAuditNonconformityController.java', 'closeNonconformity'),
]

METHOD_DECL_RE = re.compile(r'\b(?:public|protected)\s+[\w<>,\[\]\s\.]+\s+(\w+)\s*\(')


def strip_comments(lines):
    """去掉 // 与 /* */ 注释，避免 Javadoc 里提到注解名被误判为「已使用」"""
    out = []
    in_block = False
    for line in lines:
        buf = []
        i = 0
        while i < len(line):
            two = line[i:i + 2]
            if in_block:
                if two == '*/':
                    in_block = False
                    i += 2
                    continue
                i += 1
                continue
            if two == '/*':
                in_block = True
                i += 2
                continue
            if two == '//':
                break
            buf.append(line[i])
            i += 1
        out.append(''.join(buf))
    return out


def iter_java_files():
    base = os.path.join(ROOT, QMS_SRC)
    if not os.path.isdir(base):
        return
    for dirpath, _dirnames, filenames in os.walk(base):
        for fn in filenames:
            if fn.endswith('.java'):
                full = os.path.join(dirpath, fn)
                rel = os.path.relpath(full, base).replace('\\', '/')
                yield full, rel


def annotated_methods(lines):
    """返回文件中被 @ElectronicSignature 标注的方法名集合"""
    found = set()
    n = len(lines)
    for i, line in enumerate(lines):
        if not ANNOTATION_RE.search(line):
            continue
        # 向下找最近的方法声明（跳过其它注解行）
        for j in range(i + 1, min(i + 12, n)):
            m = METHOD_DECL_RE.search(lines[j])
            if m:
                found.add(m.group(1))
                break
    return found


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--min-usages', type=int, default=len(REQUIRED_SIGNATURE_POINTS),
                    help='@ElectronicSignature 最少使用处数（默认=关键管控点数量）')
    args = ap.parse_args()

    total_usages = 0
    per_file = {}
    aspect_exists = False

    for full, rel in iter_java_files():
        if rel.endswith(ASPECT_FILE.split('/')[-1]) and 'electronicsignature' in rel:
            aspect_exists = True
        if rel.endswith(ANNOTATION_DEF.split('/')[-1]) and 'framework' in rel:
            continue  # 注解定义文件本身不计入
        with open(full, 'r', encoding='utf-8', errors='ignore') as f:
            lines = strip_comments(f.readlines())
        # 切面里的 @annotation(electronicSignature) 不是「使用」，排除
        if 'framework/electronicsignature' in rel:
            continue
        methods = annotated_methods(lines)
        if methods:
            per_file[rel] = methods
            total_usages += len(methods)

    problems = []

    if not aspect_exists:
        problems.append('ElectronicSignatureAspect 切面文件缺失——签名将完全失效')

    if total_usages < args.min_usages:
        problems.append(
            f'@ElectronicSignature 实际使用 {total_usages} 处，低于基线 {args.min_usages}。'
            f'注解一旦归零，切面即成死代码，Part 11 管控点在运行时消失。')

    for path_frag, method in REQUIRED_SIGNATURE_POINTS:
        hit = False
        for rel, methods in per_file.items():
            if rel.endswith(path_frag.split('/')[-1]) and method in methods:
                hit = True
                break
        if not hit:
            problems.append(f'关键管控点缺少电子签名：{path_frag}#{method}')

    if problems:
        print(f'[FAIL] 电子签名门禁未通过（当前使用 {total_usages} 处）：')
        for p in problems:
            print(f'  - {p}')
        print('  → 电子签名是 21 CFR Part 11 的强制管控点，'
              '不得只保留实现而不挂载，也不得从关键操作上摘除。')
        return 1

    print(f'[OK] 电子签名门禁通过：@ElectronicSignature 已挂载 {total_usages} 处，'
          f'{len(REQUIRED_SIGNATURE_POINTS)} 个关键管控点全部覆盖。')
    for rel in sorted(per_file):
        print(f'  {rel}: {", ".join(sorted(per_file[rel]))}')
    return 0


if __name__ == '__main__':
    sys.exit(main())
