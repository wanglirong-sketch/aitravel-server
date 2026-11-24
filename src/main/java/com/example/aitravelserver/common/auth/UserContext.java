package com.example.aitravelserver.common.auth;


import com.example.aitravelserver.common.exception.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 从 Spring 安全上下文取当前登录用户信息。
 * 用户 id 由 JwtAuthFilter 写入 principal。
 */
public class UserContext {
    public static Long userId() {
        return getUserId();
    }

    private UserContext() {
    }

    /** 取当前登录用户 id，未登录抛 401 */
    public static Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new BizException(401, "未登录或登录已过期");
        }
        return (Long) auth.getPrincipal();
    }

    /** 取角色，未登录返回 null */
    public static String getRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return null;
        }
        for (org.springframework.security.core.GrantedAuthority a : auth.getAuthorities()) {
            String name = a.getAuthority();
            return name != null && name.startsWith("ROLE_") ? name.substring(5) : name;
        }
        return null;
    }

    /** 清空上下文，登出时用 */
    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}