package com.example.aitravelserver.common.auth;


import com.example.aitravelserver.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;


@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtUtils {
    /**
     * 令牌的签发与解析。
     * 构造函数里一次性构建 SecretKey，避免每次签发都重复计算。
     */
    private final JwtProperties props;// 配置项
    private final SecretKey key;// 签发令牌的密钥

    //访问令牌，带角色，用于接口鉴权
    public JwtUtils(JwtProperties props) {
        this.props = props;
        // 使用配置项里的 secret 构建 SecretKey
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    //角色固定写 REFRESH，防止被当成访问令牌直接使用
    public String createRefresh(Long userId) {
        return create(userId, "REFRESH", props.getRefreshDays() * 24 * 60 * 60_000L);
    }

    //统一构造：subject 存用户 id，role 放自定义声明，过期时间按传入的毫秒数计算
    private String create(Long userId, String role, long ttlMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))//用户id
                .claim("role", role)//角色
                .issuedAt(now)//签发时间
                .expiration(new Date(now.getTime() + ttlMillis))//过期时间
                .signWith(key)//签名
                .compact();//生成令牌
    }

    //解析并校验签名与过期时间，失败会抛异常，由调用方决定如何处理
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    /** 访问令牌，带角色，用于接口鉴权 */
    public String createAccess(Long userId, String role) {
        return create(userId, role, props.getAccessMinutes() * 60_000L);
    }


}
