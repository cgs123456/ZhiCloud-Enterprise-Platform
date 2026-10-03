package cn.zhicloud.server.migration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 可靠集成数据面（event_outbox / event_inbox）Flyway 迁移契约测试。
 *
 * <p>V100 只建结构：发件箱解决「业务事务与事件原子提交」，收件箱解决「消费侧幂等去重」。
 * 这两张表一旦在迁移阶段被削弱（去掉唯一键、改成 ENUM 状态、加外键），
 * 应用层再怎么补也拿不回「至少一次投递 + 幂等消费」的保证，因此用契约测试把关键性质锁死。</p>
 *
 * <p>为什么 payload 用 longtext 而不是 json：json 类型在 H2 兼容模式与部分历史 MySQL 版本上
 * 行为不一致，单元测试需要与生产同构；代价是数据库层不做格式校验，由应用层 JsonUtils 补偿。</p>
 *
 * <p>为什么 status 用 varchar 而不是 enum：enum 加状态要改列，违反「迁移只做加法」原则，
 * 且改列在大数据量表上是高危操作。状态机由应用层常量约束。</p>
 */
class EventOutboxInboxFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V100__event_outbox_inbox.sql";
    private static final String VERIFICATION = "db/verification/V100_verify.sql";

    @Test
    void migrationMustCreateBothTablesIdempotently() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "create table if not exists `event_outbox`",
                "create table if not exists `event_inbox`",
                "engine = innodb",
                "default charset = utf8mb4");

        // 发件箱：事件唯一键 + 领取扫描路径 + 分区键
        assertContainsAll(sql,
                "unique key `uk_event_id` (`event_id`)",
                "key `idx_status_next_retry` (`status`, `next_retry_at`, `id`)",
                "key `idx_tenant_aggregate` (`tenant_id`, `aggregate_type`, `aggregate_id`, `occurred_at`)",
                "`event_id` varchar(64) not null",
                "`partition_key` varchar(128) not null default ''",
                "`payload` longtext not null",
                "`status` varchar(16) not null default 'new'",
                "`next_retry_at` datetime default null",
                "`last_error` varchar(2000) default null");

        // 收件箱：同消费者同事件只处理一次
        assertContainsAll(sql,
                "unique key `uk_consumer_event` (`consumer_name`, `event_id`)",
                "key `idx_status_processed` (`status`, `processed_at`)",
                "key `idx_tenant_event` (`tenant_id`, `event_id`)",
                "`consumer_name` varchar(64) not null",
                "`status` varchar(16) not null default 'received'");

        // BaseDO 兼容字段：逻辑删除与审计列缺失会让 Starter 的软删除/填充直接报错
        assertContainsAll(sql,
                "`deleted` bit not null default 0",
                "`create_time` datetime not null default current_timestamp",
                "`update_time` datetime not null default current_timestamp on update current_timestamp");
    }

    @Test
    void migrationMustNotWeakenReliabilityGuarantees() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertFalse(sql.contains("drop table"), "迁移不得删表");
        assertFalse(sql.contains("alter table"), "V100 只新增表，不得变更既有表结构");
        assertFalse(sql.contains("insert into"), "迁移不得写入业务/事件数据");
        assertFalse(sql.contains("foreign key"),
                "Outbox/Inbox 不得加外键：业务单据被清理时会连带阻断事件归档");
        // 只判定 DDL 形式：迁移注释里会解释「为什么不用 ENUM」，不能用裸 "enum" 误伤注释
        assertFalse(sql.contains("enum("),
                "status 必须是 varchar：enum 加状态需要改列，违反迁移只做加法原则");
        assertFalse(sql.contains("status enum"),
                "status 必须是 varchar：enum 加状态需要改列，违反迁移只做加法原则");
        assertFalse(sql.contains("partition by"),
                "阶段 1 不做分区：数据量未到阈值，先靠索引覆盖扫描，避免提前设计");
    }

    @Test
    void verificationMustGateStructureAndIndexes() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "tmp_v100_verify_expected_column",
                "tmp_v100_verify_expected_index",
                "information_schema.columns",
                "information_schema.statistics",
                "column_verdict",
                "index_verdict",
                "v100_verify_verdict",
                "'pass'",
                "'fail'",
                "'missing'",
                "'wrong'");

        // 去重能力与扫描路径必须逐个校验，缺任一都要 FAIL
        assertContainsAll(sql,
                "'uk_event_id', 'event_id'",
                "'uk_consumer_event', 'consumer_name,event_id'",
                "'idx_status_next_retry','status,next_retry_at,id'");
    }

    @Test
    void verificationMustObserveBacklogWithoutFailingOnIt() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "oldest_pending_age_minutes",
                "dead_letter",
                "waiting_backoff",
                "group by i.consumer_name");

        // 堆积量是运行指标，不是结构门禁：静态阈值会在业务峰值误判，必须交给监控告警
        int verdictIdx = sql.indexOf("case when cg.missing_columns = 0");
        assertTrue(verdictIdx > 0, "缺少 PASS/FAIL 判定表达式");
        String verdict = sql.substring(verdictIdx, Math.min(sql.length(), verdictIdx + 400));
        assertFalse(verdict.contains("pending_new"), "堆积量不得参与 PASS/FAIL 判定");
        assertFalse(verdict.contains("dead_letter"), "死信量应接入监控，不得作为结构门禁判定项");
    }

    @Test
    void verificationMustBeReadOnly() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertFalse(sql.contains("insert into event_outbox"), "校验脚本不得写入发件箱");
        assertFalse(sql.contains("insert into event_inbox"), "校验脚本不得写入收件箱");
        assertFalse(sql.contains("update event_outbox"), "校验脚本不得修改发件箱");
        assertFalse(sql.contains("update event_inbox"), "校验脚本不得修改收件箱");
        assertFalse(sql.contains("delete from event_outbox"), "校验脚本不得清理发件箱");
        assertFalse(sql.contains("alter table"), "校验脚本不得变更结构");
    }

    private static String readRequiredResource(String path) {
        try (InputStream input = EventOutboxInboxFlywayMigrationContractTest.class
                .getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "缺少 Flyway 资源: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new AssertionError("读取 Flyway 资源失败: " + path, ex);
        }
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static void assertContainsAll(String actual, String... expected) {
        List<String> missing = Arrays.stream(expected)
                .filter(value -> !actual.contains(value))
                .toList();
        assertTrue(missing.isEmpty(), () -> "迁移契约缺少: " + missing);
    }
}