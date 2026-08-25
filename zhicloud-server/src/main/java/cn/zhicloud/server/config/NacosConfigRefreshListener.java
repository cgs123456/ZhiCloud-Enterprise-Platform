package cn.zhicloud.server.config;

import com.alibaba.nacos.api.config.annotation.NacosConfigListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Nacos config refresh listener.
 *
 * <p>Triggered when zhicloud-server.yaml changes in Nacos config center.
 * Only active when zhicloud.config.nacos.enabled=true (via -Dspring.profiles.active=nacos).</p>
 *
 * @author zhicloud
 */
@Component
@ConditionalOnProperty(prefix = "zhicloud.config.nacos", name = "enabled", havingValue = "true")
public class NacosConfigRefreshListener {

    private static final Logger log = LoggerFactory.getLogger(NacosConfigRefreshListener.class);

    @NacosConfigListener(dataId = "zhicloud-server.yaml", groupId = "DEFAULT_GROUP")
    public void onConfigChange(String config) {
        log.info("[NacosConfigRefresh] zhicloud-server.yaml refreshed, length={}", config.length());
    }
}
