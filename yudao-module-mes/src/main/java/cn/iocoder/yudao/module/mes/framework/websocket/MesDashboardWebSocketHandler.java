package cn.iocoder.yudao.module.mes.framework.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MES 生产看板 WebSocket Handler
 *
 * 维护所有连接到 /ws/mes/dashboard 的客户端会话，支持广播看板数据。
 *
 * 前端连接示例：ws://host:48080/ws/mes/dashboard
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class MesDashboardWebSocketHandler implements WebSocketHandler {

    /**
     * 在线客户端会话（key = session.getId()）
     */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        log.info("[mesDashboard][客户端连接成功] sessionId={}，当前在线数={}", session.getId(), sessions.size());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // 看板推送为单向（服务端 -> 客户端），客户端上行消息暂不处理
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("[mesDashboard][传输异常] sessionId={}", session.getId(), exception);
        sessions.remove(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("[mesDashboard][客户端断开连接] sessionId={}，status={}，当前在线数={}",
                session.getId(), status, sessions.size());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 广播消息给所有连接的客户端
     *
     * @param message 文本消息（一般为 JSON 字符串）
     */
    public void sendMessageToAll(String message) {
        if (sessions.isEmpty()) {
            return;
        }
        TextMessage textMessage = new TextMessage(message);
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                sessions.remove(entry.getKey());
                continue;
            }
            try {
                session.sendMessage(textMessage);
            } catch (IOException e) {
                log.warn("[mesDashboard][广播消息失败] sessionId={}", session.getId(), e);
                sessions.remove(entry.getKey());
            }
        }
    }

    /**
     * 当前在线客户端数量
     */
    public int getOnlineCount() {
        return sessions.size();
    }

}