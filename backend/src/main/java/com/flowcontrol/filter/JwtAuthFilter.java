package com.flowcontrol.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcontrol.entity.SysUser;
import com.flowcontrol.service.SysUserService;
import com.flowcontrol.util.JwtUtil;
import com.flowcontrol.util.LogMaskUtil;
import com.flowcontrol.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * 拦截每个请求，验证 JWT Token 并设置 Spring Security 上下文
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserService userService;

    @Value("${jwt.header-name}")
    private String headerName;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        // 1. 获取请求头中的 Token
        String token = extractToken(request);

        // 2. 如果有 Token，验证并设置认证信息
        if (StringUtils.hasText(token)) {
            try {
                // 验证 Token 有效性
                if (!jwtUtil.isTokenExpired(token)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    String username = jwtUtil.getUsernameFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);

                    // 从数据库查询用户（确保用户未被禁用）
                    SysUser user = userService.getUserById(userId);
                    if (user != null && user.getStatus() == 1) {
                        // 构建权限列表
                        SimpleGrantedAuthority authority =
                                new SimpleGrantedAuthority("ROLE_" + role);

                        // 构建认证 Token 并存入 SecurityContext
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        Collections.singletonList(authority)
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        log.debug("JWT认证成功: userId={}, username={}", userId, username);
                    }
                }
            } catch (Exception e) {
                log.warn("JWT验证失败: {}", e.getMessage());
                // Token 无效时，清空 SecurityContext
                SecurityContextHolder.clearContext();
            }
        }

        // 3. 继续执行后续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取 JWT Token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(headerName);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // 去掉 "Bearer " 前缀
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 以下路径跳过 JWT 过滤器（已在 SecurityConfig 配置，但双重保险）
        return path.startsWith("/auth/") ||
               path.startsWith("/announcement/public/") ||
               path.equals("/error");
    }
}
