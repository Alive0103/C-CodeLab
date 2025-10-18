package com.codelab.interfaces.ws;

import com.codelab.domain.User;
import com.codelab.infrastructure.security.JwtTokenUtils;
import com.codelab.service.UserService;
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
    private final JwtTokenUtils jwtTokenUtils;
    private final UserService userService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从URL参数中获取token
        String token = session.getUri().getQuery();
        if (token == null || !token.startsWith("token=")) {
            log.warn("WS unauthorized connection - no token in URL");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }
        
        // 提取token值
        String tokenValue = token.substring(6); // 移除 "token=" 前缀
        
        // 验证token
        if (!jwtTokenUtils.validateToken(tokenValue)) {
            log.warn("WS unauthorized connection - invalid token");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid token"));
            return;
        }
        
        // 从token中获取用户名
        String username = jwtTokenUtils.getUsernameFromToken(tokenValue);
        User user = userService.findByUsername(username).orElse(null);
        
        if (user == null) {
            log.warn("WS unauthorized connection - user not found: {}", username);
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("User not found"));
            return;
        }
        
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