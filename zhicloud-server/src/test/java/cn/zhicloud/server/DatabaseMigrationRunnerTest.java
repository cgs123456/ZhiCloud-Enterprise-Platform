package cn.zhicloud.server;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @Test
    void existingTargetAcceptsCompleteCoreSchemaSignature() throws Exception {
        Map<String, Set<String>> columns = new HashMap<>(DatabaseMigrationRunner.REQUIRED_EXISTING_COLUMNS);
        Map<String, Set<String>> indexes = new HashMap<>(DatabaseMigrationRunner.REQUIRED_EXISTING_INDEXES);

        assertDoesNotThrow(() -> DatabaseMigrationRunner.validateExistingTargetInitialization(
                sourceBaselineTables(), true, columns, indexes));
    }

    @Test
    void existingTargetRejectsCompleteTableListWhenRequiredCoreColumnIsMissing() throws Exception {
        Map<String, Set<String>> columns = new HashMap<>(DatabaseMigrationRunner.REQUIRED_EXISTING_COLUMNS);
        Set<String> userColumns = new HashSet<>(columns.get("system_users"));
        userColumns.remove("tenant_id");
        columns.put("system_users", userColumns);
        Map<String, Set<String>> indexes = new HashMap<>(DatabaseMigrationRunner.REQUIRED_EXISTING_INDEXES);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> DatabaseMigrationRunner.validateExistingTargetInitialization(
                        sourceBaselineTables(), true, columns, indexes));

        assertTrue(failure.getMessage().contains("system_users.tenant_id"));
    }

    @Test
    void existingTargetRejectsCompleteTableListWhenRequiredCoreIndexIsMissing() throws Exception {
        Map<String, Set<String>> columns = new HashMap<>(DatabaseMigrationRunner.REQUIRED_EXISTING_COLUMNS);
        Map<String, Set<String>> indexes = new HashMap<>(DatabaseMigrationRunner.REQUIRED_EXISTING_INDEXES);
        Set<String> userIndexes = new HashSet<>(indexes.get("system_users"));
        userIndexes.remove("PRIMARY");
        indexes.put("system_users", userIndexes);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> DatabaseMigrationRunner.validateExistingTargetInitialization(
                        sourceBaselineTables(), true, columns, indexes));

        assertTrue(failure.getMessage().contains("system_users.PRIMARY"));
    }

    @Test
    void postMigrationSemanticFailureMakesRunnerFailClosed() {
        int exitCode = DatabaseMigrationRunner.run(
                migrationEnvironment(),
                (url, username, password, mode, properties) -> { },
                (url, username, password, mode, properties) ->
                        new DatabaseMigrationRunner.MigrationOutcome(1, "1.1", "117"),
                (url, username, password, properties) -> {
                    throw new IllegalStateException("V117 semantic verification failed");
                });

        assertEquals(1, exitCode);
    }

    @Test
    void postMigrationSemanticSuccessCompletesRunner() {
        AtomicBoolean verified = new AtomicBoolean(false);

        int exitCode = DatabaseMigrationRunner.run(
                migrationEnvironment(),
                (url, username, password, mode, properties) -> { },
                (url, username, password, mode, properties) ->
                        new DatabaseMigrationRunner.MigrationOutcome(1, "1.1", "117"),
                (url, username, password, properties) -> verified.set(true));

        assertEquals(0, exitCode);
        assertTrue(verified.get());
    }

    @Test
    void postMigrationVerificationCatalogMatchesSqlScripts() throws Exception {
        for (DatabaseMigrationRunner.VerificationSpec spec : DatabaseMigrationRunner.POST_MIGRATION_VERIFICATIONS) {
            Set<String> declaredChecks = new HashSet<>();
            Matcher matcher = Pattern.compile("SELECT '([A-Z0-9_]+)' AS check_name")
                    .matcher(new String(readResource(spec.resource()), StandardCharsets.UTF_8));
            while (matcher.find()) {
                declaredChecks.add(matcher.group(1));
            }

            assertEquals(spec.expectedChecks(), declaredChecks, spec.resource());
        }
    }

    @Test
    void postMigrationVerificationRejectsFailedOrMissingVerdicts() {
        DatabaseMigrationRunner.VerificationSpec spec = DatabaseMigrationRunner.POST_MIGRATION_VERIFICATIONS.get(0);
        Map<String, String> verdicts = new LinkedHashMap<>();
        spec.expectedChecks().forEach(check -> verdicts.put(check, "PASS"));
        String failedCheck = spec.expectedChecks().iterator().next();
        verdicts.put(failedCheck, "FAIL");

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> DatabaseMigrationRunner.validateVerificationVerdicts(spec, verdicts));
        assertTrue(failure.getMessage().contains(failedCheck));
        assertTrue(failure.getMessage().contains("FAIL"));

        verdicts.put(failedCheck, "PASS");
        verdicts.remove(failedCheck);
        IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> DatabaseMigrationRunner.validateVerificationVerdicts(spec, verdicts));
        assertTrue(missing.getMessage().contains(failedCheck));
        assertTrue(missing.getMessage().contains("缺失"));
    }

    @Test
    void postMigrationVerificationFailsClosedForNonMySqlTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> DatabaseMigrationRunner.verifyPostMigrationSemantics(
                        "jdbc:h2:mem:verification", "sa", "", Map.of()));
    }
    private static Map<String, String> migrationEnvironment() {
        return Map.of(
                "MASTER_DATASOURCE_URL", "jdbc:mysql://db:3306/zhicloud",
                "MASTER_DATASOURCE_USERNAME", "migration-user",
                "MASTER_DATASOURCE_PASSWORD", "not-a-real-secret");
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
