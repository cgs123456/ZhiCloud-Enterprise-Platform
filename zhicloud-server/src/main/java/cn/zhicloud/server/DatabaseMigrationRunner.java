package cn.zhicloud.server;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库迁移独立入口（供 Kubernetes PreSync Job / CI 显式调用）。
 *
 * <p>本类支持两种严格分离的迁移模式：</p>
 * <ul>
 *   <li>{@code existing}（默认）：目标库必须已经由历史安装脚本初始化，Flyway 只建立 V1 基线并执行增量迁移；</li>
 *   <li>{@code clean}：仅允许空 MySQL schema。先校验并执行无公开账号/无演示数据的 V1.1 clean baseline，
 *       再让 Flyway 以 V1.1 为基线继续执行 V2..V117。</li>
 * </ul>
 *
 * <p>clean 模式必须通过环境变量 {@code MIGRATION_BASELINE_MODE=clean} 显式开启，绝不能成为默认行为。</p>
 *
 * @author zhicloud
 */
public final class DatabaseMigrationRunner {

    static final String BASELINE_MODE_ENV = "MIGRATION_BASELINE_MODE";
    static final String EXISTING_MODE = "existing";
    static final String CLEAN_MODE = "clean";

    static final String LOCATIONS = "classpath:db/migration";
    static final String HISTORY_TABLE = "flyway_schema_history";
    static final String CLEAN_BASELINE_RESOURCE = "db/clean-baseline/V1.1__system_base_schema.clean.sql";
    static final String CLEAN_MANIFEST_RESOURCE = "db/clean-baseline/manifest.json";
    static final String SOURCE_BASELINE_RESOURCE = "db/migration/V1.1__system_base_schema.sql";
    static final String EXISTING_BASELINE_VERSION = "1.1";
    static final String CLEAN_BASELINE_VERSION = "1.1";

    private static final Pattern TABLE_DEFINITION = Pattern.compile(
            "(?m)^CREATE TABLE IF NOT EXISTS `([^`]+)`");

    static final List<String> REQUIRED_EXISTING_TABLES = loadRequiredExistingTables();
    private static final Pattern JSON_HASH = Pattern.compile(
            "\"source_sha256\"\\s*:\\s*\"([0-9a-fA-F]{64})\"");
    private static final Pattern JSON_CLEAN_HASH = Pattern.compile(
            "\"clean_baseline_sha256\"\\s*:\\s*\"([0-9a-fA-F]{64})\"");
    private static final Pattern TARGET_SEED_INSERT = Pattern.compile(
            "\\bINSERT\\s+(?:IGNORE\\s+)?INTO\\s+`?(?:system_users|system_oauth2_client|"
                    + "system_social_client|system_user_role|system_user_post|zhicloud_demo\\w*)`?",
            Pattern.CASE_INSENSITIVE);

    private DatabaseMigrationRunner() {
    }

    public static void main(String[] args) {
        System.exit(run(System.getenv()));
    }

    /**
     * 运行迁移并返回进程退出码，便于测试时不触发 {@link System#exit(int)}。
     */
    static int run(Map<String, String> environment) {
        long start = System.currentTimeMillis();
        try {
            String url = requireEnv(environment, "MASTER_DATASOURCE_URL", "SPRING_DATASOURCE_URL");
            String username = requireEnv(environment, "MASTER_DATASOURCE_USERNAME", "SPRING_DATASOURCE_USERNAME");
            String password = requireEnv(environment, "MASTER_DATASOURCE_PASSWORD", "SPRING_DATASOURCE_PASSWORD");
            String mode = resolveBaselineMode(environment.get(BASELINE_MODE_ENV));

            if (CLEAN_MODE.equals(mode)) {
                requireMySqlUrl(url);
            }

            Map<String, String> connectionProperties = jdbcProperties(environment);
            System.out.printf("[flyway-migration] target=%s mode=%s%n", maskUrl(url), mode);

            if (EXISTING_MODE.equals(mode)) {
                assertExistingTargetInitialized(url, username, password, connectionProperties);
            } else {
                executeCleanBaseline(url, username, password, connectionProperties);
            }

            MigrateResult result = configureFlyway(url, username, password, mode, connectionProperties).migrate();
            System.out.printf(
                    "[flyway-migration] success: applied=%d, initialVersion=%s, targetVersion=%s, cost=%dms%n",
                    result.migrationsExecuted, result.initialSchemaVersion, result.targetSchemaVersion,
                    System.currentTimeMillis() - start);
            return 0;
        } catch (Throwable ex) {
            System.err.println("[flyway-migration] FAILED: " + ex.getMessage());
            ex.printStackTrace(System.err);
            return 1;
        }
    }

