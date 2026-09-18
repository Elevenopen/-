package com.flowcontrol.config;

import com.flowcontrol.filter.ApiAccessAuditFilter;
import com.flowcontrol.filter.JwtAuthFilter;
import com.flowcontrol.filter.LoginRateLimitFilter;
import com.flowcontrol.filter.SecurityHeadersFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CharacterEncodingFilter;

/**
 * Spring Security 安全配置
 * 配置 JWT 认证策略、路由权限、数据加密
 *
 * Phase 6 安全加固：
 * - 登录限流（防止暴力破解）
 * - 安全响应头（防XSS/点击劫持等）
 * - API 访问审计日志
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)  // 启用方法级权限注解
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private LoginRateLimitFilter loginRateLimitFilter;

    @Autowired
    private SecurityHeadersFilter securityHeadersFilter;

    @Autowired
    private ApiAccessAuditFilter apiAccessAuditFilter;

    /**
     * 认证管理器
     */
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 字符编码过滤器（确保中文正确处理）
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter("UTF-8", true);

        http
                // 0. 字符编码（最优先）
                .addFilterBefore(encodingFilter, UsernamePasswordAuthenticationFilter.class)

                // 1. 安全响应头（点击劫持/XSS防护）
                .addFilterBefore(securityHeadersFilter, UsernamePasswordAuthenticationFilter.class)

                // 2. 登录限流（在JWT过滤器之前）
                .addFilterBefore(loginRateLimitFilter, UsernamePasswordAuthenticationFilter.class)

                // 3. 禁用 CSRF（前后端分离项目使用 JWT，不需要 CSRF）
                .csrf().disable()

                // 4. 禁用 Session（使用无状态 JWT）
                .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()

                // 5. 配置路由权限
                .authorizeHttpRequests(auth -> auth
                        // ===== 公开接口（无需认证） =====
                        .antMatchers("/auth/**").permitAll()                          // 认证接口
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()          // 预检请求
                        .antMatchers("/error").permitAll()                           // 错误页面
                        .antMatchers("/actuator/health").permitAll()                  // 健康检查

                        // ===== 用户端公开接口 =====
                        .antMatchers("/announcement/public/**").permitAll()           // 公告公开查询

                        // ===== AI 智能助手接口（需要认证）=====
                        .antMatchers("/ai/**").authenticated()                         // AI接口需要登录

                        // ===== 管理员接口 =====
                        .antMatchers("/auth/register").hasRole("ADMIN")               // 只有管理员可注册用户

                        // ===== 其他接口需要认证 =====
                        .anyRequest().authenticated()
                )

                // 6. 添加 JWT 过滤器（在用户名密码认证过滤器之前）
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // 7. API 访问审计（最后执行）
                .addFilterAfter(apiAccessAuditFilter, JwtAuthFilter.class)

                // 8. 异常处理
                .exceptionHandling()
                    .authenticationEntryPoint((request, response, authException) -> {
                        // 未登录/Token无效时返回 JSON
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(401);
                        response.getWriter().write(
                                "{\"code\":401,\"message\":\"未登录或Token已过期，请重新登录\"}"
                        );
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        // 无权限时返回 JSON
                        response.setContentType("application/json;charset=UTF-8");
                        response.setStatus(403);
                        response.getWriter().write(
                                "{\"code\":403,\"message\":\"您没有权限访问该资源\"}"
                        );
                    });
    }
}
