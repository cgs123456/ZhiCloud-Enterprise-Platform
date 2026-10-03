"""V99 单据勾稽总额守卫的一次性变异测试脚本（非 CI 交付物，需要本机 Maven 可用）。

用法：python scripts/production/mutation_v99.py
每轮变异后按字节还原源文件，末尾自检还原结果；输出中文用 GBK 解码 Maven 控制台。
变异目标：证明新增单测真的能抓住“守卫位置 / 求和范围 / fail-closed / 落库基线”的回归，
而不是只跑通正向路径。
"""
import pathlib, re, subprocess, sys
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.stderr.reconfigure(encoding="utf-8", errors="replace")

root = pathlib.Path(__file__).resolve().parents[2]
impl = root / "zhicloud-module-erp/src/main/java/cn/zhicloud/module/erp/service/accounting/ErpAccountingEventServiceImpl.java"
orig = {p: p.read_bytes() for p in (impl,)}

GUARD_AFTER_IDEMPOTENCY = """        ErpAccountingEventDO existing = accountingEventMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return getExistingVoucherId(existing);
        }
        // 勾稽守卫必须放在幂等短路之后：反审核再重新审核会命中同一幂等键并直接返回原凭证，
        // 若先做累计判定，会把「原事件仍存在」误算成第二次占用额度而拒绝重新审核。
        BigDecimal docTotal = reversalOriginal == null
                ? resolveDocTotalAmount(request)
                : inheritDocTotalAmount(request, reversalOriginal);
"""
GUARD_BEFORE_IDEMPOTENCY = """        // MUTATION M1: 守卫提前到幂等短路之前
        BigDecimal docTotal = reversalOriginal == null
                ? resolveDocTotalAmount(request)
                : inheritDocTotalAmount(request, reversalOriginal);
        ErpAccountingEventDO existing = accountingEventMapper.selectByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return getExistingVoucherId(existing);
        }
"""

MUTATIONS = [
    ("M1 守卫提前到幂等短路之前（反审核→重新审核会被误判超额）",
     GUARD_AFTER_IDEMPOTENCY.encode("utf-8"), GUARD_BEFORE_IDEMPOTENCY.encode("utf-8")),
    ("M2 求和排除冲销事件（已释放额度不能再用）",
     b"            signedSum = signedSum.add(defaultZero(event.getAmountWithTax()));",
     b"            signedSum = defaultZero(event.getReversalOfEventId()) > 0 ? signedSum\n"
     b"                    : signedSum.add(defaultZero(event.getAmountWithTax()));"),
    ("M3 去掉超额判定（超额过账被放行）",
     b"        if (nextAbs.compareTo(docTotal) > 0) {", b"        if (false) {"),
    ("M4 缺基线时不再 fail-closed（用 0 兜底）",
     b"        BigDecimal docTotal = requested != null ? requested : baselineMin;",
     b"        BigDecimal docTotal = requested != null ? requested : BigDecimal.ZERO;"),
    ("M5 去掉请求与已落地基线的一致性判定",
     b"        if (requested != null && baselineMin != null && requested.compareTo(baselineMin) != 0) {",
     b"        if (false) {"),
    ("M6 事件不再落库勾稽总额基线",
     b"        event.setDocTotalAmount(docTotal);\n", b""),
    ("M7 冲销事件不再沿用原事件基线",
     b"        return requested != null ? requested : inherited;",
     b"        return requested;"),
]

KEEP = re.compile(r"Tests run:|<<< (FAILURE|ERROR)|BUILD (SUCCESS|FAILURE)|expected:|but was:")

for label, old, new in MUTATIONS:
    data = impl.read_bytes()
    n = data.count(old)
    if n != 1:
        print("\n### MUTATION:", label, "| SKIPPED, replacements =", n)
        continue
    impl.write_bytes(data.replace(old, new))
    print("\n### MUTATION:", label, "| replacements:", n)
    proc = subprocess.run(
        ["mvn.cmd", "-o", "-pl", "zhicloud-module-erp", "test",  # 无 -am：被改文件就在该模块内，见 §12.39
         "-Dtest=ErpAccountingEventServiceTest", "-DfailIfNoTests=false"],
        cwd=str(root), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    text = proc.stdout.decode("gbk", errors="replace")
    kept = [line for line in text.splitlines() if KEEP.search(line)]
    print("\n".join(kept[-14:]))
    print("MUTATION RESULT:", "TESTS_FAILED_AS_EXPECTED" if proc.returncode != 0 else "TESTS_PASSED_UNEXPECTEDLY")
    # 每轮立刻还原：否则下一轮变异会叠加在上一轮之上，结论不可用
    for path, data in orig.items():
        path.write_bytes(data)

for path, data in orig.items():
    path.write_bytes(data)
    print("restored:", path.name, path.read_bytes() == data)
