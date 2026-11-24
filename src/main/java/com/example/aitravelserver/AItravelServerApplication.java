package com.example.aitravelserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication(exclude = {ReactiveSecurityAutoConfiguration.class})
@MapperScan("com.example.aitravelserver.mapper")
public class AItravelServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AItravelServerApplication.class, args);
    }
}
