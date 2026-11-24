package com.example.aitravelserver.controller;

import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.service.AiService;
import com.example.aitravelserver.service.PlanService;
import com.example.aitravelserver.service.PlanStreamService;
import com.example.aitravelserver.vo.ItineraryPlanVO;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 业务入口。两个接口都需要登录，用于验证鉴权在真实业务里生效。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final PlanService planService;
    private final PlanStreamService planStreamService;


    public AiController(AiService aiService, PlanService planService, PlanStreamService planStreamService) {
        this.aiService = aiService;
        this.planService = planService;
        this.planStreamService = planStreamService;



    }

    @GetMapping("/ping")
    public Result<String> ping(@RequestParam(required = false) String q) {
        return Result.ok("收到: " + q);
    }

    @GetMapping("/recommend")
    public Result<List<Poi>> recommend(@RequestParam String q) {
        return Result.ok(aiService.recommend(q));
    }

    @PostMapping("/plan")
    public Result<ItineraryPlanVO> plan(@RequestBody Map<String, String> req) {
        return Result.ok(planService.plan(req.get("query")));
    }
    @GetMapping(value = "/plan/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter planStream(@RequestParam String query) {
        return planStreamService.stream(query);
    }

    @PostMapping("/refine")
    public Result<ItineraryPlanVO> refine(@RequestBody Map<String, Object> req) {
        Long itineraryId = Long.valueOf(String.valueOf(req.get("itineraryId")));
        String instruction = (String) req.get("instruction");
        return Result.ok(planService.refine(itineraryId, instruction));
    }



}