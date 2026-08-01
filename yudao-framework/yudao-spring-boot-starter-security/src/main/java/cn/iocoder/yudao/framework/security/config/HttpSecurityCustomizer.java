package cn.iocoder.yudao.framework.security.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 * HttpSecurity 定制器
 *
 * 用于在 {@link YudaoWebSecurityConfigurerAdapter#filterChain} 中按需扩展 HttpSecurity 配置。
 * 与 Spring Security 的 {@code Customizer<T>} 不同，本接口声明 {@code throws Exception}，
 * 以支持调用 {@link HttpSecurity} 上声明 throws Exception 的方法（如 oauth2ResourceServer）。
 *
 * 使用场景：OAuth2 Resource Server 等可选模块通过注册此接口 Bean，
 * 在不修改核心安全配置的前提下注入定制逻辑。
 */
@FunctionalInterface
public interface HttpSecurityCustomizer {

    /**
     * 定制 HttpSecurity 配置
     *
     * @param httpSecurity HttpSecurity 构建器
     * @throws Exception 配置过程中可能抛出的异常
     */
    void customize(HttpSecurity httpSecurity) throws Exception;

}
