"""V100 可靠集成数据面（Outbox/Inbox/中继）的一次性变异测试脚本（非 CI 交付物，需要本机 Maven 可用）。

用法：python scripts/production/mutation_v100.py
每轮变异后按字节还原源文件，末尾自检还原结果；输出中文用 GBK 解码 Maven 控制台。
变异目标：证明新增单测真的能抓住「幂等登记 / 租约互斥 / 指数退避 / 死信阈值 / 消费去重 /
失败不静默 / 批次上限 / 异常截断」这些可靠性性质的回归，而不是只跑通正向路径。

关于 M8：早期版本把「去掉服务层 PROCESSED 短路判断」单独作为一个变异体，结果是
TESTS_PASSED_UNEXPECTEDLY。经核对这是等价变异体而非测试盲区：claim() 的 SQL
前置条件 status IN ('RECEIVED','FAILED') 与服务层判断语义重复，是同一道防线的第二份
实现，单独删除服务层判断后行为不可观测。因此拆成两个可观测变异：
  M8  = 仅第二道防线失效（claim 放宽到允许抢占 PROCESSED）——由 testProcessedRowCannotBeClaimedAgain 捕获；
  M16 = 两道防线同时失效（真实复合故障：重复执行业务处理器）——由 testConsumeOnceThenDuplicate 捕获。
"""
import pathlib, re, subprocess, sys
sys.stdout.reconfigure(encoding="utf-8", errors="replace")
sys.stderr.reconfigure(encoding="utf-8", errors="replace")

root = pathlib.Path(__file__).resolve().parents[2]
pkg = root / "zhicloud-framework/zhicloud-spring-boot-starter-biz-integration/src/main/java/cn/zhicloud/framework/integration"
files = {
    "outbox_impl": pkg / "service/EventOutboxServiceImpl.java",
    "outbox_mapper": pkg / "dal/mysql/EventOutboxMapper.java",
    "inbox_impl": pkg / "service/EventInboxServiceImpl.java",
    "inbox_mapper": pkg / "dal/mysql/EventInboxMapper.java",
    "relay": pkg / "service/EventRelayService.java",
    "relay_job": pkg / "job/EventRelayJob.java",
    "text": pkg / "util/EventTextUtils.java",
}
orig = {k: p.read_bytes() for k, p in files.items()}

CLAIM_GUARD_OLD = b"AND status IN ('\" + INBOX_RECEIVED + \"','\" + INBOX_FAILED + \"')"
CLAIM_GUARD_NEW = b"AND status IN ('\" + INBOX_RECEIVED + \"','\" + INBOX_FAILED + \"','\" + INBOX_PROCESSED + \"')"

