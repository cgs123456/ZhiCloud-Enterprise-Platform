package cn.zhicloud.framework.inventory.config;

import cn.zhicloud.framework.inventory.api.InventoryApi;
import cn.zhicloud.framework.inventory.api.InventoryApiImpl;
import cn.zhicloud.framework.inventory.dal.mysql.InventoryItemMapper;
import cn.zhicloud.framework.inventory.service.InventoryReconciliationService;
import cn.zhicloud.framework.inventory.service.InventoryService;
import cn.zhicloud.framework.inventory.service.InventoryServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 共享库存自动配置（P1-4）
 *
 * <p>注意：本 Starter 的 Mapper 位于 {@code cn.zhicloud.framework.inventory.dal.mysql}，
 * 不在业务模块默认的 {@code zhicloud.info.base-package}（{@code cn.zhicloud.module}）扫描范围内，
 * 故在此用 {@link MapperScan} 显式限定包，避免与全局扫描冲突。
 *
 * @author 智云库存治理
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "zhicloud.inventory", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(InventoryProperties.class)
@MapperScan("cn.zhicloud.framework.inventory.dal.mysql")
public class InventoryAutoConfiguration {

    @Bean
    public InventoryService inventoryService() {
        return new InventoryServiceImpl();
    }

    @Bean
    public InventoryApi inventoryApi() {
        return new InventoryApiImpl();
    }

    @Bean
    public InventoryReconciliationService inventoryReconciliationService() {
        return new InventoryReconciliationService();
    }

}
