package com.example.aitravelserver.common.config;

import com.example.aitravelserver.common.auth.JwtAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 安全配置，Boot 2.6 对应 Spring Security 5.6，用的是 SecurityFilterChain 而非
 * WebSecurityConfigurerAdapter（后者在 5.7 起被废弃）。
 */

@Configuration

public class SecurityConfig {
    // JWT 认证过滤器
    private final JwtAuthFilter jwtAuthFilter;
    // JSON 工具
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 构造函数，用于依赖注入
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    //密码加密器，注册与登录校验都用同一个实例
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .cors().and()
                .formLogin().disable()
                .httpBasic().disable()
                .logout().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/api/health", "/api/auth/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/banners/public").permitAll()
                .antMatchers("/uploads/**").permitAll()
                .antMatchers(HttpMethod.GET, "/api/pois/**").permitAll()
                .antMatchers("/auth/captcha").permitAll()
                .anyRequest().authenticated()
                .and()
                .exceptionHandling()
                .authenticationEntryPoint((req, res, e) -> writeJson(res, 401, "未登录或登录已过期"))
                .accessDeniedHandler((req, res, e) -> writeJson(res, 403, "无访问权限"))
                .and()

                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 统一输出 JSON，保持与 Result 结构一致
    private void writeJson(HttpServletResponse res, int code, String msg) throws IOException {
        // 设置状态码
        res.setStatus(code);
        // 设置内容类型
        res.setContentType("application/json;charset=UTF-8");
        // 设置响应体
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", msg);
        // 写入响应体
        res.getWriter().write(objectMapper.writeValueAsString(body));
    }


}
