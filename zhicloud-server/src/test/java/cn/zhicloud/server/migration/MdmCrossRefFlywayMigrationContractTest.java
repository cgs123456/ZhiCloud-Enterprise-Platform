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
 * 主数据跨域对照（mdm_cross_ref）Flyway 迁移契约测试（V101 结构 + V102 模拟数据）。
 *
 * <p>V101 解决的是「ERP 的 productId 与 WMS 的 skuId 属于两套主键空间」这个问题：
 * 没有显式对照表时，跨模块集成只能靠「两边编码恰好相同」的巧合，一旦任一侧重新导入，
 * 下游就会把单据建到错误的 SKU / 仓库上。唯一键 uk_cross_ref 是「一个源实体在同一目标系统内
 * 只有一个当前映射」的数据库级保证，一旦被削弱，应用层再怎么补也拿不回来，因此锁死。</p>
 *
 * <p>为什么 source_id / target_id 用 varchar(64) 而不是 bigint：当前 ERP/WMS 都是数值主键，
 * 但外部系统（电商、客户编码）的标识可能是字符串，数值列会把这条路堵死。</p>
 *
 * <p>为什么状态和系统/实体类型都是 varchar 而不是 enum：新增业务域（MES/QMS/TMS/CRM）只需插入新行，
 * 不需要改列发版；enum 加值要改列，违反「迁移只做加法」原则。</p>
 *
 * <p>为什么结构（V101）与模拟数据（V102）分开：结构要进生产，模拟数据不进生产。
 * 混在一个迁移里会让「生产环境是否该执行这个文件」无法回答。</p>
 */
class MdmCrossRefFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V101__mdm_cross_ref.sql";
    private static final String VERIFICATION = "db/verification/V101_verify.sql";
    private static final String SEED = "db/migration/V102__mdm_cross_ref_seed.sql";
    private static final String SEED_VERIFICATION = "db/verification/V102_verify.sql";

    @Test
    void migrationMustCreateCrossRefTableIdempotently() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "create table if not exists `mdm_cross_ref`",
                "engine = innodb",
                "default charset = utf8mb4",
                "unique key `uk_cross_ref` (`tenant_id`, `source_system`, `source_entity`, `source_id`, `target_system`, `target_entity`)",
                "key `idx_tenant_source` (`tenant_id`, `source_system`, `source_entity`, `source_id`)",
                "key `idx_target` (`target_system`, `target_entity`, `target_id`)",
                "`tenant_id` bigint not null default 0",
                "`source_system` varchar(32) not null",
                "`source_entity` varchar(32) not null",
                "`source_id` varchar(64) not null",
                "`target_system` varchar(32) not null",
                "`target_entity` varchar(32) not null",
                "`target_id` varchar(64) not null",
                "`status` varchar(16) not null default 'valid'");

        // BaseDO 兼容字段：逻辑删除与审计列缺失会让 Starter 的软删除/填充直接报错
        assertContainsAll(sql,
                "`deleted` bit not null default 0",
                "`create_time` datetime not null default current_timestamp",
                "`update_time` datetime not null default current_timestamp on update current_timestamp");
    }

    @Test
    void migrationMustNotWeakenMappingGuarantees() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertFalse(sql.contains("drop table"), "迁移不得删表");
        assertFalse(sql.contains("alter table"), "V101 只新增表，不得变更既有表结构");
        assertFalse(sql.contains("insert into"), "结构迁移不得写入数据，模拟数据由 V102 单独承载");
        assertFalse(sql.contains("foreign key"),
                "映射表不得加外键：主数据被归档/清理时会连带阻断映射维护");
        assertFalse(sql.contains("enum("),
                "状态与实体类型必须是 varchar：enum 加值需要改列，违反迁移只做加法原则");
    }

    @Test
    void seedMustBeIdempotentAndExplicitlyFake() {
        String sql = normalize(readRequiredResource(SEED));

        assertContainsAll(sql,
                "insert into `mdm_cross_ref`",
                "on duplicate key update",
                "8000000000000000001",
                "fake-模拟数据",
                "'erp'",
                "'wms'",
                "'product'",
                "'sku'",
                "'warehouse'",
                "'customer'",
                "'merchant'",
                "tenant_id=0");

        assertFalse(sql.contains("drop table"), "模拟数据迁移不得删表");
        assertFalse(sql.contains("alter table"), "模拟数据迁移不得变更结构");
        assertFalse(sql.contains("truncate"), "模拟数据迁移不得清表");
        assertTrue(sql.contains("禁止入生产"),
                "模拟数据必须显式声明禁止入生产，否则会被误当成可上线的数据");
    }

    @Test
    void verificationMustGateStructureAndIndexes() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "tmp_v101_verify_expected_column",
                "tmp_v101_verify_expected_index",
                "information_schema.columns",
                "information_schema.statistics",
                "column_verdict",
                "index_verdict",
                "v101_verify_verdict",
                "'pass'",
                "'fail'",
                "'missing'",
                "'wrong'");

        assertContainsAll(sql,
                "'uk_cross_ref', 'tenant_id,source_system,source_entity,source_id,target_system,target_entity'",
                "'idx_tenant_source','tenant_id,source_system,source_entity,source_id'",
                "'idx_target', 'target_system,target_entity,target_id'");
    }

    @Test
    void seedVerificationMustGateGroupCountsAndDuplicates() {
        String sql = normalize(readRequiredResource(SEED_VERIFICATION));

        assertContainsAll(sql,
                "tmp_v102_verify_expected_group",
                "'product', 'sku', 13",
                "'warehouse', 'warehouse', 3",
                "'customer', 'merchant', 2",
                "duplicate_verdict",
                "v102_verify_verdict",
                "'pass'",
                "'fail'");
    }

    @Test
    void verificationMustBeReadOnly() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertFalse(sql.contains("insert into mdm_cross_ref"), "校验脚本不得写入映射表");
        assertFalse(sql.contains("update mdm_cross_ref"), "校验脚本不得修改映射表");
        assertFalse(sql.contains("alter table"), "校验脚本不得变更结构");
    }

    private static String readRequiredResource(String path) {
        try (InputStream input = MdmCrossRefFlywayMigrationContractTest.class
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