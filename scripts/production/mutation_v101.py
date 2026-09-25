"""V101「ERP 销售出库审核通过 -> WMS 出库单 + MDM 主数据映射」的一次性变异测试脚本。

用法：python scripts/production/mutation_v101.py
每轮变异只改一处源码，跑对应模块的单测，然后按字节还原；末尾自检还原结果（全 True 才可提交）。
变异目标：证明新增单测真的能抓住「映射缺失必须失败 / 映射幂等 / 失效映射不参与解析 /
租户隔离 / 反审核不发事件 / eventId 可推导 / 业务级幂等 / 可选映射不阻断 / 跨仓库不静默 /
单号可推导 / 事件契约匹配」这些性质的回归，而不是只跑通正向路径。
"""
import pathlib, re, subprocess, sys
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.stderr.reconfigure(encoding="utf-8", errors="replace")

root = pathlib.Path(__file__).resolve().parents[2]
mdm_pkg = root / "zhicloud-framework/zhicloud-spring-boot-starter-biz-mdm/src/main/java/cn/zhicloud/framework/mdm"
int_pkg = root / "zhicloud-framework/zhicloud-spring-boot-starter-biz-integration/src/main/java/cn/zhicloud/framework/integration"

files = {
    "mdm_impl": mdm_pkg / "service/MdmCrossRefServiceImpl.java",
    "mdm_mapper": mdm_pkg / "dal/mysql/MdmCrossRefMapper.java",
    "event_contract": int_pkg / "contract/ErpSaleOutApprovedEvent.java",
    "erp_sale_out": root / "zhicloud-module-erp/src/main/java/cn/zhicloud/module/erp/service/sale/ErpSaleOutServiceImpl.java",
    "wms_consumer": root / "zhicloud-module-wms/src/main/java/cn/zhicloud/module/wms/service/integration/ErpSaleOutApprovedConsumer.java",
}
orig = {k: p.read_bytes() for k, p in files.items()}

CMD_MDM = ["mvn.cmd", "-o", "-pl", "zhicloud-framework/zhicloud-spring-boot-starter-biz-mdm", "test"]
CMD_ERP = ["mvn.cmd", "-o", "-pl", "zhicloud-module-erp", "test"]
CMD_WMS = ["mvn.cmd", "-o", "-pl", "zhicloud-module-wms", "test", "-Dtest=ErpSaleOutApprovedConsumerTest"]
# 事件契约属于集成 Starter：ERP 模块按 -pl 单跑时依赖的是已安装 jar 而不是 Starter 源码，
# 因此改 Starter 源码必须回 Starter 自己的测试里验证，改 ERP 源码才在 ERP 模块里验证。
CMD_INT = ["mvn.cmd", "-o", "-pl", "zhicloud-framework/zhicloud-spring-boot-starter-biz-integration",
           "test", "-Dtest=ErpSaleOutApprovedEventTest"]

