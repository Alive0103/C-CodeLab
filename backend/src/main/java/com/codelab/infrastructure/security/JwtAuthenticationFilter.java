package com.codelab.infrastructure.security;


import com.codelab.infrastructure.common.ApiResponseCode;
import com.codelab.interfaces.web.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtils jwtTokenUtils;
    private final ObjectMapper objectMapper;


    // 处理JWT验证
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                // 直接验证token有效性（通过Redis中的有效token比较）
                if (jwtTokenUtils.validateToken(token)) {
                    username = jwtTokenUtils.getUsernameFromToken(token);
                } else {
                    log.warn("Token validation failed: {}", token);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    ApiResponse<String> errorResponse = ApiResponse.error(ApiResponseCode.UNAUTHORIZED, "Token无效或已失效");
                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    return;
                }
            } catch (Exception e) {
                log.error("Invalid JWT token: {}", token, e);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                ApiResponse<String> errorResponse = ApiResponse.error(ApiResponseCode.UNAUTHORIZED, "Token解析失败");
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        } else {
            // 如果没有Authorization头或格式不正确，继续过滤器链
            filterChain.doFilter(request, response);
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            log.debug("Username extracted from token: {}", username);
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        filterChain.doFilter(request, response);
    }
}