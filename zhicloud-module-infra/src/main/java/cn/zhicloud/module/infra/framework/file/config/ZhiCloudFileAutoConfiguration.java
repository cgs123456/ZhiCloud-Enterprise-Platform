package cn.zhicloud.module.infra.framework.file.config;

import cn.zhicloud.module.infra.framework.file.core.client.FileClientFactory;
import cn.zhicloud.module.infra.framework.file.core.client.FileClientFactoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 文件配置类
 *
 * @author 智云
 */
@Configuration(proxyBeanMethods = false)
public class ZhiCloudFileAutoConfiguration {

    @Bean
    public FileClientFactory fileClientFactory() {
        return new FileClientFactoryImpl();
    }

}
