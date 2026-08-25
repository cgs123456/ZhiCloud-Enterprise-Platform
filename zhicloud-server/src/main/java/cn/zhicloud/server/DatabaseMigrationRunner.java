package cn.zhicloud.server;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;

/**
 * 数据库迁移独立入口（供 Kubernetes PreSync Job / CI 显式调用）
 *
 * <p><b>为什么需要它</b>：以往迁移完全依赖应用启动时的 Spring Boot Flyway 自动执行，
 * 这在多副本滚动发布下有两个硬伤：
 * <ol>
 *   <li>迁移与业务启动耦合，DDL 失败表现为「Pod 起不来」，排障成本高；</li>
 *   <li>多副本同时启动会并发触发迁移，只能依赖数据库锁兜底，缺少显式的前置串行化步骤。</li>
 * </ol>
 * 本类把迁移拆成独立的一次性进程：<b>先迁移成功，再滚动业务副本</b>。
 *
 * <p><b>运行方式</b>（复用业务镜像，无需额外构建迁移镜像）：
 * <pre>
 * java -cp /zhicloud-server/app.jar \
 *      -Dloader.main=cn.zhicloud.server.DatabaseMigrationRunner \
 *      org.springframework.boot.loader.launch.PropertiesLauncher
 * </pre>
 *
 * <p><b>退出码</b>：0 表示迁移成功（含「无待执行脚本」）；1 表示失败，CI/ArgoCD 据此阻断发布。
 *
 * <p><b>并发安全</b>：Flyway 在 MySQL 上通过 {@code GET_LOCK} 对 schema history 表加锁，
 * 即使 Job 因重试而并发，也只有一个进程能执行迁移，其余等待后观察到无待执行脚本。
 *
 * @author zhicloud
 */
public final class DatabaseMigrationRunner {

    /**
     * 迁移脚本位置，与 application.yaml 的 spring.flyway.locations 保持一致
     */
    private static final String LOCATIONS = "classpath:db/migration";

    /**
     * 迁移历史表名，与 application.yaml 的 spring.flyway.table 保持一致
     */
    private static final String HISTORY_TABLE = "flyway_schema_history";

    private DatabaseMigrationRunner() {
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        try {
            String url = requireEnv("MASTER_DATASOURCE_URL", "SPRING_DATASOURCE_URL");
            String username = requireEnv("MASTER_DATASOURCE_USERNAME", "SPRING_DATASOURCE_USERNAME");
            String password = requireEnv("MASTER_DATASOURCE_PASSWORD", "SPRING_DATASOURCE_PASSWORD");

            System.out.println("[flyway-migration] target = " + maskUrl(url));
            Flyway flyway = Flyway.configure(DatabaseMigrationRunner.class.getClassLoader())
                    .dataSource(url, username, password)
                    .locations(LOCATIONS)
                    .table(HISTORY_TABLE)
                    .encoding("UTF-8")
                    // 与 application.yaml 完全对齐：存量库建基线、严格校验、禁止乱序
                    .baselineOnMigrate(true)
                    .baselineVersion("1")
                    .validateOnMigrate(true)
                    .outOfOrder(false)
                    // 迁移失败时不清库，交由人工介入，避免误删生产数据
                    .cleanDisabled(true)
                    .load();

            MigrateResult result = flyway.migrate();
            System.out.printf("[flyway-migration] success: applied=%d, initialVersion=%s, targetVersion=%s, cost=%dms%n",
                    result.migrationsExecuted, result.initialSchemaVersion, result.targetSchemaVersion,
                    System.currentTimeMillis() - start);
            System.exit(0);
        } catch (Throwable ex) {
            System.err.println("[flyway-migration] FAILED: " + ex.getMessage());
            ex.printStackTrace(System.err);
            // 非 0 退出码是 ArgoCD PreSync / CI 阻断发布的唯一依据，必须显式返回
            System.exit(1);
        }
    }

    /**
     * 读取环境变量，支持多个候选名；全部缺失则直接失败（fail-fast，避免连到错误的库）
     */
    private static String requireEnv(String... names) {
        for (String name : names) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        // @bare-throw-ignore 本类是脱离 Spring 容器运行的迁移 CLI（K8s PreSync Job 的 main 方法），
        // 异常只会被上方 catch(Throwable) 接住并转成非 0 退出码，永远不会返回给任何 API 调用方。
        // 若改用 ServiceException 则必须为它注册一个全局业务错误码，纯属污染错误码空间。
        throw new IllegalStateException("缺少必需的环境变量: " + String.join(" 或 ", names));
    }

    /**
     * 打印数据源时抹掉 query string，避免把密码等敏感参数写进 Pod 日志
     */
    private static String maskUrl(String url) {
        int idx = url.indexOf('?');
        return idx > 0 ? url.substring(0, idx) : url;
    }

}
