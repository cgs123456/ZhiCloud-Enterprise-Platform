package cn.zhicloud.module.fms.framework.web;

import cn.zhicloud.framework.swagger.config.ZhiCloudSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FMS 模块的 web 组件的 Configuration
 *
 * @author 智云
 */
@Configuration(proxyBeanMethods = false)
public class FmsWebConfiguration {

    /**
     * FMS 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi fmsGroupedOpenApi() {
        return ZhiCloudSwaggerAutoConfiguration.buildGroupedOpenApi("fms");
    }

}
