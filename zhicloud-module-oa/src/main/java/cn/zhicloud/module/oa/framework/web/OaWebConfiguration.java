package cn.zhicloud.module.oa.framework.web;

import cn.zhicloud.framework.swagger.config.ZhiCloudSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OA 模块的 web 组件的 Configuration
 *
 * @author zhicloud
 */
@Configuration(proxyBeanMethods = false)
public class OaWebConfiguration {

    /**
     * OA 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi oaGroupedOpenApi() {
        return ZhiCloudSwaggerAutoConfiguration.buildGroupedOpenApi("oa");
    }

}
