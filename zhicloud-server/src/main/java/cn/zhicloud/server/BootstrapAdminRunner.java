package cn.zhicloud.server;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 生产管理员引导入口（供 Kubernetes PreSync Job / 受控 break-glass 操作显式调用）。
 *
 * <p>默认 {@code ensure} 模式具有严格的幂等和 fail-closed 语义：</p>
 * <ul>
 *   <li>管理员不存在：校验角色、岗位和输入后，在同一事务内创建用户及绑定；</li>
 *   <li>管理员已存在且 BCrypt 口令匹配：不修改口令，仅修复缺失的角色/岗位绑定；</li>
 *   <li>管理员已存在但口令不同：返回非 0，拒绝覆盖，避免 Secret 变更静默改写生产管理员；</li>
 * </ul>
 *
 * <p>{@code rotate} 模式仅用于受控的 break-glass 口令轮换，会显式更新口令并修复绑定。
 * 无论哪种模式，本入口都不会输出明文口令或 BCrypt 哈希。</p>
 *
 * @author zhicloud
 */
public final class BootstrapAdminRunner {

    static final String MODE_ENV = "BOOTSTRAP_ADMIN_MODE";
    static final String ENSURE_MODE = "ensure";
    static final String ROTATE_MODE = "rotate";
    static final String USERNAME_ENV = "BOOTSTRAP_ADMIN_USERNAME";
    static final String PASSWORD_ENV = "BOOTSTRAP_ADMIN_PASSWORD";
    static final String TENANT_ID_ENV = "BOOTSTRAP_ADMIN_TENANT_ID";
    static final String ROLE_ID_ENV = "BOOTSTRAP_ADMIN_ROLE_ID";
    static final String POST_ID_ENV = "BOOTSTRAP_ADMIN_POST_ID";
    static final String BCRYPT_COST_ENV = "BOOTSTRAP_ADMIN_BCRYPT_COST";

    static final Set<String> KNOWN_SEED_USERNAMES = Set.of(
            "admin", "zhicloud", "yuanma", "test", "admin107", "admin108",
            "admin109", "admin110", "newobject", "aoteman", "hrmgr", "aotemane",
            "admin123", "goudan", "wwbwwb", "admin1", "test01", "a00001",
            "aoteman001");

    private static final String DEFAULT_USERNAME = "zhicloud_admin";
    private static final long DEFAULT_TENANT_ID = 1L;
    private static final long DEFAULT_ROLE_ID = 1L;
    private static final long DEFAULT_POST_ID = 4L;
    private static final int DEFAULT_BCRYPT_COST = 12;
    private static final int MIN_BCRYPT_COST = 10;
    private static final int MAX_BCRYPT_COST = 16;
    private static final int MIN_PASSWORD_LENGTH = 12;
    /** 登录接口 AuthLoginReqVO 对口令的 @Length(min = 4, max = 16) 约束，超长口令会导致无法登录。 */
    private static final int MAX_PASSWORD_LENGTH = 16;
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final String ACTOR = "bootstrap-admin";
    private static final Pattern USERNAME_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{2,29}$");

    private BootstrapAdminRunner() {
    }

    @FunctionalInterface
    interface ConnectionFactory {
        Connection open() throws SQLException;
    }

    public static void main(String[] args) {
        System.exit(run(System.getenv(), () -> openConnection(System.getenv())));
    }

    /**
     * 执行引导并返回进程退出码，便于测试时不触发 {@link System#exit(int)}。
     */
    static int run(Map<String, String> environment, ConnectionFactory connectionFactory) {
        long start = System.currentTimeMillis();
        BootstrapConfig config;
        try {
            config = BootstrapConfig.from(environment);
        } catch (Throwable ex) {
            System.err.println("[bootstrap-admin] FAILED: " + safeMessage(ex));
            return 1;
        }

        System.out.printf(
                "[bootstrap-admin] target=%s mode=%s username=%s tenantId=%d%n",
                maskUrl(config.databaseUrl), config.mode, config.username, config.tenantId);

        try (Connection connection = connectionFactory.open()) {
            connection.setAutoCommit(false);
            try {
                String action = execute(connection, config);
                connection.commit();
                System.out.printf(
                        "[bootstrap-admin] success: action=%s username=%s tenantId=%d roleId=%d postId=%d cost=%dms%n",
                        action, config.username, config.tenantId, config.roleId, config.postId,
                        System.currentTimeMillis() - start);
                return 0;
            } catch (Throwable ex) {
                rollbackQuietly(connection);
                throw ex;
            }
        } catch (Throwable ex) {
            System.err.println("[bootstrap-admin] FAILED: " + safeMessage(ex));
            return 1;
        }
    }