MUTATIONS = [
    ("M1 MDM 映射缺失不再抛错（静默返回 null，下游会建到错误 SKU 上）", CMD_MDM, [
        ("mdm_impl",
         b"throw exception(MDM_CROSS_REF_NOT_FOUND, sourceSystem, sourceEntity, sourceId, targetSystem, targetEntity);",
         b"return null;"),
    ]),
    ("M2 MDM 登记失去幂等（同一源键重复登记新增第二行）", CMD_MDM, [
        ("mdm_impl",
         b"req.getSourceId(), req.getTargetSystem(), req.getTargetEntity());\n        if (existing == null) {",
         b"req.getSourceId(), req.getTargetSystem(), req.getTargetEntity());\n        if (true) {"),
    ]),
    ("M3 MDM 失效映射参与解析（已废弃映射继续写下游单据）", CMD_MDM, [
        ("mdm_impl",
         b"if (ref == null || !STATUS_VALID.equals(ref.getStatus())) {",
         b"if (ref == null) {"),
    ]),
    ("M4 MDM 解析丢失租户条件（跨租户串数据）", CMD_MDM, [
        ("mdm_mapper",
         b".eq(MdmCrossRefDO::getTenantId, tenantId)\n                .eq(MdmCrossRefDO::getSourceSystem, sourceSystem)\n                .eq(MdmCrossRefDO::getSourceEntity, sourceEntity)\n                .eq(MdmCrossRefDO::getSourceId, sourceId)\n                .eq(MdmCrossRefDO::getTargetSystem, targetSystem)\n                .eq(MdmCrossRefDO::getTargetEntity, targetEntity));",
         b".eq(MdmCrossRefDO::getSourceSystem, sourceSystem)\n                .eq(MdmCrossRefDO::getSourceEntity, sourceEntity)\n                .eq(MdmCrossRefDO::getSourceId, sourceId)\n                .eq(MdmCrossRefDO::getTargetSystem, targetSystem)\n                .eq(MdmCrossRefDO::getTargetEntity, targetEntity));"),
    ]),
    ("M5 ERP 去掉审核通过守卫（反审核也登记事件，等于告诉 WMS 发生过出库）", CMD_ERP, [
        ("erp_sale_out",
         b"if (approve) {\n            eventOutboxService.append(",
         b"if (true) {\n            eventOutboxService.append("),
    ]),
    ("M6 事件幂等键不再可推导（同一单据重试得到不同 eventId）", CMD_INT, [
        ("event_contract",
         b'return "erp-sale-out-" + saleOutId + "-approved";',
         b'return "erp-sale-out-" + saleOutId + "-approved-" + System.currentTimeMillis();'),
    ]),
    ("M7 WMS 消费者去掉业务单号查重（重复投递建出第二张出库单）", CMD_WMS, [
        ("wms_consumer",
         b"if (shipmentOrderMapper.selectByBizOrderNo(payload.getNo()) != null) {",
         b"if (false) {"),
    ]),
    ("M8 WMS 消费者把可选客户映射当必填（映射缺失阻断出库）", CMD_WMS, [
        ("wms_consumer",
         b"String merchantIdText = mdmCrossRefService.findTargetId(",
         b"String merchantIdText = mdmCrossRefService.mapTargetId("),
    ]),
    ("M9 WMS 消费者跨仓库检查失效（第二个仓库的货按第一个仓库建单）", CMD_WMS, [
        ("wms_consumer",
         b"} else if (!Objects.equals(warehouseId, itemWarehouseId)) {",
         b"} else if (false) {"),
    ]),
    ("M10 WMS 出库单号不可推导（加时间戳，重试会绕开单号唯一校验）", CMD_WMS, [
        ("wms_consumer",
         b'return "WS" + erpNo;',
         b'return "WS" + erpNo + "-" + System.currentTimeMillis();'),
    ]),
    ("M11 WMS 消费者事件类型匹配放宽（错误版本事件被静默处理）", CMD_WMS, [
        ("wms_consumer",
         b"return IntegrationEventTypes.ERP_SALE_OUT_APPROVED.equals(eventType);",
         b'return "erp.sale-out.approved.v1".equals(eventType) || "erp.sale-out.approved.v2".equals(eventType);'),
    ]),
]

KEEP = re.compile(r"Tests run:|<<< (FAILURE|ERROR)|BUILD (SUCCESS|FAILURE)|expected:|but was:|ERROR\]   ")


def restore_all():
    for k, raw in orig.items():
        files[k].write_bytes(raw)


only = set(sys.argv[1:])
survivors = []
for label, cmd, ops in MUTATIONS:
    if only and label.split(" ")[0] not in only:
        continue
    applied = True
    for key, old, new in ops:
        path = files[key]
        data = path.read_bytes()
        n = data.count(old)
        if n != 1:
            print("\n### MUTATION:", label, "| SKIPPED, target =", key, "replacements =", n)
            applied = False
            break
        path.write_bytes(data.replace(old, new))
    if not applied:
        restore_all()
        survivors.append(label + " (SKIPPED)")
        continue
    print("\n### MUTATION:", label)
    proc = subprocess.run(cmd, cwd=str(root), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    text = proc.stdout.decode("gbk", errors="replace")
    kept = [line for line in text.splitlines() if KEEP.search(line)]
    print("\n".join(kept[-8:]))
    killed = proc.returncode != 0
    print("MUTATION RESULT:", "TESTS_FAILED_AS_EXPECTED" if killed else "TESTS_PASSED_UNEXPECTEDLY")
    if not killed:
        survivors.append(label)
    restore_all()

restore_all()
print("\n=== SUMMARY ===")
print("total:", len(MUTATIONS), "survivors:", len(survivors))
for s in survivors:
    print("SURVIVOR:", s)
for k, raw in orig.items():
    print("restored:", files[k].name, files[k].read_bytes() == raw)