package com.jd.genie.config;

import com.jd.genie.config.security.JwtAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 配置类
 * 配置JWT认证、跨域、白名单等
 * 完美适配Spring Boot 3.x
 *
 * @author JD Genie
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用方法级安全注解（@PreAuthorize等）
public class SecurityConfig {

    /**
     * 配置密码编码器（BCrypt）
     * BCrypt是目前最安全的密码加密算法之一
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置JWT认证过滤器
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * 配置Spring Security过滤器链
     * 定义哪些路径需要认证，哪些路径公开访问
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.info("配置Spring Security过滤器链");

        http
                // 禁用CSRF（因为使用JWT，不需要CSRF保护）
                .csrf(csrf -> csrf.disable())

                // 配置跨域（CORS）
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 配置Session管理：无状态（不创建Session）
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 配置授权规则
                .authorizeHttpRequests(auth -> auth
                        // 白名单：允许匿名访问的路径
                        .requestMatchers(
                                "/api/user/login",      // 登录接口
                                "/api/user/register",   // 注册接口
                                "/web/health",          // 健康检查
                                "/AutoAgent",           // Agent调用接口
                                "/doc.html",            // Swagger UI
                                "/swagger-resources/**", // Swagger资源
                                "/v3/api-docs/**",      // OpenAPI文档
                                "/webjars/**"           // Swagger静态资源
                        ).permitAll()

                        // 其他所有请求都需要认证
                        .anyRequest().authenticated()
                )

                // 添加JWT认证过滤器（在UsernamePasswordAuthenticationFilter之前执行）
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置跨域（CORS）
     * 允许前端从不同域访问后端API
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允许的源（生产环境应该配置具体的域名）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 允许的请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 允许发送Cookie
        configuration.setAllowCredentials(true);

        // 预检请求的有效期（秒）
        configuration.setMaxAge(3600L);

        // 暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // 应用到所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
