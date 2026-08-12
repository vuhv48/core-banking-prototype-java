package com.example.accountdemo.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filter 1 – Authentication.
 *
 * Đọc header "Authorization: Bearer <token>", validate JWT,
 * rồi đặt Authentication vào SecurityContext.
 *
 * Sau filter này, SecurityContext chứa:
 *   principal = username
 *   authorities = danh sách quyền lấy từ JWT claim "permissions"
 *
 * Filter này KHÔNG kiểm tra path/method; việc đó thuộc về AuthorizationFilter
 * được cấu hình trong SecurityConfig.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                List<String> permissions = jwtUtil.getPermissionsFromToken(token);

                List<SimpleGrantedAuthority> authorities = permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            // Token không hợp lệ → SecurityContext rỗng → SecurityConfig sẽ từ chối nếu path cần auth
        }

        filterChain.doFilter(request, response);
    }
}
