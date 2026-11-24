package com.example.aitravelserver.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 绑定配置文件里的 jwt.* 三项
 * secret 必须不少于 32 字节，否则 HS256 初始化时会直接抛异常
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessMinutes = 120;
    private long refreshDays = 7;
}
