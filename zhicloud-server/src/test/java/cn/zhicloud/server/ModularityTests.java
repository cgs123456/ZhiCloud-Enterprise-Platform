package cn.zhicloud.server;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith 模块边界校验测试（A3 升级）
 *
 * <p>本测试类不验证业务逻辑，仅校验模块边界：
 * <ul>
 *   <li>{@link #verifyModularity()}：校验所有模块的依赖关系是否符合声明，禁止循环依赖、禁止跨模块直接访问内部实现</li>
 *   <li>{@link #writeDocumentation()}：自动生成 PlantUML 模块文档到 target/spring-modulith-docs/</li>
 * </ul>
 *
 * <p>注意：本测试需要 Spring Boot 上下文，运行时间相对较长，但应在 CI 中作为强制门控。
 *
 * <p>失败时常见原因：
 * <ol>
 *   <li>模块 package-info.java 缺失或未声明 {@code @org.springframework.modulith.ApplicationModule}</li>
 *   <li>某模块直接访问了另一个模块的内部包（非 api 包）</li>
 *   <li>出现模块间循环依赖</li>
 * </ol>
 *
 * @author zhicloud
 */
class ModularityTests {

    /**
     * 校验 Spring Modulith 模块结构。
     * <p>ApplicationModules.of(ZhiCloudServerApplication.class) 会扫描所有带 package-info.java 的模块，
     * 校验它们的显式声明依赖（@ApplicationModule(allowedDependencies=...)）和实际使用是否一致。
     * <p>当前项目尚未为所有模块添加 @ApplicationModule 注解，先做基本校验。
     * 后续逐步为各模块补齐 @ApplicationModule(allowedDependencies=...) 声明后，校验严格度会自动提升。
     */
    @Test
    void verifyModularity() {
        ApplicationModules.of(ZhiCloudServerApplication.class).verify();
    }

    /**
     * 生成模块文档（PlantUML + AsciiDoc）。
     * <p>输出目录：target/spring-modulith-docs/
     * <p>本测试默认不强制运行，可在 CI 中手动触发或通过 mvn test -Dtest=ModularityTests#writeDocumentation 单独运行。
     * <p>产出的 PlantUML 文件可用于架构图自动生成。
     */
    @Test
    void writeDocumentation() {
        new Documenter(ApplicationModules.of(ZhiCloudServerApplication.class))
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml();
    }

}
