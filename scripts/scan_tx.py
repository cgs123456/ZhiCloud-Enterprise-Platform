# -*- coding: utf-8 -*-
"""
扫描 Service 实现类中「单个方法内写入 >= 2 个不同 Mapper / Service，但未标注 @Transactional」的方法。
这类方法在中途异常时会产生脏数据（部分表已写、部分表未写），属于原子性缺陷。

用法: python scripts/scan_tx.py <module-dir> [<module-dir> ...]
"""
import os
import re
import sys

# 写操作方法名（MyBatis-Plus BaseMapper + zhicloud 扩展）
WRITE_METHODS = (
    "insert", "insertBatch", "insertOrUpdate",
    "update", "updateById", "updateBatch",
    "delete", "deleteById", "deleteByIds", "deleteBatchIds",
    "save", "saveBatch", "remove", "removeById",
)
WRITE_RE = re.compile(
    r"\b(\w+(?:Mapper|Service))\s*\.\s*(" + "|".join(WRITE_METHODS) + r")\w*\s*\(")
# 方法签名：public/private/protected 返回值 方法名(
METHOD_RE = re.compile(
    r"^\s*(?:@\w+[^\n]*\n\s*)*"
    r"(public|protected|private)\s+[\w<>\[\],\s\.\?]+\s+(\w+)\s*\([^;{]*\)\s*\{")

# 视为「已具备事务语义」的注解。不止 @Transactional：
# 多数据源场景使用 dynamic-datasource 的 @DSTransactional（同样提供本地事务 + 数据源切换），
# 分布式事务场景使用 Seata 的 @GlobalTransactional。漏掉它们会产生大量误报。
TX_ANNOTATIONS = ("@Transactional", "@DSTransactional", "@GlobalTransactional")

# 显式豁免标记。少数方法内含 sleep / 外部 HTTP / 文件下载，包事务会长时间占用数据库连接，
# 危害大于收益（典型：MpMessageServiceImpl.receiveMessage 含 15s 轮询 + 微信 API + 媒体下载）。
# 这类方法在方法签名上方注释中标注 `// @tx-ignore 原因`，扫描器跳过，
# 同时保证豁免理由留在代码里、可被 code review 审视，而不是藏在脚本白名单中。
IGNORE_MARK = "@tx-ignore"


def scan_file(path):
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        lines = f.readlines()

    findings = []
    i = 0
    n = len(lines)
    while i < n:
        line = lines[i]
        m = re.match(r"^\s{4}(public|protected)\s+[\w<>\[\],\s\.\?]+\s+(\w+)\s*\(", line)
        if not m:
            i += 1
            continue
        method_name = m.group(2)
        # 向上回溯收集注解。
        # 注意：必须处理「跨行注解」，例如
        #     @Transactional(rollbackFor = Exception.class)
        #     @LogRecord(type = X, subType = Y, bizNo = "...",
        #             success = Z)
        #     public Long createBusiness(...)
        # 从签名行往上第一行是 `success = Z)`——它既不以 @ 开头也不是注释，
        # 早期实现在此直接 break，导致更上方的 @Transactional 被漏看（假阳性）。
        # 解法：用括号配平判断是否仍处于某个注解的续行中。
        # 注意：注释行也要收集，因为豁免标记 `// @tx-ignore` 就写在注释里。
        preamble = []
        j = i - 1
        paren_depth = 0
        while j >= 0:
            s = lines[j].strip()
            paren_depth += s.count(")") - s.count("(")
            if paren_depth > 0:
                # 右括号多于左括号 => 该注解在更上方才开始，属续行，继续向上
                preamble.append(s)
                j -= 1
                continue
            if s.startswith("@") or s.startswith("*") or s.startswith("/*") \
                    or s.startswith("//") or s == "":
                preamble.append(s)
                j -= 1
            else:
                break
        has_tx = any(any(t in s for t in TX_ANNOTATIONS) for s in preamble)
        ignored = any(IGNORE_MARK in s for s in preamble)

        # 找方法体（花括号配平）
        depth = 0
        started = False
        body_start = i
        k = i
        while k < n:
            depth += lines[k].count("{") - lines[k].count("}")
            if "{" in lines[k]:
                started = True
            if started and depth <= 0:
                break
            k += 1
        body = "".join(lines[body_start:k + 1])

        writes = WRITE_RE.findall(body)
        targets = sorted(set(w[0] for w in writes))
        if len(targets) >= 2 and not has_tx and not ignored:
            findings.append((i + 1, method_name, targets, len(writes)))
        i = k + 1
    return findings


def main():
    total = 0
    for root_dir in sys.argv[1:]:
        for dirpath, _, filenames in os.walk(root_dir):
            if os.sep + "test" + os.sep in dirpath:
                continue
            for fn in filenames:
                if not fn.endswith("ServiceImpl.java"):
                    continue
                p = os.path.join(dirpath, fn)
                res = scan_file(p)
                if res:
                    rel = os.path.relpath(p, ".")
                    print("\n" + rel)
                    for ln, name, targets, cnt in res:
                        total += 1
                        print("  L%-5d %-40s writes=%d  ->  %s"
                              % (ln, name, cnt, ", ".join(targets)))
    print("\n==== 合计 %d 个方法缺少 @Transactional ====" % total)
    if total:
        print("修复方式二选一：")
        print("  1) 补 @Transactional(rollbackFor = Exception.class)；")
        print("  2) 若方法内含 sleep / 外部 HTTP / 文件下载等长耗时操作，包事务弊大于利，")
        print("     则在方法签名上方注释写明 `// @tx-ignore <理由>` 显式豁免。")
    return 1 if total else 0


if __name__ == "__main__":
    # 以退出码反馈结果，便于直接作为 CI 门禁使用（非 0 即阻断构建）
    sys.exit(main())
