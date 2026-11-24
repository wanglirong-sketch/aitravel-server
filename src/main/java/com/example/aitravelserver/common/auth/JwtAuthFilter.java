package com.example.aitravelserver.common.auth;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
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
 * 令牌过滤器，只负责“解析并放进上下文”，不负责拦截。
 * 拦截由后面的授权规则完成：没放进上下文且路径需要登录，就返回 401。
 */
@Slf4j // 日志
@Component // Spring 管理
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    public JwtAuthFilter(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. 优先取请求头里的令牌
        String header = request.getHeader("Authorization");
        String token = null;
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }

        // 2. 请求头没有，回退到 query 参数，供 EventSource 使用（它无法带自定义请求头）
        if (!StringUtils.hasText(token)) {
            token = request.getParameter("token");
        }

        // 3. 有令牌就解析，没有就放过，让后续规则决定
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtUtils.parse(token);              // 获取令牌信息
                Long userId = Long.valueOf(claims.getSubject());    // 获取用户ID
                String role = (String) claims.get("role");          // 获取角色

                // 刷新令牌不能当访问令牌用，这里显式排除刷新令牌
                if (!"REFRESH".equals(role)) {
                    // 将用户ID和角色放进上下文
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                // 解析失败，放过，让后续规则决定
                log.info("JWT 解析失败：{}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        // 继续处理下一个过滤器
        filterChain.doFilter(request, response);

    }
}