    private static Flyway configureFlyway(
            String url, String username, String password, String mode,
            Map<String, String> jdbcProperties) {
        String baselineVersion = CLEAN_MODE.equals(mode) ? CLEAN_BASELINE_VERSION : EXISTING_BASELINE_VERSION;
        return Flyway.configure(DatabaseMigrationRunner.class.getClassLoader())
                .dataSource(url, username, password)
                .jdbcProperties(jdbcProperties)
                .locations(LOCATIONS)
                .table(HISTORY_TABLE)
                .encoding("UTF-8")
                .baselineOnMigrate(true)
                .baselineVersion(baselineVersion)
                .validateOnMigrate(true)
                .outOfOrder(false)
                // 迁移失败时不清库，交由人工介入，避免误删生产数据
                .cleanDisabled(true)
                .load();
    }

    /**
     * 读取环境变量，支持多个候选名；全部缺失则直接失败（fail-fast，避免连到错误的库）。
     */
    /**
     * 将 Helm/K8s 注入的 MySQL TLS truststore 环境变量映射为 Connector/J 连接属性。
     *
     * <p>仅在值非空时写入，避免把空字符串误当成有效 truststore 配置；这样既能支持
     * 开发环境的非 TLS 连接，也能保证生产 VERIFY_IDENTITY 在启动前就使用同一套证书链。</p>
     */
    static Map<String, String> jdbcProperties(Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        Map<String, String> properties = new LinkedHashMap<>();
        copyIfPresent(environment, properties,
                "MYSQL_TLS_TRUSTSTORE_URL", "trustCertificateKeyStoreUrl");
        copyIfPresent(environment, properties,
                "MYSQL_TLS_TRUSTSTORE_TYPE", "trustCertificateKeyStoreType");
        copyIfPresent(environment, properties,
                "MYSQL_TLS_TRUSTSTORE_PASSWORD", "trustCertificateKeyStorePassword");
        return Map.copyOf(properties);
    }

    private static void copyIfPresent(
            Map<String, String> environment, Map<String, String> properties,
            String envName, String propertyName) {
        String value = environment.get(envName);
        if (value != null && !value.isBlank()) {
            properties.put(propertyName, value);
        }
    }

    private static Connection openConnection(
            String url, String username, String password, Map<String, String> jdbcProperties) throws SQLException {
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        jdbcProperties.forEach(properties::setProperty);
        return DriverManager.getConnection(url, properties);
    }

