package com.example.aitravelserver.common.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * SSE 推流要用独立线程，不能占用 Tomcat 请求线程。
 */

//队列满了就让调用线程自己跑，宁可慢一点也不要直接拒绝请求。
@Configuration
public class AsyncConfig {

    @Bean("planExecutor")
    public ThreadPoolTaskExecutor planExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(8);
        exec.setQueueCapacity(50);
        exec.setThreadNamePrefix("plan-");
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.initialize();
        return exec;
    }
}