    private static String execute(Connection connection, BootstrapConfig config) throws SQLException {
        assertActiveRole(connection, config);
        assertActivePost(connection, config);

        List<ExistingUser> users = findActiveUsers(connection, config);
        if (users.size() > 1) {
            throw new IllegalStateException("同一租户存在多个同名活跃管理员，拒绝自动选择或覆盖");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }

        if (users.isEmpty()) {
            long userId = insertUser(connection, config);
            ensureRoleBinding(connection, config, userId);
            ensurePostBinding(connection, config, userId);
            return "created";
        }

        ExistingUser user = users.get(0);
        if (user.status != 0) {
            throw new IllegalStateException("管理员账号已存在但处于停用状态，拒绝自动启用");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(config.bcryptCost);
        boolean passwordMatches = encoder.matches(config.password, user.passwordHash);
        if (!ENSURE_MODE.equals(config.mode) && !ROTATE_MODE.equals(config.mode)) {
            throw new IllegalStateException("不支持的管理员引导模式: " + config.mode);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        if (ENSURE_MODE.equals(config.mode) && !passwordMatches) {
            throw new IllegalStateException("管理员账号已存在且口令不匹配；ensure 模式拒绝覆盖，需受控 rotate");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }

        boolean bindingsChanged = false;
        bindingsChanged |= ensureRoleBinding(connection, config, user.id);
        bindingsChanged |= ensurePostBinding(connection, config, user.id);

        if (ROTATE_MODE.equals(config.mode)) {
            updatePassword(connection, config, user.id);
            return bindingsChanged ? "rotated_and_repaired" : "rotated";
        }
        return bindingsChanged ? "noop_repaired" : "noop";
    }

    private static void assertActiveRole(Connection connection, BootstrapConfig config) throws SQLException {
        assertActiveReference(
                connection, "system_role", config.roleId, config.tenantId, "角色");
    }

    private static void assertActivePost(Connection connection, BootstrapConfig config) throws SQLException {
        assertActiveReference(
                connection, "system_post", config.postId, config.tenantId, "岗位");
    }

    private static void assertActiveReference(
            Connection connection, String table, long id, long tenantId, String label) throws SQLException {
        String sql = "SELECT id FROM " + table
                + " WHERE id = ? AND tenant_id = ? AND status = 0 AND deleted = 0 FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.setLong(2, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException(label + "不存在、已停用或不属于目标租户: id=" + id);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                }
            }
        }
    }

