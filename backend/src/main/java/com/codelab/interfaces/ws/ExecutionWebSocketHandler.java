package com.codelab.interfaces.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 简化认证：直接允许连接，实际项目中可以通过session参数传递用户信息
        String username = session.getUri().getQuery();
        if (username == null || username.isEmpty()) {
            log.warn("WS unauthorized connection - no username provided");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        sessionsByUser.put(username, session);
        log.info("WS connected: {}", username);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionsByUser.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
    }

    public void pushToUser(String username, String message) {
        WebSocketSession s = sessionsByUser.get(username);
        if (s != null && s.isOpen()) {
            try {
                s.sendMessage(new TextMessage(message));
            } catch (Exception ignored) {}
        }
    }
}


