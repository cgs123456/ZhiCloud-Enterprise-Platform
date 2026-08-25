package cn.zhicloud.server.config;

import cn.zhicloud.module.erp.service.finance.cost.bom.ErpBomProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * ERP BOM 数据提供者配置
 *
 * <p>提供 ErpBomProvider 的默认实现，当容器中不存在其他实现时启用。
 */
@Configuration
public class ErpBomProviderConfig {

    @Bean
    public ErpBomProvider erpBomProvider() {
        return new ErpBomProvider() {
            @Override
            public List<cn.zhicloud.module.erp.service.finance.cost.bom.ErpBomComponent> getBomComponents(Long productId) {
                return Collections.emptyList();
            }
        };
    }

}
