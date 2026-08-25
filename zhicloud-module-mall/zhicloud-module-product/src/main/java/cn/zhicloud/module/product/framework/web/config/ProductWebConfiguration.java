package cn.zhicloud.module.product.framework.web.config;

import cn.zhicloud.framework.swagger.config.ZhiCloudSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * product 模块的 web 组件的 Configuration
 *
 * @author 智云
 */
@Configuration(proxyBeanMethods = false)
public class ProductWebConfiguration {

    /**
     * product 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi productGroupedOpenApi() {
        return ZhiCloudSwaggerAutoConfiguration.buildGroupedOpenApi("product");
    }

}
