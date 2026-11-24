package com.example.aitravelserver.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface PlanStreamService {

    /**
     * SSE 推流：start → retrieving → token(多次) → done / error
     */
    SseEmitter stream(String query);
}
