package cn.zhicloud.server;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class V77RepairScriptContractTest {

    private static final Path SCRIPT = findRepoRoot()
            .resolve("scripts/production/repair_v77_aimultiagent_resilience.sql");

    @Test
    void repairScriptKeepsIdempotentFailClosedContract() throws Exception {
        assertTrue(Files.isRegularFile(SCRIPT), "V77 repair script must exist: " + SCRIPT);
        String sql = Files.readString(SCRIPT);

        assertTrue(sql.contains("information_schema.columns"), "repair must inspect existing columns");
        assertTrue(sql.contains("SIGNAL SQLSTATE '45000'"), "repair must fail closed on inconsistent history");
        assertTrue(sql.contains("V77_REPAIR_APPLIED"), "repair must expose an applied marker");
        assertTrue(sql.contains("V77_REPAIR_ALREADY_CONSISTENT"), "repair must expose an already-consistent marker");
        assertTrue(sql.contains("V77_REPAIR_NOOP_NO_FLYWAY_HISTORY"), "repair must expose no-history marker");
        assertTrue(sql.contains("V77_REPAIR_NOOP_NO_V77_HISTORY"), "repair must expose no-V77 marker");
        assertTrue(sql.contains("checksum = 1776434009"), "repair must pin the published V77 checksum");
        assertTrue(sql.contains("DELIMITER $$"), "repair requires mysql client DELIMITER support");
        assertTrue(sql.contains("DELIMITER ;"), "repair must restore the mysql client delimiter");
    }

    @Test
    void repairScriptDoesNotDeleteBusinessDataOrTables() throws Exception {
        String sql = Files.readString(SCRIPT).toUpperCase();

        assertFalse(sql.contains("DROP TABLE"), "repair must not drop tables");
        assertFalse(sql.contains("TRUNCATE "), "repair must not truncate tables");
        assertFalse(sql.contains("DELETE FROM"), "repair must not delete business data");
    }

    private static Path findRepoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        if (current.getFileName() != null && "zhicloud-server".equals(current.getFileName().toString())) {
            return current.getParent();
        }
        return current;
    }
}