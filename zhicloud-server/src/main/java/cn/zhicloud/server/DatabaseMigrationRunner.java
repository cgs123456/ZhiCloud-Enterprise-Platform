package cn.zhicloud.server;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
import java.util.zip.CRC32;

/**
 * 数据库迁移独立入口（供 Kubernetes PreSync Job / CI 显式调用）。
 *
 * <p>本类支持两种严格分离的迁移模式：</p>
 * <ul>
 *   <li>{@code existing}（默认）：目标库必须已经由历史安装脚本初始化，Flyway 只建立 V1 基线并执行增量迁移；</li>
 *   <li>{@code clean}：仅允许空 MySQL schema。先校验并执行无公开账号/无演示数据的 V1.1 clean baseline，
 *       再让 Flyway 以 V1.1 为基线继续执行 V2..V121。</li>
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
    static final String V116_VERIFICATION_RESOURCE = "db/verification/V116_verify.sql";
    static final String V117_VERIFICATION_RESOURCE = "db/verification/V117_verify.sql";
    static final String V118_VERIFICATION_RESOURCE = "db/verification/V118_verify.sql";
    static final String V119_VERIFICATION_RESOURCE = "db/verification/V119_verify.sql";
    static final String V120_VERIFICATION_RESOURCE = "db/verification/V120_verify.sql";
    static final String V121_VERIFICATION_RESOURCE = "db/verification/V121_verify.sql";

    /**
     * 已发布 V1.1 的 Flyway checksum。该值来自 Flyway 11.7.2 的 CRC32 行拼接算法，
     * 并以有符号 int 保存；只允许从这一个精确值做一次性受控对齐。
     */
    static final int LEGACY_V11_CHECKSUM = -1213470718;
    static final int CURRENT_V11_CHECKSUM = loadCurrentV11Checksum();

    /**
     * Flyway 只能证明脚本“执行成功”，不能证明关键结构约束与数据不变量已经满足。
     * 这里把 V116-V121 的只读语义检查登记为迁移退出码的硬门禁；任何一项缺失、
     * 失败或额外漂移都会让 PreSync Job 失败，阻止业务 Deployment 继续发布。
     */
    static final List<VerificationSpec> POST_MIGRATION_VERIFICATIONS = List.of(
            new VerificationSpec(V116_VERIFICATION_RESOURCE, Set.of(
                    "V116_ACTIVE_USERNAME_COLUMN",
                    "V116_ACTIVE_USERNAME_UNIQUE_INDEX",
                    "V116_ACTIVE_USERNAME_NO_DUPLICATES")),
            new VerificationSpec(V117_VERIFICATION_RESOURCE, Set.of(
                    "V117_CRM_RECEIVABLE_IDEMPOTENCY_COLUMNS",
                    "V117_CRM_RECEIVABLE_IDEMPOTENCY_INDEX",
                    "V117_CRM_RECEIVABLE_IDEMPOTENCY_DATA")),
            new VerificationSpec(V118_VERIFICATION_RESOURCE, Set.of(
                    "V118_PROCESS_INSTANCE_IDEMPOTENCY_TABLE",
                    "V118_PROCESS_INSTANCE_IDEMPOTENCY_INDEX")),
            new VerificationSpec(V119_VERIFICATION_RESOURCE, Set.of(
                    "V119_INVENTORY_ITEM_TENANT_COLUMNS",
                    "V119_INVENTORY_ITEM_TENANT_UNIQUE_INDEX",
                    "V119_INVENTORY_ITEM_LEGACY_INDEX_REMOVED",
                    "V119_INVENTORY_ITEM_TENANT_UNIQUE_DATA")),
            new VerificationSpec(V120_VERIFICATION_RESOURCE, Set.of(
                    "V120_EVENT_INBOX_OWNER_COLUMNS",
                    "V120_EVENT_OUTBOX_OWNER_COLUMNS",
                    "V120_EVENT_INBOX_LEASE_INDEX",
                    "V120_EVENT_OUTBOX_LEASE_INDEX")),
            new VerificationSpec(V121_VERIFICATION_RESOURCE, Set.of(
                    "V121_BPM_IDEMPOTENCY_CLAIM_TOKEN_COLUMN")));

    record VerificationSpec(String resource, Set<String> expectedChecks) {
    }

    record MigrationOutcome(int appliedMigrations, String initialVersion, String targetVersion) {
    }

    enum V11ChecksumDisposition {
        CLEAN_BASELINE,
        CURRENT,
        LEGACY_REPAIR_REQUIRED
    }

    /**
     * flyway_schema_history 的完整快照行。checksum 是唯一允许在受控 V1.1 修复中变化的字段。
     */
    record HistoryRow(
            int installedRank,
            String version,
            String description,
            String type,
            String script,
            Integer checksum,
            String installedBy,
            String installedOn,
            long executionTime,
            boolean success) {

        boolean sameExceptChecksum(HistoryRow other) {
            return other != null
                    && installedRank == other.installedRank
                    && Objects.equals(version, other.version)
                    && Objects.equals(description, other.description)
                    && Objects.equals(type, other.type)
                    && Objects.equals(script, other.script)
                    && Objects.equals(installedBy, other.installedBy)
                    && Objects.equals(installedOn, other.installedOn)
                    && executionTime == other.executionTime
                    && success == other.success;
        }
    }

    @FunctionalInterface
    interface MigrationPreflight {
        void check(String url, String username, String password, String mode,
                   Map<String, String> jdbcProperties) throws Exception;
    }

    @FunctionalInterface
    interface MigrationExecutor {
        MigrationOutcome execute(String url, String username, String password, String mode,
                                 Map<String, String> jdbcProperties) throws Exception;
    }

    @FunctionalInterface
    interface PostMigrationVerifier {
        void verify(String url, String username, String password,
                    Map<String, String> jdbcProperties) throws Exception;
    }

    private static final Pattern TABLE_DEFINITION = Pattern.compile(
            "(?m)^CREATE TABLE IF NOT EXISTS `([^`]+)`");

    static final List<String> REQUIRED_EXISTING_TABLES = loadRequiredExistingTables();

    /**
     * existing 存量库的最小结构签名。
     *
     * <p>这里刻意只覆盖迁移、生产种子清理和后续关键约束所依赖的字段，不复制 V1.1
     * 的全部字段定义。历史库允许存在向后兼容的附加列，但缺少必需列时必须在任何
     * DDL 前失败，避免把结构错误的历史库错误标记成 V1.1。</p>
     */
    static final Map<String, Set<String>> REQUIRED_EXISTING_COLUMNS = Map.ofEntries(
            Map.entry("system_users", Set.of("id", "tenant_id", "username", "password", "status", "deleted")),
            Map.entry("system_role", Set.of("id", "tenant_id", "code", "status", "deleted")),
            Map.entry("system_user_role", Set.of("id", "tenant_id", "user_id", "role_id", "deleted")),
            Map.entry("system_user_post", Set.of("id", "tenant_id", "user_id", "post_id", "deleted")),
            Map.entry("system_oauth2_access_token",
                    Set.of("id", "tenant_id", "user_id", "deleted", "expires_time", "update_time")),
            Map.entry("system_oauth2_refresh_token",
                    Set.of("id", "tenant_id", "user_id", "deleted", "expires_time", "update_time")),
            Map.entry("system_oauth2_approve",
                    Set.of("id", "tenant_id", "user_id", "deleted", "expires_time", "update_time")),
            Map.entry("system_oauth2_code",
                    Set.of("id", "tenant_id", "user_id", "deleted", "expires_time", "update_time")),
            Map.entry("system_social_user_bind",
                    Set.of("id", "tenant_id", "user_id", "deleted", "update_time")),
            Map.entry("system_oauth2_client", Set.of("id", "client_id", "status", "update_time")),
            Map.entry("system_social_client", Set.of("id", "client_id", "status", "update_time")),
            Map.entry("crm_receivable", Set.of("id", "tenant_id")));

    /**
     * 最小索引签名只要求主键存在，避免把不同团队对非关键二级索引的命名差异误判为
     * 不可迁移。V116/V117 新增的唯一索引由迁移后语义校验单独验证。
     */
    static final Map<String, Set<String>> REQUIRED_EXISTING_INDEXES = Map.ofEntries(
            Map.entry("system_users", Set.of("PRIMARY")),
            Map.entry("system_role", Set.of("PRIMARY")),
            Map.entry("system_user_role", Set.of("PRIMARY")),
            Map.entry("system_user_post", Set.of("PRIMARY")),
            Map.entry("system_oauth2_access_token", Set.of("PRIMARY")),
            Map.entry("system_oauth2_refresh_token", Set.of("PRIMARY")),
            Map.entry("system_oauth2_approve", Set.of("PRIMARY")),
            Map.entry("system_oauth2_code", Set.of("PRIMARY")),
            Map.entry("system_social_user_bind", Set.of("PRIMARY")),
            Map.entry("system_oauth2_client", Set.of("PRIMARY")),
            Map.entry("system_social_client", Set.of("PRIMARY")),
            Map.entry("crm_receivable", Set.of("PRIMARY")));
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
        return run(environment,
                DatabaseMigrationRunner::preflightMigrationTarget,
                DatabaseMigrationRunner::executeFlywayMigration,
                DatabaseMigrationRunner::verifyPostMigrationSemantics);
    }

    /**
     * 运行迁移并返回进程退出码。预检、Flyway 执行、迁移后语义校验三段都通过后才返回 0；
     * 最后一段失败时会进入统一 catch，确保“Flyway success”不能绕过 V116/V117 门禁。
     */
    static int run(
            Map<String, String> environment,
            MigrationPreflight preflight,
            MigrationExecutor executor,
            PostMigrationVerifier verifier) {
        long start = System.currentTimeMillis();
        try {
            String url = requireEnv(environment, "MASTER_DATASOURCE_URL", "SPRING_DATASOURCE_URL");
            String username = requireEnv(environment, "MASTER_DATASOURCE_USERNAME", "SPRING_DATASOURCE_USERNAME");
            String password = requireEnv(environment, "MASTER_DATASOURCE_PASSWORD", "SPRING_DATASOURCE_PASSWORD");
            String mode = resolveBaselineMode(environment.get(BASELINE_MODE_ENV));

            Map<String, String> connectionProperties = jdbcProperties(environment);
            System.out.printf("[flyway-migration] target=%s mode=%s%n", maskUrl(url), mode);

            preflight.check(url, username, password, mode, connectionProperties);
            MigrationOutcome result = executor.execute(url, username, password, mode, connectionProperties);
            verifier.verify(url, username, password, connectionProperties);

            System.out.printf(
                    "[flyway-migration] success: applied=%d, initialVersion=%s, targetVersion=%s, cost=%dms%n",
                    result.appliedMigrations(), result.initialVersion(), result.targetVersion(),
                    System.currentTimeMillis() - start);
            return 0;
        } catch (Throwable ex) {
            System.err.println("[flyway-migration] FAILED: " + ex.getMessage());
            ex.printStackTrace(System.err);
            return 1;
        }
    }

    private static void preflightMigrationTarget(
            String url, String username, String password, String mode,
            Map<String, String> jdbcProperties) throws Exception {
        if (CLEAN_MODE.equals(mode)) {
            requireMySqlUrl(url);
            executeCleanBaseline(url, username, password, jdbcProperties);
            return;
        }
        assertExistingTargetInitialized(url, username, password, jdbcProperties);
    }

    private static MigrationOutcome executeFlywayMigration(
            String url, String username, String password, String mode,
            Map<String, String> jdbcProperties) {
        MigrateResult result = configureFlyway(url, username, password, mode, jdbcProperties).migrate();
        return new MigrationOutcome(
                result.migrationsExecuted, result.initialSchemaVersion, result.targetSchemaVersion);
    }

    static void verifyPostMigrationSemantics(
            String url, String username, String password,
            Map<String, String> jdbcProperties) throws Exception {
        requireMySqlUrl(url);
        try (Connection connection = openConnection(url, username, password, jdbcProperties)) {
            for (VerificationSpec spec : POST_MIGRATION_VERIFICATIONS) {
                Map<String, String> verdicts = executeVerificationScript(connection, spec.resource());
                validateVerificationVerdicts(spec, verdicts);
                System.out.printf(
                        "[flyway-migration] verification PASS: resource=%s, checks=%d%n",
                        spec.resource(), verdicts.size());
            }
        }
    }

    private static Map<String, String> executeVerificationScript(
            Connection connection, String resource) throws IOException, SQLException {
        String script = new String(readRequiredResource(resource), StandardCharsets.UTF_8);
        List<String> statements = SqlScriptSplitter.split(script);
        if (statements.isEmpty()) {
            throw new IllegalStateException("验证脚本没有可执行语句: " + resource);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }

        Map<String, String> verdicts = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement()) {
            for (String rawStatement : statements) {
                String sql = rawStatement.strip();
                if (sql.isEmpty()) {
                    continue;
                }
                statement.execute(sql);
                try (ResultSet resultSet = statement.getResultSet()) {
                    if (resultSet != null) {
                        collectVerificationVerdicts(resultSet, verdicts);
                    }
                }
            }
        }
        return Map.copyOf(verdicts);
    }

    private static void collectVerificationVerdicts(
            ResultSet resultSet, Map<String, String> verdicts) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        int checkNameIndex = findColumn(metadata, "check_name");
        int verdictIndex = findColumn(metadata, "verdict");
        if (checkNameIndex < 0 || verdictIndex < 0) {
            return;
        }

        while (resultSet.next()) {
            String checkName = resultSet.getString(checkNameIndex);
            String verdict = resultSet.getString(verdictIndex);
            if (checkName == null || checkName.isBlank()) {
                throw new IllegalStateException("验证脚本返回了空检查项名称");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
            String normalizedName = checkName.trim();
            String normalizedVerdict = verdict == null
                    ? "" : verdict.trim().toUpperCase(Locale.ROOT);
            String previous = verdicts.putIfAbsent(normalizedName, normalizedVerdict);
            if (previous != null) {
                throw new IllegalStateException("验证脚本返回了重复检查项: " + normalizedName);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
        }
    }

    private static int findColumn(ResultSetMetaData metadata, String expectedName) throws SQLException {
        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            if (expectedName.equalsIgnoreCase(metadata.getColumnLabel(index))) {
                return index;
            }
        }
        return -1;
    }

    static void validateVerificationVerdicts(
            VerificationSpec spec, Map<String, String> verdicts) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(verdicts, "verdicts");

        Set<String> missing = new LinkedHashSet<>(spec.expectedChecks());
        missing.removeAll(verdicts.keySet());
        Set<String> unexpected = new LinkedHashSet<>(verdicts.keySet());
        unexpected.removeAll(spec.expectedChecks());
        Map<String, String> failed = new LinkedHashMap<>();
        for (String checkName : spec.expectedChecks()) {
            String verdict = verdicts.get(checkName);
            if (verdict != null && !"PASS".equals(verdict)) {
                failed.put(checkName, verdict);
            }
        }

        if (missing.isEmpty() && unexpected.isEmpty() && failed.isEmpty()) {
            return;
        }
        StringBuilder message = new StringBuilder("迁移后语义校验未通过: resource=").append(spec.resource());
        if (!missing.isEmpty()) {
            message.append("，缺失=").append(String.join(",", missing));
        }
        if (!unexpected.isEmpty()) {
            message.append("，未预期=").append(String.join(",", unexpected));
        }
        if (!failed.isEmpty()) {
            message.append("，失败=").append(failed);
        }
        throw new IllegalStateException(message.toString());  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
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
        throw new IllegalStateException("缺少必需的环境变量: " + String.join(" 或 ", names));  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
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
            throw new IllegalArgumentException("数据库迁移入口只支持 MySQL JDBC URL");
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
        validateExistingTargetInitialization(
                tableNames, successfulBaselineRecorded, REQUIRED_EXISTING_COLUMNS, REQUIRED_EXISTING_INDEXES);
    }

    static void validateExistingTargetInitialization(
            Set<String> tableNames,
            boolean successfulBaselineRecorded,
            Map<String, Set<String>> existingColumns,
            Map<String, Set<String>> existingIndexes) {
        Objects.requireNonNull(tableNames, "tableNames");
        Objects.requireNonNull(existingColumns, "existingColumns");
        Objects.requireNonNull(existingIndexes, "existingIndexes");
        if (tableNames.isEmpty()) {
            throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                    "existing 模式拒绝空库：请先执行历史安装脚本，或显式设置 MIGRATION_BASELINE_MODE=clean");
        }

        Set<String> missingTables = missingRequiredTables(tableNames);
        if (!missingTables.isEmpty()) {
            throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                    "existing 模式目标库缺少关键系统表: " + String.join(", ", missingTables)
                            + "；请确认连接的是历史安装脚本初始化的 schema，"
                            + "或显式设置 MIGRATION_BASELINE_MODE=clean 重建空库");
        }

        if (!successfulBaselineRecorded) {
            throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                    "existing 模式目标库缺少 flyway_schema_history 中成功的 V1.1 记录；"
                            + "拒绝自动建立 V1.1 baseline，请先完成受控历史库基线修复");
        }

        Set<String> missingStructure = new LinkedHashSet<>();
        collectMissingStructure(existingColumns, REQUIRED_EXISTING_COLUMNS, missingStructure);
        collectMissingStructure(existingIndexes, REQUIRED_EXISTING_INDEXES, missingStructure);
        if (!missingStructure.isEmpty()) {
            throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                    "existing 模式目标库缺少关键结构: " + String.join(", ", missingStructure)
                            + "；拒绝在结构不完整的历史库上继续迁移");
        }
    }

    private static void collectMissingStructure(
            Map<String, Set<String>> existing,
            Map<String, Set<String>> required,
            Set<String> missingStructure) {
        for (Map.Entry<String, Set<String>> entry : required.entrySet()) {
            Set<String> normalizedExisting = normalizeNames(existing.get(entry.getKey()));
            for (String requiredName : entry.getValue()) {
                if (!normalizedExisting.contains(requiredName.toLowerCase(Locale.ROOT))) {
                    missingStructure.add(entry.getKey() + "." + requiredName);
                }
            }
        }
    }

    private static Set<String> normalizeNames(Set<String> names) {
        Set<String> normalized = new HashSet<>();
        if (names == null) {
            return normalized;
        }
        for (String name : names) {
            if (name != null && !name.isBlank()) {
                normalized.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
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
            Map<String, Set<String>> existingColumns = readRequiredColumns(connection);
            Map<String, Set<String>> existingIndexes = readRequiredIndexes(connection);
            validateExistingTargetInitialization(
                    tableNames, successfulBaselineRecorded, existingColumns, existingIndexes);
            alignExistingV11Checksum(connection);
        }
    }

    /**
     * 唯一允许的 Flyway checksum 对齐：仅当现存库记录的是已发布 V1.1 的精确 legacy checksum 时，
     * 将 V1.1 这一行的 checksum 更新到当前源码值。不得调用全局 Flyway.repair()，
     * 也不得忽略其他迁移的 checksum mismatch。
     */
    private static void alignExistingV11Checksum(Connection connection) throws SQLException {
        Map<Integer, HistoryRow> before = readHistoryRows(connection, false);
        HistoryRow v11 = requireV11HistoryRow(before);
        V11ChecksumDisposition v11Disposition = classifyV11HistoryRow(v11);
        if (v11Disposition == V11ChecksumDisposition.CLEAN_BASELINE
                || v11Disposition == V11ChecksumDisposition.CURRENT) {
            return;
        }
        requireV1HistoryRowForLegacyChecksumRepair(before, v11);

        connection.setAutoCommit(false);
        try {
            Map<Integer, HistoryRow> locked = readHistoryRows(connection, true);
            if (!locked.equals(before)) {
                throw new IllegalStateException("V1.1 checksum 对齐前 flyway_schema_history 发生变化，拒绝继续");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
            HistoryRow lockedV11 = requireV11HistoryRow(locked);
            if (classifyV11HistoryRow(lockedV11) != V11ChecksumDisposition.LEGACY_REPAIR_REQUIRED) {
                throw new IllegalStateException("V1.1 checksum 对齐状态发生变化，拒绝继续");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }

            int updated;
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE " + HISTORY_TABLE + " SET checksum = ? "
                            + "WHERE installed_rank = ? AND version = ? AND script = ? "
                            + "AND success = 1 AND checksum = ?")) {
                statement.setInt(1, CURRENT_V11_CHECKSUM);
                statement.setInt(2, lockedV11.installedRank());
                statement.setString(3, EXISTING_BASELINE_VERSION);
                statement.setString(4, "V1.1__system_base_schema.sql");
                statement.setInt(5, LEGACY_V11_CHECKSUM);
                updated = statement.executeUpdate();
            }
            if (updated != 1) {
                throw new IllegalStateException("V1.1 checksum 对齐必须恰好更新一行，实际=" + updated);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }

            Map<Integer, HistoryRow> after = readHistoryRows(connection, false);
            validateV11ChecksumRepairScope(locked, after, lockedV11.installedRank(), CURRENT_V11_CHECKSUM);
            connection.commit();
            System.out.printf(
                    "[flyway-migration] V1.1 checksum aligned: legacy=%d current=%d rank=%d%n",
                    LEGACY_V11_CHECKSUM, CURRENT_V11_CHECKSUM, lockedV11.installedRank());
        } catch (Throwable ex) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                ex.addSuppressed(rollbackEx);
            }
            if (ex instanceof SQLException sqlEx) {
                throw sqlEx;
            }
            if (ex instanceof RuntimeException runtimeEx) {
                throw runtimeEx;
            }
            throw new IllegalStateException("V1.1 checksum 对齐失败", ex);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * 旧库 V1.1 checksum 修复前必须确认 V1 基线真实存在；否则 Flyway 会把
     * V1.1 从 BASELINE 改判为 SQL 后判定 version 1 缺失，导致修复后迁移失败。
     */
    static void requireV1HistoryRowForLegacyChecksumRepair(
            Map<Integer, HistoryRow> history, HistoryRow v11) {
        List<HistoryRow> rows = history.values().stream()
                .filter(row -> "1".equals(row.version()))
                .toList();
        if (rows.size() != 1) {
            throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                    "V1.1 legacy checksum 修复前必须存在恰好一条成功的 V1 基线记录，实际=" + rows.size());
        }
        HistoryRow row = rows.get(0);
        if (!row.success()) {
            throw new IllegalStateException("flyway_schema_history 中 V1 基线记录不是成功迁移");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        if (row.installedRank() >= v11.installedRank()) {
            throw new IllegalStateException("flyway_schema_history 中 V1 基线记录必须早于 V1.1");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        if ("BASELINE".equals(row.type())) {
            if (!"<< Flyway Baseline >>".equals(row.script())) {
                throw new IllegalStateException("flyway_schema_history 中 V1 BASELINE 记录的脚本不符合 Flyway 基线行");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
            return;
        }
        if (!"SQL".equals(row.type()) || !"V1__baseline.sql".equals(row.script())) {
            throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                    "flyway_schema_history 中 V1 基线记录既不是 Flyway BASELINE，也不是 V1__baseline.sql");
        }
    }

    private static HistoryRow requireV11HistoryRow(Map<Integer, HistoryRow> history) {
        List<HistoryRow> rows = history.values().stream()
                .filter(row -> EXISTING_BASELINE_VERSION.equals(row.version()))
                .toList();
        if (rows.size() != 1) {
            throw new IllegalStateException("flyway_schema_history 中 V1.1 记录必须恰好一行，实际=" + rows.size());  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        HistoryRow row = rows.get(0);
        classifyV11HistoryRow(row);
        return row;
    }

    private static Map<Integer, HistoryRow> readHistoryRows(Connection connection, boolean forUpdate)
            throws SQLException {
        String sql = "SELECT installed_rank, version, description, type, script, checksum, "
                + "installed_by, installed_on, execution_time, success FROM " + HISTORY_TABLE
                + " ORDER BY installed_rank" + (forUpdate ? " FOR UPDATE" : "");
        Map<Integer, HistoryRow> rows = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                int installedRank = resultSet.getInt("installed_rank");
                HistoryRow row = new HistoryRow(
                        installedRank,
                        resultSet.getString("version"),
                        resultSet.getString("description"),
                        resultSet.getString("type"),
                        resultSet.getString("script"),
                        readNullableInt(resultSet, "checksum"),
                        resultSet.getString("installed_by"),
                        resultSet.getString("installed_on"),
                        resultSet.getLong("execution_time"),
                        resultSet.getBoolean("success"));
                if (rows.put(installedRank, row) != null) {
                    throw new IllegalStateException("flyway_schema_history 存在重复 installed_rank=" + installedRank);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                }
            }
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("flyway_schema_history 为空，无法确认 V1.1 基线");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        return rows;
    }

    static V11ChecksumDisposition classifyV11HistoryRow(HistoryRow row) {
        Objects.requireNonNull(row, "row");
        if (!row.success()) {
            throw new IllegalStateException("flyway_schema_history 中 V1.1 记录不是成功迁移");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        if (isCleanBaselineRow(row)) {
            return V11ChecksumDisposition.CLEAN_BASELINE;
        }
        if (!"SQL".equals(row.type())
                || !"V1.1__system_base_schema.sql".equals(row.script())) {
            throw new IllegalStateException("flyway_schema_history 中 V1.1 记录既不是 clean BASELINE，也不是正式 SQL 迁移");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        if (row.checksum() == null) {
            throw new IllegalStateException("flyway_schema_history 中 V1.1 SQL 迁移 checksum 不能为空");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        return classifyV11Checksum(row.checksum());
    }

    private static boolean isCleanBaselineRow(HistoryRow row) {
        return EXISTING_BASELINE_VERSION.equals(row.version())
                && "<< Flyway Baseline >>".equals(row.description())
                && "BASELINE".equals(row.type())
                && "<< Flyway Baseline >>".equals(row.script())
                && row.checksum() == null
                && row.success();
    }

    static V11ChecksumDisposition classifyV11Checksum(int appliedChecksum) {
        if (appliedChecksum == CURRENT_V11_CHECKSUM) {
            return V11ChecksumDisposition.CURRENT;
        }
        if (appliedChecksum == LEGACY_V11_CHECKSUM) {
            return V11ChecksumDisposition.LEGACY_REPAIR_REQUIRED;
        }
        throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                "未知 V1.1 checksum: " + appliedChecksum + "；只允许当前或已发布 legacy checksum");
    }

    private static Integer readNullableInt(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    static void validateV11ChecksumRepairScope(
            Map<Integer, HistoryRow> before,
            Map<Integer, HistoryRow> after,
            int repairedRank,
            int expectedChecksum) {
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        if (expectedChecksum != CURRENT_V11_CHECKSUM) {
            throw new IllegalStateException("V1.1 checksum 对齐目标不是当前源码 checksum");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        if (!before.keySet().equals(after.keySet())) {
            throw new IllegalStateException("V1.1 checksum 对齐不得增删 flyway_schema_history 行");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }

        int changedRows = 0;
        for (Integer installedRank : before.keySet()) {
            HistoryRow beforeRow = before.get(installedRank);
            HistoryRow afterRow = after.get(installedRank);
            if (Objects.equals(beforeRow, afterRow)) {
                continue;
            }
            changedRows++;
            if (installedRank != repairedRank) {
                throw new IllegalStateException("V1.1 checksum 对齐修改了非目标 history 行: " + installedRank);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
            if (beforeRow == null || afterRow == null || !beforeRow.sameExceptChecksum(afterRow)) {
                throw new IllegalStateException("V1.1 checksum 对齐修改了 checksum 以外的字段");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
            if (!Objects.equals(beforeRow.checksum(), LEGACY_V11_CHECKSUM)
                    || !Objects.equals(afterRow.checksum(), CURRENT_V11_CHECKSUM)
                    || !afterRow.success()
                    || !EXISTING_BASELINE_VERSION.equals(afterRow.version())
                    || !"V1.1__system_base_schema.sql".equals(afterRow.script())) {
                throw new IllegalStateException("V1.1 checksum 对齐结果不符合受控契约");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
        }
        if (changedRows != 1) {
            throw new IllegalStateException("V1.1 checksum 对齐必须恰好修改一行，实际=" + changedRows);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
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

    private static Map<String, Set<String>> readRequiredColumns(Connection connection) throws SQLException {
        Map<String, Set<String>> columns = new LinkedHashMap<>();
        for (String tableName : REQUIRED_EXISTING_COLUMNS.keySet()) {
            columns.put(tableName, readColumnNames(connection, tableName));
        }
        return columns;
    }

    private static Map<String, Set<String>> readRequiredIndexes(Connection connection) throws SQLException {
        Map<String, Set<String>> indexes = new LinkedHashMap<>();
        for (String tableName : REQUIRED_EXISTING_INDEXES.keySet()) {
            indexes.put(tableName, readIndexNames(connection, tableName));
        }
        return indexes;
    }

    private static Set<String> readColumnNames(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        Set<String> columnNames = collectColumnNames(metadata, catalog, schema, tableName);
        if (columnNames.isEmpty()) {
            columnNames = collectColumnNames(metadata, catalog, null, tableName);
        }
        if (columnNames.isEmpty()) {
            columnNames = collectColumnNames(metadata, null, null, tableName);
        }
        return columnNames;
    }

    private static Set<String> collectColumnNames(
            DatabaseMetaData metadata, String catalog, String schema, String tableName) throws SQLException {
        Set<String> columnNames = new HashSet<>();
        try (ResultSet resultSet = metadata.getColumns(catalog, schema, tableName, "%")) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                if (columnName != null && !columnName.isBlank()) {
                    columnNames.add(columnName.toLowerCase(Locale.ROOT));
                }
            }
        }
        return columnNames;
    }

    private static Set<String> readIndexNames(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        Set<String> indexNames = collectIndexNames(metadata, catalog, schema, tableName);
        if (indexNames.isEmpty()) {
            indexNames = collectIndexNames(metadata, catalog, null, tableName);
        }
        if (indexNames.isEmpty()) {
            indexNames = collectIndexNames(metadata, null, null, tableName);
        }
        return indexNames;
    }

    private static Set<String> collectIndexNames(
            DatabaseMetaData metadata, String catalog, String schema, String tableName) throws SQLException {
        Set<String> indexNames = new HashSet<>();
        try (ResultSet resultSet = metadata.getIndexInfo(catalog, schema, tableName, false, false)) {
            while (resultSet.next()) {
                String indexName = resultSet.getString("INDEX_NAME");
                if (indexName != null && !indexName.isBlank()) {
                    indexNames.add(indexName.toLowerCase(Locale.ROOT));
                }
            }
        }
        return indexNames;
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
                throw new IllegalStateException("clean baseline 未声明任何基础表");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
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
            throw new IllegalStateException("clean baseline 包含公开种子/演示数据 INSERT，拒绝执行");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        List<String> statements = SqlScriptSplitter.split(script);
        if (statements.isEmpty()) {
            throw new IllegalStateException("clean baseline 没有可执行语句");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
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
        throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
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
            throw new IllegalStateException("clean baseline 源文件 SHA-256 与 manifest 不一致");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        if (!Objects.equals(baselineHash, expectedBaselineHash)) {
            throw new IllegalStateException("clean baseline SHA-256 与 manifest 不一致");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
    }

    private static String extractHash(Pattern pattern, String text, String field) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) {
            throw new IllegalStateException("clean baseline manifest 缺少 " + field);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
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

    static int flywayChecksum(byte[] resourceBytes) {
        Objects.requireNonNull(resourceBytes, "resourceBytes");
        CRC32 crc32 = new CRC32();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(resourceBytes), StandardCharsets.UTF_8), 4096)) {
            String line = reader.readLine();
            if (line != null) {
                if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                    line = line.substring(1);
                }
                do {
                    crc32.update(line.getBytes(StandardCharsets.UTF_8));
                } while ((line = reader.readLine()) != null);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("无法计算 Flyway checksum", ex);
        }
        return (int) crc32.getValue();
    }

    private static int loadCurrentV11Checksum() {
        try {
            return flywayChecksum(readRequiredResource(SOURCE_BASELINE_RESOURCE));
        } catch (IOException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
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
