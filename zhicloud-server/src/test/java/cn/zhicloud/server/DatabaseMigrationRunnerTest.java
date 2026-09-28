package cn.zhicloud.server;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseMigrationRunnerTest {

    @Test
    void baselineModeDefaultsToExistingAndRejectsUnknownValues() {
        assertEquals(DatabaseMigrationRunner.EXISTING_MODE,
                DatabaseMigrationRunner.resolveBaselineMode(null));
        assertEquals(DatabaseMigrationRunner.EXISTING_MODE,
                DatabaseMigrationRunner.resolveBaselineMode("  "));
        assertEquals(DatabaseMigrationRunner.CLEAN_MODE,
                DatabaseMigrationRunner.resolveBaselineMode("CLEAN"));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseMigrationRunner.resolveBaselineMode("fresh"));
    }

    @Test
    void existingBaselineVersionMustBeOneDotOneToSkipSourceSeeds() {
        assertEquals("1.1", DatabaseMigrationRunner.EXISTING_BASELINE_VERSION);
    }

    @Test
    void existingTargetAcceptsCompleteSchemaWithSuccessfulV11History() throws Exception {
        assertDoesNotThrow(() -> DatabaseMigrationRunner.validateExistingTargetInitialization(
                sourceBaselineTables(), true));
    }

    @Test
    void existingTargetRejectsMissingV11HistoryEvenWhenAllTablesExist() throws Exception {
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> DatabaseMigrationRunner.validateExistingTargetInitialization(
                        sourceBaselineTables(), false));

        assertTrue(failure.getMessage().contains("V1.1"));
        assertTrue(failure.getMessage().contains("flyway_schema_history"));
    }

    @Test
    void existingTargetRejectsEmptyOrIncompleteSchema() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> DatabaseMigrationRunner.validateExistingTargetInitialization(Set.of(), false));

        Set<String> incompleteTables = sourceBaselineTables();
        incompleteTables.remove("system_users");
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> DatabaseMigrationRunner.validateExistingTargetInitialization(incompleteTables, true));

        assertTrue(failure.getMessage().contains("system_users"));
    }

    @Test
    void existingSchemaSignatureRequiresEverySourceBaselineTable() throws Exception {
        Set<String> sourceTables = sourceBaselineTables();
        assertFalse(sourceTables.isEmpty());
        sourceTables.remove("system_menu");

        assertEquals(Set.of("system_menu"), DatabaseMigrationRunner.missingRequiredTables(sourceTables));
    }

    @Test
    void missingRequiredTablesDetectsCoreSchemaGapsCaseInsensitively() throws Exception {
        Set<String> sourceTables = sourceBaselineTables();
        assertEquals(Set.of(), DatabaseMigrationRunner.missingRequiredTables(sourceTables));

        sourceTables.remove("system_users");
        assertEquals(Set.of("system_users"), DatabaseMigrationRunner.missingRequiredTables(sourceTables));

        sourceTables.add("system_users");
        sourceTables.remove("system_user_role");
        sourceTables.remove("system_user_post");
        assertEquals(Set.of("system_user_role", "system_user_post"),
                DatabaseMigrationRunner.missingRequiredTables(sourceTables));
    }

    @Test
    void cleanModeOnlyAcceptsMySqlJdbcUrls() {
        DatabaseMigrationRunner.requireMySqlUrl("jdbc:mysql://db:3306/zhicloud");
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseMigrationRunner.requireMySqlUrl("jdbc:postgresql://db/zhicloud"));
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseMigrationRunner.requireMySqlUrl(null));
    }

    @Test
    void cleanBaselineResumeRequiresSuccessfulV11HistoryOnNonEmptySchema() {
        assertTrue(DatabaseMigrationRunner.shouldExecuteCleanBaseline(0, false));
        assertFalse(DatabaseMigrationRunner.shouldExecuteCleanBaseline(463, true));
        assertThrows(IllegalStateException.class,
                () -> DatabaseMigrationRunner.shouldExecuteCleanBaseline(463, false));
    }

    @Test
    void splitterKeepsSemicolonsInsideStringsAndComments() {
        String sql = """
                -- comment; not a separator
                INSERT INTO `demo` (`value`) VALUES ('a;b');
                /* block; comment */
                INSERT INTO `demo` (`value`) VALUES ("c;d");
                """;

        List<String> statements = SqlScriptSplitter.split(sql);

        assertEquals(2, statements.size());
        assertTrue(statements.get(0).contains("'a;b'"));
        assertTrue(statements.get(1).contains("\"c;d\""));
    }

    @Test
    void splitterHandlesEscapedQuotesBackticksAndTransactionMarkers() {
        String sql = "BEGIN;"
                + "INSERT INTO `demo` (`value`) VALUES ('it\\'s;ok', 'it''s;also-ok');"
                + "COMMIT;";

        List<String> statements = SqlScriptSplitter.split(sql);

        assertEquals(3, statements.size());
        assertEquals("BEGIN", statements.get(0));
        assertTrue(statements.get(1).contains("it\\'s;ok"));
        assertTrue(statements.get(1).contains("it''s;also-ok"));
        assertEquals("COMMIT", statements.get(2));
    }

    @Test
    void splitterRejectsUnterminatedLiteralAndBlockComment() {
        assertThrows(IllegalArgumentException.class,
                () -> SqlScriptSplitter.split("INSERT INTO t VALUES ('unterminated);"));
        assertThrows(IllegalArgumentException.class,
                () -> SqlScriptSplitter.split("/* unterminated"));
    }

    @Test
    void packagedCleanBaselineMatchesManifestAndContainsNoTargetSeedInserts() throws Exception {
        byte[] source = readResource(DatabaseMigrationRunner.SOURCE_BASELINE_RESOURCE);
        byte[] clean = readResource(DatabaseMigrationRunner.CLEAN_BASELINE_RESOURCE);
        String manifest = new String(readResource(DatabaseMigrationRunner.CLEAN_MANIFEST_RESOURCE),
                StandardCharsets.UTF_8);

        assertTrue(manifest.contains(DatabaseMigrationRunner.sha256(source)));
        assertTrue(manifest.contains(DatabaseMigrationRunner.sha256(clean)));

        String script = new String(clean, StandardCharsets.UTF_8);
        assertFalse(script.matches("(?is).*\\bINSERT\\s+(?:IGNORE\\s+)?INTO\\s+`?(?:system_users|"
                + "system_oauth2_client|system_social_client|system_user_role|system_user_post|"
                + "zhicloud_demo\\w*)`?.*"));
        assertTrue(script.contains("CREATE TABLE IF NOT EXISTS `system_users`"));
        assertTrue(script.contains("CREATE TABLE IF NOT EXISTS `system_user_role`"));
        assertTrue(script.contains("CREATE TABLE IF NOT EXISTS `system_user_post`"));
    }

    @Test
    void jdbcPropertiesMapTruststoreEnvironmentToConnectorJProperties() {
        Map<String, String> properties = DatabaseMigrationRunner.jdbcProperties(Map.of(
                "MYSQL_TLS_TRUSTSTORE_URL", "file:/etc/zhicloud/mysql-tls/truststore.p12",
                "MYSQL_TLS_TRUSTSTORE_TYPE", "PKCS12",
                "MYSQL_TLS_TRUSTSTORE_PASSWORD", "not-a-real-secret"));

        assertEquals(Map.of(
                "trustCertificateKeyStoreUrl", "file:/etc/zhicloud/mysql-tls/truststore.p12",
                "trustCertificateKeyStoreType", "PKCS12",
                "trustCertificateKeyStorePassword", "not-a-real-secret"), properties);
    }

    @Test
    void jdbcPropertiesIgnoreBlankAndUnrelatedEnvironmentValues() {
        Map<String, String> properties = DatabaseMigrationRunner.jdbcProperties(Map.of(
                "MYSQL_TLS_TRUSTSTORE_URL", "  ",
                "MYSQL_TLS_TRUSTSTORE_TYPE", "",
                "MASTER_DATASOURCE_URL", "jdbc:mysql://db:3306/zhicloud"));

        assertTrue(properties.isEmpty());
    }

    private static Set<String> sourceBaselineTables() throws Exception {
        Set<String> sourceTables = new HashSet<>();
        Matcher matcher = Pattern.compile("(?m)^CREATE TABLE IF NOT EXISTS `([^`]+)`")
                .matcher(new String(readResource(DatabaseMigrationRunner.CLEAN_BASELINE_RESOURCE),
                        StandardCharsets.UTF_8));
        while (matcher.find()) {
            sourceTables.add(matcher.group(1).toLowerCase());
        }
        return sourceTables;
    }

    private static byte[] readResource(String resource) throws Exception {
        try (InputStream input = DatabaseMigrationRunnerTest.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("missing test resource: " + resource);
            }
            return input.readAllBytes();
        }
    }
}
