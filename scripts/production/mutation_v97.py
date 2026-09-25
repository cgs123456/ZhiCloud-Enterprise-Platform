"""V97 退货过账规则的一次性变异测试脚本（非 CI 交付物，需要本机 Maven 可用）。

用法：python scripts/production/mutation_v97.py
每轮变异后按字节还原源文件，末尾自检还原结果；输出中文用 GBK 解码 Maven 控制台。
"""
import pathlib, re, subprocess, sys
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.stderr.reconfigure(encoding="utf-8", errors="replace")
root = pathlib.Path(__file__).resolve().parents[2]
sale = root / "zhicloud-module-erp/src/main/java/cn/zhicloud/module/erp/service/sale/ErpSaleReturnServiceImpl.java"
pur = root / "zhicloud-module-erp/src/main/java/cn/zhicloud/module/erp/service/purchase/ErpPurchaseReturnServiceImpl.java"
mig = root / "zhicloud-server/src/main/resources/db/migration/V97__erp_return_posting_rule_seed.sql"
ver = root / "zhicloud-server/src/main/resources/db/verification/V97_verify.sql"
orig = {p: p.read_bytes() for p in (sale, pur, mig, ver)}

HOOK_SALE = """        if (approve) {
            accountingEventService.postEvent(buildSaleReturnEvent(saleReturn, saleReturnItems));
        } else if (ErpAuditStatus.APPROVE.getStatus().equals(saleReturn.getStatus())) {
            accountingEventService.reverseEvent(buildSaleReturnReversalRequest(saleReturn));
        }
"""
HOOK_PUR = """        if (approve) {
            accountingEventService.postEvent(buildPurchaseReturnEvent(purchaseReturn));
        } else if (ErpAuditStatus.APPROVE.getStatus().equals(purchaseReturn.getStatus())) {
            accountingEventService.reverseEvent(buildPurchaseReturnReversalRequest(purchaseReturn));
        }
"""

SALES_MAP = "'10:amountWithoutTax|10:taxAmount|20:amountWithTax|10:costAmount|20:costAmount'"
SALES_MAP_MUT = "'10:amountWithoutTax|10:taxAmount|10:amountWithTax|10:costAmount|20:costAmount'"

MUTATIONS = [
    ("M1 sale return: remove accounting hook", sale,
     HOOK_SALE.encode("utf-8"), b"", "zhicloud-module-erp",
     "ErpSaleReturnStatusTest"),
    ("M2 sale return: event type becomes SALES_SHIPMENT_POSTED", sale,
     b'"SALES_RETURN_POSTED"', b'"SALES_SHIPMENT_POSTED"', "zhicloud-module-erp",
     "ErpSaleReturnStatusTest"),
    ("M3 purchase return: remove accounting hook", pur,
     HOOK_PUR.encode("utf-8"), b"", "zhicloud-module-erp",
     "ErpPurchaseReturnStatusTest"),
    ("M4 V97: flip sales line 3 direction to debit", mig,
     b"@v97_sales_return_rule_id, 3, 20, '1122'", b"@v97_sales_return_rule_id, 3, 10, '1122'",
     "zhicloud-server", "ErpReturnPostingRuleSeedFlywayMigrationContractTest"),
    ("M5 V97_verify: expected sales lines 5 -> 4", ver,
     b"'ERP_SALES_RETURN_POSTED', 'SALES_RETURN_POSTED', 5",
     b"'ERP_SALES_RETURN_POSTED', 'SALES_RETURN_POSTED', 4",
     "zhicloud-server", "ErpReturnPostingRuleSeedFlywayMigrationContractTest"),
    ("M6 V97: drop purchase return rule seed", mig,
     b"ERP_PURCHASE_RETURN_POSTED", b"ERP_PURCHASE_RETURN_OTHER",
     "zhicloud-server", "ErpReturnPostingRuleSeedFlywayMigrationContractTest"),
    ("M7 V97_verify: expected sales side map flips line 3 to debit", ver,
     SALES_MAP.encode("utf-8"), SALES_MAP_MUT.encode("utf-8"),
     "zhicloud-server", "ErpReturnPostingRuleSeedFlywayMigrationContractTest"),
    ("M8 V97: flip sales line 1 direction to credit", mig,
     b"@v97_sales_return_rule_id, 1, 10, '6001'", b"@v97_sales_return_rule_id, 1, 20, '6001'",
     "zhicloud-server", "ErpReturnPostingRuleSeedFlywayMigrationContractTest"),
    ("M9 V97: purchase line 2 account 22210101 -> 22210102", mig,
     b"@v97_purchase_return_rule_id, 2, 20, '22210101'",
     b"@v97_purchase_return_rule_id, 2, 20, '22210102'",
     "zhicloud-server", "ErpReturnPostingRuleSeedFlywayMigrationContractTest"),
    ("M10 V97_verify: checked_lines gate 8 -> 7", ver,
     b"lg.checked_lines = 8", b"lg.checked_lines = 7",
     "zhicloud-server", "ErpReturnPostingRuleSeedFlywayMigrationContractTest"),
    ("M11 V97_verify: drop direction gate from verdict", ver,
     b"                  AND rg.wrong_direction_maps = 0\n", b"",
     "zhicloud-server", "ErpReturnPostingRuleSeedFlywayMigrationContractTest"),
    ("M12 V97: drop sales line 5 (credit cost) entirely", mig,
     b"@v97_sales_return_rule_id, 5, 20, '6401'", b"@v97_sales_return_rule_id, 5, 20, '9999'",
     "zhicloud-server", "ErpReturnPostingRuleSeedFlywayMigrationContractTest"),
]

KEEP = re.compile(r"Tests run:.*(Failures: [1-9]|Errors: [1-9])|<<< (FAILURE|ERROR)|BUILD (SUCCESS|FAILURE)|"
                  r"migrationMust\w+|verificationMust\w+|expected:|but was:|\[ERROR\].*\.java")

for label, path, old, new, module, tests in MUTATIONS:
    data = path.read_bytes()
    n = data.count(old)
    path.write_bytes(data.replace(old, new))
    print("\n### MUTATION:", label, "| replacements:", n)
    try:
        r = subprocess.run(
            f"mvn -B -o -pl {module} -Dtest={tests} -Dsurefire.failIfNoSpecifiedTests=false test",
            cwd=str(root), shell=True, capture_output=True)
        out = (r.stdout + r.stderr).decode("gbk", errors="replace")
        keep = [l.strip() for l in out.splitlines() if KEEP.search(l)]
        if not keep:
            print("!!! NO TEST RESULT LINES")
        for l in keep[:12]:
            print("   ", l[:200])
    finally:
        path.write_bytes(orig[path])
print("\nall restored")
for p in (sale, pur, mig, ver):
    print(p.name, "restored-ok" if p.read_bytes() == orig[p] else "RESTORE-FAILED")