    static String requireEnv(Map<String, String> environment, String... names) {
        for (String name : names) {
            String value = environment.get(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw new IllegalStateException("缺少必需的环境变量: " + String.join(" 或 ", names));
    }

    static String resolveBaselineMode(String value) {
        if (value == null || value.isBlank()) {
            return EXISTING_MODE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (EXISTING_MODE.equals(normalized) || CLEAN_MODE.equals(normalized)) {
            return normalized;
        }
        throw new IllegalArgumentException("MIGRATION_BASELINE_MODE 只允许 existing 或 clean，实际=" + value);
    }

    static void requireMySqlUrl(String url) {
        if (url == null || !url.toLowerCase(Locale.ROOT).startsWith("jdbc:mysql:")) {
            throw new IllegalArgumentException("clean baseline 模式只支持 MySQL JDBC URL");
        }
    }

    /**
     * existing 模式只接受已经完整初始化且已记录成功 V1.1 的存量库。
     *
     * <p>不能仅凭“表非空”建立 V1.1 基线：Flyway 会把带表但缺少历史记录的 schema 直接标成
     * V1.1，导致目标库是否真的具备 V1.1 结构无法核实。该路径必须 fail-closed，由人工恢复或
     * 显式 clean 建库后重新发布。</p>
     */
    static void validateExistingTargetInitialization(
            Set<String> tableNames, boolean successfulBaselineRecorded) {
        Objects.requireNonNull(tableNames, "tableNames");
        if (tableNames.isEmpty()) {
            throw new IllegalStateException(
                    "existing 模式拒绝空库：请先执行历史安装脚本，或显式设置 MIGRATION_BASELINE_MODE=clean");
        }

        Set<String> missingTables = missingRequiredTables(tableNames);
        if (!missingTables.isEmpty()) {
            throw new IllegalStateException(
                    "existing 模式目标库缺少关键系统表: " + String.join(", ", missingTables)
                            + "；请确认连接的是历史安装脚本初始化的 schema，"
                            + "或显式设置 MIGRATION_BASELINE_MODE=clean 重建空库");
        }

        if (!successfulBaselineRecorded) {
            throw new IllegalStateException(
                    "existing 模式目标库缺少 flyway_schema_history 中成功的 V1.1 记录；"
                            + "拒绝自动建立 V1.1 baseline，请先完成受控历史库基线修复");
        }
    }

    /**
     * existing 模式拒绝空库，并拒绝任何未记录成功 V1.1 的库，
     * 避免误把公开种子账号通过 V1.1 写入一个全新数据库或错误信任 legacy schema。
     */
    private static void assertExistingTargetInitialized(
            String url, String username, String password, Map<String, String> jdbcProperties) throws SQLException {
        try (Connection connection = openConnection(url, username, password, jdbcProperties)) {
            Set<String> tableNames = readTableNames(connection);
            boolean successfulBaselineRecorded = hasSuccessfulMigration(connection, EXISTING_BASELINE_VERSION);
            validateExistingTargetInitialization(tableNames, successfulBaselineRecorded);
        }
    }

    private static boolean hasSuccessfulMigration(Connection connection, String version) throws SQLException {
        if (!readTableNames(connection).contains(HISTORY_TABLE)) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM " + HISTORY_TABLE + " WHERE success = 1 AND version = ?")) {
            statement.setString(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getLong(1) > 0;
            }
        }
    }

    private static Set<String> readTableNames(Connection connection) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        Set<String> tableNames = collectTableNames(metadata, catalog, schema);
        if (tableNames.isEmpty()) {
            tableNames = collectTableNames(metadata, catalog, null);
        }
        if (tableNames.isEmpty()) {
            tableNames = collectTableNames(metadata, null, null);
        }
        return tableNames;
    }

    private static Set<String> collectTableNames(DatabaseMetaData metadata, String catalog, String schema)
            throws SQLException {
        Set<String> tableNames = new HashSet<>();
        try (ResultSet resultSet = metadata.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
            while (resultSet.next()) {
                String tableName = resultSet.getString("TABLE_NAME");
                if (tableName != null && !tableName.isBlank()) {
                    tableNames.add(tableName.toLowerCase(Locale.ROOT));
                }
            }
        }
        return tableNames;
    }

    private static List<String> loadRequiredExistingTables() {
        try {
            String baseline = new String(
                    readRequiredResource(CLEAN_BASELINE_RESOURCE), StandardCharsets.UTF_8);
            Set<String> tableNames = new LinkedHashSet<>();
            Matcher matcher = TABLE_DEFINITION.matcher(baseline);
            while (matcher.find()) {
                tableNames.add(matcher.group(1).toLowerCase(Locale.ROOT));
            }
            if (tableNames.isEmpty()) {
                throw new IllegalStateException("clean baseline 未声明任何基础表");
            }
            return List.copyOf(tableNames);
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static Set<String> missingRequiredTables(Set<String> tableNames) {
        Set<String> normalized = new HashSet<>();
        for (String tableName : tableNames) {
            if (tableName != null && !tableName.isBlank()) {
                normalized.add(tableName.toLowerCase(Locale.ROOT));
            }
        }
        Set<String> missing = new LinkedHashSet<>(REQUIRED_EXISTING_TABLES);
        missing.removeAll(normalized);
        return missing;
    }

    private static void executeCleanBaseline(
            String url, String username, String password, Map<String, String> jdbcProperties) throws Exception {
        byte[] sourceBytes = readRequiredResource(SOURCE_BASELINE_RESOURCE);
        byte[] baselineBytes = readRequiredResource(CLEAN_BASELINE_RESOURCE);
        String sourceHash = sha256(sourceBytes);
        String baselineHash = sha256(baselineBytes);

        verifyCleanBaselineManifest(sourceHash, baselineHash);
        String script = new String(baselineBytes, StandardCharsets.UTF_8);
        if (TARGET_SEED_INSERT.matcher(script).find()) {
            throw new IllegalStateException("clean baseline 包含公开种子/演示数据 INSERT，拒绝执行");
        }
        List<String> statements = SqlScriptSplitter.split(script);
        if (statements.isEmpty()) {
            throw new IllegalStateException("clean baseline 没有可执行语句");
        }

        try (Connection connection = openConnection(url, username, password, jdbcProperties)) {
            long tableCount = countBaseTables(connection);
            boolean cleanBaselineRecorded = tableCount > 0 && hasSuccessfulCleanBaseline(connection);
            if (!shouldExecuteCleanBaseline(tableCount, cleanBaselineRecorded)) {
                System.out.printf(
                        "[flyway-migration] clean baseline already recorded: version=%s, tables=%d%n",
                        CLEAN_BASELINE_VERSION, tableCount);
                return;
            }

            connection.setAutoCommit(false);
            try {
                for (String statement : statements) {
                    String normalized = statement.strip();
                    String upper = normalized.toUpperCase(Locale.ROOT);
                    if ("BEGIN".equals(upper) || "START TRANSACTION".equals(upper)) {
                        continue;
                    }
                    if ("COMMIT".equals(upper)) {
                        connection.commit();
                        continue;
                    }
                    try (Statement jdbcStatement = connection.createStatement()) {
                        jdbcStatement.execute(normalized);
                    }
                }
                connection.commit();
            } catch (Throwable ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        }

        System.out.printf(
                "[flyway-migration] clean baseline: sourceSha256=%s, outputSha256=%s, statements=%d%n",
                sourceHash, baselineHash, statements.size());
    }

    /**
     * clean 模式必须同时满足“空库首次执行”或“已存在成功 V1.1 clean baseline 记录”。
     * 非空但没有 baseline 记录时直接失败，避免把任意历史库误当作 clean 安装目标。
     */
    static boolean shouldExecuteCleanBaseline(long tableCount, boolean cleanBaselineRecorded) {
        if (tableCount == 0) {
            return true;
        }
        if (cleanBaselineRecorded) {
            return false;
        }
        throw new IllegalStateException(
                "clean baseline 只允许空 schema，或已记录成功 V1.1 的 clean schema；实际已有 "
                        + tableCount + " 张表");
    }

    private static boolean hasSuccessfulCleanBaseline(Connection connection) throws SQLException {
        if (!readTableNames(connection).contains(HISTORY_TABLE)) {
            return false;
        }
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + HISTORY_TABLE
                             + " WHERE success = 1 AND version = '" + CLEAN_BASELINE_VERSION + "'")) {
            return resultSet.next() && resultSet.getLong(1) > 0;
        }
    }

    private static long countBaseTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.tables "
                             + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'")) {
            if (!resultSet.next()) {
                throw new SQLException("无法读取当前 schema 的表数量");
            }
            return resultSet.getLong(1);
        }
    }

