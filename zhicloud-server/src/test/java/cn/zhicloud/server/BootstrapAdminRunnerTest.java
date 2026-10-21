package cn.zhicloud.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BootstrapAdminRunnerTest {

    private static final String DB_URL =
            "jdbc:h2:mem:bootstrap_runner;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
    private static final String USERNAME = "zhicloud_admin";
    private static final String PASSWORD = "correct-horse-1";
    private static final String TENANT_ID = "1";

    @BeforeEach
    void setUpSchema() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
            statement.execute("CREATE TABLE system_role (" +
                    "id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, status TINYINT NOT NULL, deleted TINYINT NOT NULL)");
            statement.execute("CREATE TABLE system_post (" +
                    "id BIGINT PRIMARY KEY, tenant_id BIGINT NOT NULL, status TINYINT NOT NULL, deleted TINYINT NOT NULL)");
            statement.execute("CREATE TABLE system_users (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(30) NOT NULL, password VARCHAR(100) NOT NULL, " +
                    "nickname VARCHAR(30) NOT NULL, remark VARCHAR(500), dept_id BIGINT, post_ids VARCHAR(255), " +
                    "email VARCHAR(50) NOT NULL DEFAULT '', mobile VARCHAR(11) NOT NULL DEFAULT '', sex TINYINT, " +
                    "avatar VARCHAR(512), status TINYINT NOT NULL, login_ip VARCHAR(50), login_date TIMESTAMP, " +
                    "creator VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updater VARCHAR(64), " +
                    "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted TINYINT NOT NULL, tenant_id BIGINT NOT NULL)");
            statement.execute("CREATE TABLE system_user_role (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, role_id BIGINT NOT NULL, " +
                    "creator VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updater VARCHAR(64), " +
                    "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted TINYINT NOT NULL, tenant_id BIGINT NOT NULL)");
            statement.execute("CREATE TABLE system_user_post (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, post_id BIGINT NOT NULL, " +
                    "creator VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updater VARCHAR(64), " +
                    "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted TINYINT NOT NULL, tenant_id BIGINT NOT NULL)");
            statement.execute("INSERT INTO system_role(id, tenant_id, status, deleted) VALUES (1, 1, 0, 0)");
            statement.execute("INSERT INTO system_post(id, tenant_id, status, deleted) VALUES (4, 1, 0, 0)");
        }
    }

    @AfterEach
    void tearDownSchema() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP ALL OBJECTS");
        }
    }

    @Test
    void createsAdminAndRequiredBindings() throws Exception {
        int exitCode = run(baseEnv(PASSWORD));

        assertEquals(0, exitCode);
        assertEquals(1, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1 AND username = ? AND deleted = 0"));
        assertEquals(1, count("SELECT COUNT(*) FROM system_user_role WHERE tenant_id = 1 AND deleted = 0"));
        assertEquals(1, count("SELECT COUNT(*) FROM system_user_post WHERE tenant_id = 1 AND deleted = 0"));
        String hash = scalar("SELECT password FROM system_users WHERE tenant_id = 1 AND username = ?");
        assertTrue(hash.startsWith("$2a$10$"));
        assertTrue(new BCryptPasswordEncoder(10).matches(PASSWORD, hash));
    }

    @Test
    void repeatedEnsureWithSamePasswordIsNoop() throws Exception {
        assertEquals(0, run(baseEnv(PASSWORD)));
        String firstHash = scalar("SELECT password FROM system_users WHERE tenant_id = 1 AND username = ?");

        assertEquals(0, run(baseEnv(PASSWORD)));
        String secondHash = scalar("SELECT password FROM system_users WHERE tenant_id = 1 AND username = ?");

        assertEquals(firstHash, secondHash);
        assertEquals(1, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1 AND username = ? AND deleted = 0"));
    }

    @Test
    void ensureRejectsDifferentPasswordWithoutMutation() throws Exception {
        assertEquals(0, run(baseEnv(PASSWORD)));
        String firstHash = scalar("SELECT password FROM system_users WHERE tenant_id = 1 AND username = ?");

        Map<String, String> env = baseEnv("different-pass-1");
        assertEquals(1, run(env));

        assertEquals(firstHash, scalar("SELECT password FROM system_users WHERE tenant_id = 1 AND username = ?"));
        assertEquals(1, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1 AND username = ? AND deleted = 0"));
    }

    @Test
    void rotateUpdatesPasswordAndRepairsMissingBindings() throws Exception {
        assertEquals(0, run(baseEnv(PASSWORD)));
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("UPDATE system_user_role SET deleted = 1 WHERE tenant_id = 1");
            statement.execute("UPDATE system_user_post SET deleted = 1 WHERE tenant_id = 1");
        }
        String newPassword = "new-correct-1";
        Map<String, String> env = baseEnv(newPassword);
        env.put(BootstrapAdminRunner.MODE_ENV, BootstrapAdminRunner.ROTATE_MODE);

        assertEquals(0, run(env));

        String hash = scalar("SELECT password FROM system_users WHERE tenant_id = 1 AND username = ?");
        assertTrue(new BCryptPasswordEncoder(10).matches(newPassword, hash));
        assertEquals(1, count("SELECT COUNT(*) FROM system_user_role WHERE tenant_id = 1 AND deleted = 0"));
        assertEquals(1, count("SELECT COUNT(*) FROM system_user_post WHERE tenant_id = 1 AND deleted = 0"));
    }

    @Test
    void duplicateActiveUsernameFailsClosed() throws Exception {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO system_users(username, password, nickname, status, deleted, tenant_id) VALUES (?, ?, ?, 0, 0, 1)")) {
            statement.setString(1, USERNAME);
            statement.setString(2, "$2a$10$" + "a".repeat(53));
            statement.setString(3, "first");
            statement.executeUpdate();
            statement.setString(1, USERNAME);
            statement.setString(2, "$2a$10$" + "b".repeat(53));
            statement.setString(3, "second");
            statement.executeUpdate();
        }

        assertEquals(1, run(baseEnv(PASSWORD)));
        assertEquals(2, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1 AND username = ? AND deleted = 0"));
        assertEquals(0, count("SELECT COUNT(*) FROM system_user_role WHERE tenant_id = 1"));
    }

    @Test
    void missingRoleFailsClosedBeforeCreatingUser() throws Exception {
        try (Connection connection = openConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM system_role WHERE id = 1");
        }

        assertEquals(1, run(baseEnv(PASSWORD)));
        assertEquals(0, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1"));
    }

    @Test
    void shortPasswordIsRejectedBeforeDatabaseMutation() throws Exception {
        assertEquals(1, run(baseEnv("too-short")));
        assertEquals(0, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1"));
    }

    @Test
    void overlongPasswordIsRejectedBeforeDatabaseMutation() throws Exception {
        assertEquals(1, run(baseEnv("correct-horse-1234")));
        assertEquals(0, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1"));
    }

    @Test
    void seedUsernameIsRejectedBeforeDatabaseMutation() throws Exception {
        Map<String, String> env = baseEnv(PASSWORD);
        env.put(BootstrapAdminRunner.USERNAME_ENV, "admin");

        assertEquals(1, run(env));
        assertEquals(0, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1"));
    }

    @Test
    void unknownModeIsRejectedBeforeDatabaseMutation() throws Exception {
        Map<String, String> env = baseEnv(PASSWORD);
        env.put(BootstrapAdminRunner.MODE_ENV, "destroy");

        assertEquals(1, run(env));
        assertEquals(0, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1"));
    }

    @Test
    void missingPasswordEnvironmentFailsClosed() throws Exception {
        Map<String, String> env = baseEnv(PASSWORD);
        env.remove(BootstrapAdminRunner.PASSWORD_ENV);

        assertEquals(1, run(env));
        assertEquals(0, count("SELECT COUNT(*) FROM system_users WHERE tenant_id = 1"));
    }

    @Test
    void seedUsernameListDoesNotContainProductionBootstrapName() {
        assertFalse(BootstrapAdminRunner.KNOWN_SEED_USERNAMES.contains(USERNAME));
    }

    private int run(Map<String, String> env) {
        return BootstrapAdminRunner.run(env, BootstrapAdminRunnerTest::openConnection);
    }

    private Map<String, String> baseEnv(String password) {
        Map<String, String> env = new HashMap<>();
        env.put("MASTER_DATASOURCE_URL", "jdbc:mysql://test.invalid/zhicloud_platform");
        env.put("MASTER_DATASOURCE_USERNAME", "sa");
        env.put("MASTER_DATASOURCE_PASSWORD", "test-db-password");
        env.put(BootstrapAdminRunner.USERNAME_ENV, USERNAME);
        env.put(BootstrapAdminRunner.PASSWORD_ENV, password);
        env.put(BootstrapAdminRunner.TENANT_ID_ENV, TENANT_ID);
        env.put(BootstrapAdminRunner.ROLE_ID_ENV, "1");
        env.put(BootstrapAdminRunner.POST_ID_ENV, "4");
        env.put(BootstrapAdminRunner.BCRYPT_COST_ENV, "10");
        return env;
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, "sa", "");
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (sql.contains("?")) {
                statement.setString(1, USERNAME);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getInt(1);
            }
        }
    }

    private String scalar(String sql) throws SQLException {
        try (Connection connection = openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (sql.contains("?")) {
                statement.setString(1, USERNAME);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                return resultSet.getString(1);
            }
        }
    }
}