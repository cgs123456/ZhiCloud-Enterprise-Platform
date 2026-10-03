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
 * V103 WMS 入库/出库类型列扩容迁移契约测试。
 *
 * <p>代码枚举使用 100/200 段，而 V11 的历史 DDL 把类型列定义为有符号 TINYINT；
 * 这个不一致会让 WMS 建单在 MySQL 严格模式下直接失败。本测试锁死前向迁移，
 * 防止后续有人把列改回 TINYINT 或误删两个域的扩容语句。</p>
 */
class WmsOrderTypeRangeFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V103__wms_order_type_range.sql";
    private static final String VERIFICATION = "db/verification/V103_verify.sql";

    @Test
    void migrationMustWidenBothWmsTypeColumnsToSmallint() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "alter table `wms_receipt_order`",
                "alter table `wms_shipment_order`",
                "modify column `type` smallint not null",
                "100",
                "101",
                "102",
                "103",
                "200",
                "201",
                "202");

        assertFalse(sql.contains("drop table"), "V103 不得删除表");
        assertFalse(sql.contains("delete from"), "V103 不得删除业务数据");
        assertFalse(sql.contains("truncate"), "V103 不得清表");
        assertFalse(sql.contains("modify column `type` tinyint"), "迁移不得继续把 WMS 类型列定义为 TINYINT");
    }

    @Test
    void verificationMustGateBothColumnsAndBeReadOnly() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "'wms_receipt_order'",
                "'wms_shipment_order'",
                "information_schema.columns",
                "'smallint'",
                "'no'",
                "'pass'",
                "'fail'",
                "correct_columns");

        assertFalse(sql.contains("alter table"), "校验脚本不得变更结构");
        assertFalse(sql.contains("delete from"), "校验脚本不得删除业务数据");
        assertFalse(sql.contains("update wms_"), "校验脚本不得修改 WMS 业务数据");
        assertFalse(sql.contains("insert into wms_"), "校验脚本不得写入 WMS 业务数据");
    }

    private static String readRequiredResource(String path) {
        try (InputStream input = WmsOrderTypeRangeFlywayMigrationContractTest.class
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