package cn.iocoder.yudao.module.oa.framework.web;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OA 模块的 web 组件的 Configuration
 *
 * @author yudao
 */
@Configuration(proxyBeanMethods = false)
public class OaWebConfiguration {

    /**
     * OA 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi oaGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("oa");
    }

}
