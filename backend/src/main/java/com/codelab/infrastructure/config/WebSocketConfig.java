package com.codelab.infrastructure.config;

import com.codelab.interfaces.ws.ExecutionWebSocketHandler;
import com.codelab.domain.User;
import com.codelab.service.UserService;
import com.codelab.infrastructure.security.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Optional;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    private final ExecutionWebSocketHandler webSocketHandler;
    private final JwtTokenUtils jwtTokenUtils;
    private final UserService userService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws/execution-result")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                            WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
                        
                        if (request instanceof ServletServerHttpRequest) {
                            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                            HttpServletRequest httpRequest = servletRequest.getServletRequest();
                            
                            // 从查询参数或请求头中获取token
                            String token = getTokenFromRequest(httpRequest);
                            
                            if (token != null && jwtTokenUtils.validateToken(token)) {
                                try {
                                    String username = jwtTokenUtils.getUsernameFromToken(token);
                                    Optional<User> userOpt = userService.findByUsername(username);
                                    if (userOpt.isPresent()) {
                                        attributes.put("user", userOpt.get());
                                        log.info("WS handshake successful for user: {}", username);
                                        return true;
                                    }
                                } catch (Exception e) {
                                    log.warn("WS handshake failed - token validation error: {}", e.getMessage());
                                }
                            }
                            
                            log.warn("WS handshake failed - no valid token");
                            return false;
                        }
                        return false;
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                            WebSocketHandler wsHandler, Exception exception) {
                        // Nothing to do after handshake
                    }
                })
                .setAllowedOrigins("*");
    }
    
    private String getTokenFromRequest(HttpServletRequest request) {
        // 首先尝试从Authorization头获取
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 然后尝试从查询参数获取
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isEmpty()) {
            return tokenParam;
        }
        
        return null;
    }
}