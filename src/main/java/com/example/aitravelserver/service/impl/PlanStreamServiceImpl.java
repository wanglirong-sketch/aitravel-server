package com.example.aitravelserver.service.impl;

import com.example.aitravelserver.common.LlmClient;
import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.service.PlanService;
import com.example.aitravelserver.service.PlanStreamService;
import com.example.aitravelserver.vo.ItineraryPlanVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SSE 推流：start → retrieving → token(多次) → done / error
 */
@Service
public class PlanStreamServiceImpl implements PlanStreamService {

    private final LlmClient llm;
    private final PlanService planService;
    private final ThreadPoolTaskExecutor executor;

    public PlanStreamServiceImpl(LlmClient llm,
                                 PlanService planService,
                                 @Qualifier("planExecutor") ThreadPoolTaskExecutor executor) {
        this.llm = llm;
        this.planService = planService;
        this.executor = executor;
    }

    @Override
    public SseEmitter stream(String query) {
        // 3 分钟超时，够生成两天行程
        SseEmitter emitter = new SseEmitter(180000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(t -> emitter.complete());

        executor.execute(() -> {
            try {
                send(emitter, "start", singleton("message", "开始规划行程"));

                List<Poi> candidates = planService.searchCandidates(query);
                List<String> names = new ArrayList<>();
                for (Poi p : candidates) {
                    names.add(p.getName());
                }

                Map<String, Object> pick = new LinkedHashMap<>();
                pick.put("count", candidates.size());
                pick.put("names", names);
                send(emitter, "retrieving", pick);

                String userPrompt = planService.buildPrompt(query, candidates);

                StringBuilder fullText = new StringBuilder();

                String resultText = llm.chatStream(
                        planService.systemPrompt(),
                        userPrompt,
                        0.3,
                        2000,
                        delta -> {
                            fullText.append(delta);
                            send(emitter, "token", singleton("t", delta));
                        }
                );

                // 优先使用 chatStream 返回的全文；如果返回为空，再用回调收集到的文本
                String finalText = resultText != null && !resultText.isEmpty()
                        ? resultText
                        : fullText.toString();

                ItineraryPlanVO plan = planService.parseText(finalText, candidates);
                send(emitter, "done", plan);

                emitter.complete();
            } catch (Exception e) {
                try {
                    send(emitter, "error", singleton("message", "生成失败：" + e.getMessage()));
                } catch (Exception ignored) {
                    // 客户端已断开
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 生成一个只包含一个键值对的 Map
     */
    private Map<String, Object> singleton(String k, Object v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k, v);
        return m;
    }

    /**
     * 向客户端推送事件
     */
    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception ignored) {
            // 推送失败说明客户端断开，交给外层处理
        }
    }
}
