package com.codelab.interfaces.ws;

import com.codelab.domain.User;
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

    private final Map<Long, WebSocketSession> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从会话属性中获取用户信息
        Object userObj = session.getAttributes().get("user");
        if (userObj == null || !(userObj instanceof User)) {
            log.warn("WS unauthorized connection - no user in session");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        
        User user = (User) userObj;
        sessionsByUser.put(user.getId(), session);
        log.info("WS connected: {}", user.getUsername());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessionsByUser.entrySet().removeIf(e -> e.getValue().getId().equals(session.getId()));
    }

    public void pushToUser(Long userId, String message) {
        WebSocketSession s = sessionsByUser.get(userId);
        if (s != null && s.isOpen()) {
            try {
                s.sendMessage(new TextMessage(message));
            } catch (Exception ignored) {}
        }
    }
}