    private static void verifyCleanBaselineManifest(String sourceHash, String baselineHash) throws IOException {
        String manifest = new String(readRequiredResource(CLEAN_MANIFEST_RESOURCE), StandardCharsets.UTF_8);
        String expectedSourceHash = extractHash(JSON_HASH, manifest, "source_sha256");
        String expectedBaselineHash = extractHash(JSON_CLEAN_HASH, manifest, "clean_baseline_sha256");
        if (!Objects.equals(sourceHash, expectedSourceHash)) {
            throw new IllegalStateException("clean baseline 源文件 SHA-256 与 manifest 不一致");
        }
        if (!Objects.equals(baselineHash, expectedBaselineHash)) {
            throw new IllegalStateException("clean baseline SHA-256 与 manifest 不一致");
        }
    }

    private static String extractHash(Pattern pattern, String text, String field) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("clean baseline manifest 缺少 " + field);
        }
        return matcher.group(1).toLowerCase(Locale.ROOT);
    }

    private static byte[] readRequiredResource(String resource) throws IOException {
        try (InputStream input = DatabaseMigrationRunner.class.getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IOException("classpath 资源不存在: " + resource);
            }
            return input.readAllBytes();
        }
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);
        }
    }

    /**
     * 打印数据源时抹掉 query string，避免把密码等敏感参数写进 Pod 日志。
     */
    static String maskUrl(String url) {
        int idx = url.indexOf('?');
        return idx > 0 ? url.substring(0, idx) : url;
    }
}
