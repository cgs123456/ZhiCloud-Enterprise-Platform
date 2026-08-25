package cn.zhicloud.server.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.runtime.ApplicationModulesRuntime;
import org.springframework.modulith.runtime.ApplicationRuntime;

/**
 * Spring Modulith 配置（A3）
 *
 * <p>问题：zhicloud-server 启动类在 {@code cn.zhicloud.server}，
 * 但业务模块在 {@code cn.zhicloud.module.*}（兄弟包，非子包）。
 * Modulith 默认以 {@link ApplicationRuntime#getMainApplicationClass()} 的包为根检测模块，
 * 因此扫描 {@code cn.zhicloud.server.*}，找不到业务模块的 {@code @ApplicationModule} 注解。
 *
 * <p>修复：
 * <ol>
 *   <li>显式构建 {@link ApplicationModules}，指定根包为 {@code cn.zhicloud.module}，
 *       让 Modulith 在业务模块包下检测 {@code @ApplicationModule} 注解。</li>
 *   <li>覆盖默认的 {@link ApplicationRuntime} 和 {@link ApplicationModulesRuntime} Bean，
 *       让 {@code /actuator/modulith} 端点使用我们的 {@link ApplicationModules}，
 *       而非默认的 {@code ApplicationModulesBootstrap.initializeApplicationModules(mainClass)}，
 *       后者只能从启动类包扫描，无法识别业务模块。</li>
 * </ol>
 *
 * <p>验证：访问 {@code /actuator/modulith} 应返回 9 个业务模块（system/erp/mes/wms/qms/crm/ai/airag/aimultiagent）。
 *
 * @author zhicloud
 */
@Slf4j
@Configuration
public class ModulithConfiguration {

    /**
     * 业务模块根包：所有 zhicloud-module-* 模块的公共父包
     */
    private static final String MODULE_BASE_PACKAGE = "cn.zhicloud.module";

    /**
     * 自定义 {@link ApplicationModules} Bean，从业务模块根包检测。
     *
     * <p>覆盖 Modulith 默认的 {@code ModulithAutoConfiguration.applicationModules(...)}，
     * 后者使用启动类所在包 {@code cn.zhicloud.server}，扫描不到业务模块。
     */
    @Bean
    public ApplicationModules applicationModules() {
        ApplicationModules modules = ApplicationModules.of(MODULE_BASE_PACKAGE);
        log.info("[ModulithConfiguration] ApplicationModules 创建完成: basePackage={}, modules={}",
                MODULE_BASE_PACKAGE, modules);
        return modules;
    }

    /**
     * 自定义 {@link ApplicationRuntime} Bean，覆盖默认实现。
     *
     * <p>这里使用默认的 {@link ApplicationRuntime#of(ApplicationContext)}，但通过显式声明为 Bean，
     * 触发 {@code @ConditionalOnMissingBean(ApplicationRuntime.class)} 跳过默认创建，
     * 让后续 {@link #applicationModulesRuntime} 能拿到这个 Bean。
     */
    @Bean
    public ApplicationRuntime applicationRuntime(ApplicationContext applicationContext) {
        log.info("[ModulithConfiguration] ApplicationRuntime 创建完成 (覆盖默认)");
        return ApplicationRuntime.of(applicationContext);
    }

    /**
     * 自定义 {@link ApplicationModulesRuntime} Bean，包装我们的 {@link ApplicationModules}。
     *
     * <p>覆盖 Modulith 默认的 {@code SpringModulithRuntimeAutoConfiguration.modulesRuntime(...)}，
     * 后者通过 {@code ApplicationModulesBootstrap.initializeApplicationModules(mainClass)} 从启动类包加载，
     * 找不到业务模块。这里直接注入我们的 {@link ApplicationModules}，保证 actuator endpoint 能正确显示。
     */
    @Bean
    public ApplicationModulesRuntime applicationModulesRuntime(
            ApplicationModules applicationModules,
            ApplicationRuntime applicationRuntime) {
        log.info("[ModulithConfiguration] ApplicationModulesRuntime 创建完成，使用自定义 ApplicationModules");
        return new ApplicationModulesRuntime(() -> applicationModules, applicationRuntime);
    }

}
