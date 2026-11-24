package com.example.aitravelserver.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 高德开放平台配置，读取 application.yml 的 amap.key */
@Component
@ConfigurationProperties(prefix = "amap")
public class AmapProperties {

    private String key;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
