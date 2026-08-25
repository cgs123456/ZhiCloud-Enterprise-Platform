package cn.zhicloud.module.hr.framework.web.config;

import cn.zhicloud.framework.swagger.config.ZhiCloudSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * HR 模块的 web 组件的 Configuration
 *
 * @author zhicloud
 */
@Configuration(proxyBeanMethods = false)
public class HrWebConfiguration {

    /**
     * HR 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi hrGroupedOpenApi() {
        return ZhiCloudSwaggerAutoConfiguration.buildGroupedOpenApi("hr");
    }

}