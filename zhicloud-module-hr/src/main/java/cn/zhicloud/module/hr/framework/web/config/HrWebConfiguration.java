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
     * HR 模块的 API 分组（旧 /hr 域已退役，分组对齐现存 /hrm 端点）
     */
    @Bean
    public GroupedOpenApi hrmGroupedOpenApi() {
        return ZhiCloudSwaggerAutoConfiguration.buildGroupedOpenApi("hrm");
    }

}
