package com.jd.genie.config.security;

import com.jd.genie.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 拦截所有请求，验证JWT Token，并将用户信息存入SecurityContext
 * 兼容Spring Boot 3的jakarta.servlet包
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    /**
     * 核心过滤方法
     * 每个请求只执行一次
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // 1. 从请求头中获取JWT Token
            String jwt = getJwtFromRequest(request);

            // 2. 如果Token存在且有效
            if (StringUtils.hasText(jwt) && JwtUtil.validateToken(jwt)) {

                // 3. 从Token中获取用户名
                String username = JwtUtil.getUsernameFromToken(jwt);

                // 4. 加载用户详细信息
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // 6. 设置请求详情
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 7. 将认证信息存入SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("JWT认证成功: username={}", username);
            }
        } catch (Exception e) {
            log.error("JWT认证失败: {}", e.getMessage(), e);
            // 认证失败不抛出异常，继续过滤器链
            // 让Spring Security的异常处理器处理未认证的请求
        }

        // 继续过滤器链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取JWT Token
     * 支持 "Bearer {token}" 格式
     *
     * @param request HTTP请求
     * @return JWT Token字符串（不含Bearer前缀）
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        // 检查是否存在Authorization头，且以"Bearer "开头
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 去除"Bearer "前缀，返回纯Token
            return bearerToken.substring(7);
        }

        return null;
    }
}
