package cn.zhicloud.module.report.framework.security.config;

import cn.zhicloud.framework.security.config.AuthorizeRequestsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * Report 模块的 Security 配置
 */
@Configuration("reportSecurityConfiguration")
public class SecurityConfiguration {

    @Bean("reportAuthorizeRequestsCustomizer")
    public AuthorizeRequestsCustomizer authorizeRequestsCustomizer() {
        return new AuthorizeRequestsCustomizer() {

            @Override
            public void customize(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
                // 安全加固：积木报表仅放行只读展示端点，其余（design/datasource/drag 等管理端点）走认证
                registry.requestMatchers("/jmreport/view/**").permitAll();
                registry.requestMatchers("/jmreport/preview/**").permitAll();
                registry.requestMatchers("/jmreport/img/**").permitAll();
            }

        };
    }

}