MUTATIONS = [
    ("M1 幂等登记失效（同 eventId 重复登记不再复用同一行）", [
        ("outbox_impl",
         b"            EventOutboxDO existing = eventOutboxMapper.selectByEventId(req.getEventId());",
         b"            EventOutboxDO existing = null;"),
    ]),
    ("M2 领取扫描忽略退避窗口（next_retry_at 未到期也会被领取）", [
        ("outbox_mapper",
         b"AND (next_retry_at IS NULL OR next_retry_at <= #{now}) ORDER BY id LIMIT #{limit}",
         b"ORDER BY id LIMIT #{limit}"),
    ]),
    ("M3 领取不再设置租约（并发节点可立即重复领取同一事件）", [
        ("outbox_impl",
         b"return eventOutboxMapper.claim(id, base, base.plusSeconds(leaseSeconds())) == 1;",
         b"return eventOutboxMapper.claim(id, base, base) == 1;"),
    ]),
    ("M4 死信阈值放宽一位（maxRetry 次失败仍继续重试）", [
        ("outbox_impl",
         b"        if (nextRetry >= maxRetry()) {",
         b"        if (nextRetry > maxRetry()) {"),
    ]),
    ("M5 指数退避退化为固定退避", [
        ("outbox_impl",
         b"        long seconds = base * (1L << shift);",
         b"        long seconds = base;"),
    ]),
    ("M6 分区键不再默认 tenantId:aggregateId", [
        ("outbox_impl",
         b'? tenantId + ":" + req.getAggregateId() : req.getPartitionKey())',
         b'? "" : req.getPartitionKey())'),
    ]),
    ("M7 扫描丢失批次上限（单轮可能全表扫）", [
        ("outbox_mapper",
         b"ORDER BY id LIMIT #{limit}",
         b"ORDER BY id"),
    ]),
    ("M8 消费去重最后一道防线失效（claim 允许抢占已 PROCESSED 的记录）", [
        ("inbox_mapper", CLAIM_GUARD_OLD, CLAIM_GUARD_NEW),
    ]),
    ("M9 死信事件被静默跳过（故障消失在监控之外）", [
        ("inbox_impl",
         b"                throw ServiceExceptionUtil.exception(ErrorCodeConstants.EVENT_INBOX_DEAD);",
         b"                return ConsumeResult.DUPLICATE;"),
    ]),
    ("M10 消费失败不再递增重试计数（永远进不了死信）", [
        ("inbox_impl",
         b"            int nextRetry = (row.getRetryCount() == null ? 0 : row.getRetryCount()) + 1;",
         b"            int nextRetry = (row.getRetryCount() == null ? 0 : row.getRetryCount());"),
    ]),
    ("M11 投递完成忽略状态前置条件（未领取事件可被标记完成）", [
        ("outbox_impl",
         b"        return eventOutboxMapper.markPublished(id, LocalDateTime.now()) == 1;",
         b"        return true;"),
    ]),
    ("M12 异常信息不再截断到列宽（超长文本让 UPDATE 直接失败）", [
        ("text",
         b"        return value.length() <= maxLen ? value : value.substring(0, maxLen);",
         b"        return value;"),
    ]),
    ("M13 失败原因只记最外层包装，不再取根因", [
        ("text",
         b"        while (root.getCause() != null && root.getCause() != root) {",
         b"        while (false) {"),
    ]),
    ("M14 中继在消费失败时仍标记投递完成（事件静默丢失）", [
        ("relay",
         b"                eventOutboxService.markRetryOrDead(event.getId(), ex);",
         b"                eventOutboxService.markPublished(event.getId());"),
    ]),
    ("M15 消费成功不再落 processed_at（处理时间不可观测）", [
        ("inbox_mapper",
         b", processed_at = #{now}, error = NULL ",
         b", error = NULL "),
    ]),
    ("M17 中继定时任务空转（不调用 relayOnce，事件永不排空）", [
        ("relay_job",
         b"            int published = eventRelayService.relayOnce(0);",
         b"            int published = 0;"),
    ]),
    ("M16 消费去重两道防线同时失效（已 PROCESSED 事件被重复执行）", [
        ("inbox_impl",
         b"            if (INBOX_PROCESSED.equals(row.getStatus())) {",
         b"            if (false) {"),
        ("inbox_mapper", CLAIM_GUARD_OLD, CLAIM_GUARD_NEW),
    ]),
]

KEEP = re.compile(r"Tests run:|<<< (FAILURE|ERROR)|BUILD (SUCCESS|FAILURE)|expected:|but was:")


def restore_all():
    for k, raw in orig.items():
        files[k].write_bytes(raw)


for label, ops in MUTATIONS:
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
        continue
    print("\n### MUTATION:", label, "| replacements:", len(ops))
    proc = subprocess.run(
        ["mvn.cmd", "-o", "-pl", "zhicloud-framework/zhicloud-spring-boot-starter-biz-integration", "test"],
        cwd=str(root), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    text = proc.stdout.decode("gbk", errors="replace")
    kept = [line for line in text.splitlines() if KEEP.search(line)]
    print("\n".join(kept[-10:]))
    print("MUTATION RESULT:", "TESTS_FAILED_AS_EXPECTED" if proc.returncode != 0 else "TESTS_PASSED_UNEXPECTEDLY")
    # 每轮立刻还原：否则下一轮变异会叠加在上一轮之上，结论不可用
    restore_all()

restore_all()
for k, raw in orig.items():
    print("restored:", files[k].name, files[k].read_bytes() == raw)