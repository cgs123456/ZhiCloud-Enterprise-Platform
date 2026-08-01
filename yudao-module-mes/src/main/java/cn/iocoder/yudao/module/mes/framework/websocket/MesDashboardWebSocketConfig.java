package cn.iocoder.yudao.module.mes.framework.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * MES 生产看板 WebSocket 配置
 *
 * 注册端点 /ws/mes/dashboard，前端可通过 ws://host:48080/ws/mes/dashboard 连接。
 *
 * 同时启用 @EnableScheduling 以驱动 {@link MesDashboardPushService} 的定时推送。
 * （若全局已启用 @EnableScheduling，此处重复声明无副作用。）
 *
 * @author 芋道源码
 */
@Configuration
@EnableWebSocket
@EnableScheduling
public class MesDashboardWebSocketConfig implements WebSocketConfigurer {

    private final MesDashboardWebSocketHandler mesDashboardWebSocketHandler;

    public MesDashboardWebSocketConfig(MesDashboardWebSocketHandler mesDashboardWebSocketHandler) {
        this.mesDashboardWebSocketHandler = mesDashboardWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册看板推送端点，允许跨域以便前端直连
        registry.addHandler(mesDashboardWebSocketHandler, "/ws/mes/dashboard")
                .setAllowedOriginPatterns("*");
    }

}