package com.codelab.interfaces.ws;

import com.codelab.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecutionWebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider tokenProvider;
    private final Map<String, WebSocketSession> sessionsByUser = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String auth = session.getHandshakeHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String username = null;
        if (auth != null && auth.startsWith("Bearer ")) {
            var claims = tokenProvider.parseClaims(auth.substring(7));
            if (claims != null && !tokenProvider.isTokenExpired(claims)) {
                username = claims.getSubject();
            }
        }
        if (username == null) {
            log.warn("WS unauthorized connection");
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


