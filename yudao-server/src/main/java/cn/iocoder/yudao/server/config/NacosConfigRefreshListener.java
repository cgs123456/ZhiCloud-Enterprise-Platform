package cn.iocoder.yudao.server.config;

import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Nacos config refresh listener.
 *
 * <p>Triggered when yudao-server.yaml changes in Nacos config center.
 * Only active when yudao.config.nacos.enabled=true (via -Dspring.profiles.active=nacos).</p>
 *
 * @author yudao
 */
@Component
@ConditionalOnProperty(prefix = "yudao.config.nacos", name = "enabled", havingValue = "true")
public class NacosConfigRefreshListener {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigRefreshListener.class);

    @NacosConfigListener(dataId = "yudao-server.yaml", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String config) {
        log.info("[NacosConfigRefresh] yudao-server.yaml refreshed, length={}", config.length());
    }
}
