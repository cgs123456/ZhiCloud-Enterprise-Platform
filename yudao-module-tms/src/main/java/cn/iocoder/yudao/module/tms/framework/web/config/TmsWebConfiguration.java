package cn.iocoder.yudao.module.tms.framework.web.config;

import cn.iocoder.yudao.framework.swagger.config.YudaoSwaggerAutoConfiguration;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TMS 模块的 web 组件的 Configuration
 *
 * @author yudao
 */
@Configuration(proxyBeanMethods = false)
public class TmsWebConfiguration {

    /**
     * TMS 模块的 API 分组
     */
    @Bean
    public GroupedOpenApi tmsGroupedOpenApi() {
        return YudaoSwaggerAutoConfiguration.buildGroupedOpenApi("tms");
    }

}