    private static List<ExistingUser> findActiveUsers(
            Connection connection, BootstrapConfig config) throws SQLException {
        String sql = "SELECT id, password, status FROM system_users"
                + " WHERE tenant_id = ? AND username = ? AND deleted = 0 FOR UPDATE";
        List<ExistingUser> users = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, config.tenantId);
            statement.setString(2, config.username);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(new ExistingUser(
                            resultSet.getLong("id"),
                            resultSet.getString("password"),
                            resultSet.getInt("status")));
                }
            }
        }
        return users;
    }

    private static long insertUser(Connection connection, BootstrapConfig config) throws SQLException {
        String sql = "INSERT INTO system_users ("
                + "username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar, "
                + "status, login_ip, login_date, creator, create_time, updater, update_time, deleted, tenant_id"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, 0, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, config.username);
            statement.setString(2, new BCryptPasswordEncoder(config.bcryptCost).encode(config.password));
            statement.setString(3, "智云");
            statement.setString(4, "生产初始化管理员");
            statement.setLong(5, 103L);
            statement.setString(6, "[" + config.postId + "]");
            statement.setString(7, "");
            statement.setString(8, "");
            statement.setInt(9, 1);
            statement.setNull(10, java.sql.Types.VARCHAR);
            statement.setInt(11, 0);
            statement.setNull(12, java.sql.Types.VARCHAR);
            statement.setNull(13, java.sql.Types.TIMESTAMP);
            statement.setString(14, ACTOR);
            statement.setString(15, ACTOR);
            statement.setLong(16, config.tenantId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("创建管理员后未取得用户主键");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void updatePassword(
            Connection connection, BootstrapConfig config, long userId) throws SQLException {
        String sql = "UPDATE system_users SET password = ?, updater = ?, update_time = CURRENT_TIMESTAMP"
                + " WHERE id = ? AND tenant_id = ? AND deleted = 0 AND status = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, new BCryptPasswordEncoder(config.bcryptCost).encode(config.password));
            statement.setString(2, ACTOR);
            statement.setLong(3, userId);
            statement.setLong(4, config.tenantId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("轮换管理员口令时更新行数不为 1");
            }
        }
    }

    private static boolean ensureRoleBinding(
            Connection connection, BootstrapConfig config, long userId) throws SQLException {
        return ensureBinding(connection, "system_user_role", "role_id", config.roleId, config, userId);
    }

    private static boolean ensurePostBinding(
            Connection connection, BootstrapConfig config, long userId) throws SQLException {
        return ensureBinding(connection, "system_user_post", "post_id", config.postId, config, userId);
    }

    private static boolean ensureBinding(
            Connection connection, String table, String referenceColumn, long referenceId,
            BootstrapConfig config, long userId) throws SQLException {
        String selectSql = "SELECT id, deleted FROM " + table
                + " WHERE tenant_id = ? AND user_id = ? AND " + referenceColumn + " = ?"
                + " ORDER BY deleted ASC, id ASC FOR UPDATE";
        List<BindingRow> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setLong(1, config.tenantId);
            statement.setLong(2, userId);
            statement.setLong(3, referenceId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new BindingRow(resultSet.getLong("id"), resultSet.getInt("deleted")));
                }
            }
        }

        long activeCount = rows.stream().filter(row -> row.deleted == 0).count();
        if (activeCount > 1) {
            throw new IllegalStateException(table + " 存在重复活跃绑定，拒绝自动修复");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        if (activeCount == 1) {
            return false;
        }

        if (!rows.isEmpty()) {
            String updateSql = "UPDATE " + table
                    + " SET deleted = 0, updater = ?, update_time = CURRENT_TIMESTAMP"
                    + " WHERE id = ? AND tenant_id = ? AND user_id = ? AND " + referenceColumn + " = ? AND deleted = 1";
            try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                statement.setString(1, ACTOR);
                statement.setLong(2, rows.get(0).id);
                statement.setLong(3, config.tenantId);
                statement.setLong(4, userId);
                statement.setLong(5, referenceId);
                if (statement.executeUpdate() != 1) {
                    throw new SQLException("修复 " + table + " 绑定失败：更新行数不为 1");
                }
            }
            return true;
        }

        String insertSql = "INSERT INTO " + table
                + " (user_id, " + referenceColumn + ", creator, create_time, updater, update_time, deleted, tenant_id)"
                + " VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, CURRENT_TIMESTAMP, 0, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setLong(1, userId);
            statement.setLong(2, referenceId);
            statement.setString(3, ACTOR);
            statement.setString(4, ACTOR);
            statement.setLong(5, config.tenantId);
            statement.executeUpdate();
        }
        return true;
    }

    private static Connection openConnection(Map<String, String> environment) throws SQLException {
        String url = requireEnv(environment, "MASTER_DATASOURCE_URL", "SPRING_DATASOURCE_URL");
        String username = requireEnv(environment, "MASTER_DATASOURCE_USERNAME", "SPRING_DATASOURCE_USERNAME");
        String password = requireEnv(environment, "MASTER_DATASOURCE_PASSWORD", "SPRING_DATASOURCE_PASSWORD");

        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        copyIfPresent(environment, properties, "MYSQL_TLS_TRUSTSTORE_URL", "trustCertificateKeyStoreUrl");
        copyIfPresent(environment, properties, "MYSQL_TLS_TRUSTSTORE_TYPE", "trustCertificateKeyStoreType");
        copyIfPresent(environment, properties, "MYSQL_TLS_TRUSTSTORE_PASSWORD", "trustCertificateKeyStorePassword");
        return java.sql.DriverManager.getConnection(url, properties);
    }

    private static void copyIfPresent(
            Map<String, String> environment, Properties properties, String envName, String propertyName) {
        String value = environment.get(envName);
        if (value != null && !value.isBlank()) {
            properties.setProperty(propertyName, value);
        }
    }

    private static String requireEnv(Map<String, String> environment, String primary, String fallback) {
        String value = environment.get(primary);
        if (value == null || value.isBlank()) {
            value = environment.get(fallback);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少必要环境变量: " + primary + " 或 " + fallback);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
        }
        return value;
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // 原始异常更重要；回滚失败会在连接关闭时再次暴露。
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        return message.replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * 打印数据源时抹掉 query string，避免把密码等敏感参数写进 Pod 日志。
     */
    static String maskUrl(String url) {
        int idx = url.indexOf('?');
        return idx > 0 ? url.substring(0, idx) : url;
    }

    private record ExistingUser(long id, String passwordHash, int status) {
    }

    private record BindingRow(long id, int deleted) {
    }

    private record BootstrapConfig(
            String databaseUrl,
            String mode,
            String username,
            String password,
            long tenantId,
            long roleId,
            long postId,
            int bcryptCost) {

        private static BootstrapConfig from(Map<String, String> environment) {
            String databaseUrl = requireEnv(
                    environment, "MASTER_DATASOURCE_URL", "SPRING_DATASOURCE_URL");
            String mode = environment.getOrDefault(MODE_ENV, ENSURE_MODE).trim().toLowerCase(Locale.ROOT);
            if (!ENSURE_MODE.equals(mode) && !ROTATE_MODE.equals(mode)) {
                throw new IllegalStateException("不支持的管理员引导模式: " + mode);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }

            String username = environment.getOrDefault(USERNAME_ENV, DEFAULT_USERNAME);
            if (!USERNAME_PATTERN.matcher(username).matches()) {
                throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                        "管理员用户名必须为 3-30 位字母开头，且只能包含字母、数字、点、下划线和连字符");
            }
            if (KNOWN_SEED_USERNAMES.contains(username.toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException("管理员用户名不得复用 V1.1/V115 公开种子账号名");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }

            String password = environment.get(PASSWORD_ENV);
            if (password == null || password.isEmpty()) {
                throw new IllegalStateException("缺少必要环境变量: " + PASSWORD_ENV);  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
            if (password.length() < MIN_PASSWORD_LENGTH) {
                throw new IllegalStateException("管理员口令至少需要 " + MIN_PASSWORD_LENGTH + " 个字符");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
            if (password.length() > MAX_PASSWORD_LENGTH) {
                // 登录接口 AuthLoginReqVO 对口令做了 4-16 位校验：超长口令虽然能写进库，
                // 但登录时会在参数校验阶段被拒，表现为"账号建好了却登不进去"，必须在引导阶段就拦住。
                throw new IllegalStateException("管理员口令长度不得超过 " + MAX_PASSWORD_LENGTH + " 个字符（登录接口限制 4-16 位，超长将无法登录）");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
            if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
                throw new IllegalStateException("管理员口令 UTF-8 长度不得超过 " + MAX_PASSWORD_BYTES + " 字节");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }

            long tenantId = parsePositiveLong(environment, TENANT_ID_ENV, DEFAULT_TENANT_ID);
            long roleId = parsePositiveLong(environment, ROLE_ID_ENV, DEFAULT_ROLE_ID);
            long postId = parsePositiveLong(environment, POST_ID_ENV, DEFAULT_POST_ID);
            int bcryptCost = parsePositiveInt(environment, BCRYPT_COST_ENV, DEFAULT_BCRYPT_COST);
            if (bcryptCost < MIN_BCRYPT_COST || bcryptCost > MAX_BCRYPT_COST) {
                throw new IllegalStateException(  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
                        "BCrypt cost 必须在 " + MIN_BCRYPT_COST + "-" + MAX_BCRYPT_COST + " 之间");
            }
            return new BootstrapConfig(
                    databaseUrl, mode, username, password, tenantId, roleId, postId, bcryptCost);
        }

        private static long parsePositiveLong(
                Map<String, String> environment, String key, long defaultValue) {
            String raw = environment.get(key);
            if (raw == null || raw.isBlank()) {
                return defaultValue;
            }
            try {
                long value = Long.parseLong(raw.trim());
                if (value <= 0) {
                    throw new NumberFormatException("not positive");
                }
                return value;
            } catch (NumberFormatException ex) {
                throw new IllegalStateException(key + " 必须为正整数");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
        }

        private static int parsePositiveInt(
                Map<String, String> environment, String key, int defaultValue) {
            String raw = environment.get(key);
            if (raw == null || raw.isBlank()) {
                return defaultValue;
            }
            try {
                int value = Integer.parseInt(raw.trim());
                if (value <= 0) {
                    throw new NumberFormatException("not positive");
                }
                return value;
            } catch (NumberFormatException ex) {
                throw new IllegalStateException(key + " 必须为正整数");  // @bare-throw-ignore Flyway/引导 CLI 运行在 Spring 容器之外，硬套 ServiceException 只会污染业务错误码空间
            }
        }
    }
}
