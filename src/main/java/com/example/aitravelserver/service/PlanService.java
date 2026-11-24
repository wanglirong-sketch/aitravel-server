package com.example.aitravelserver.service;

import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.vo.ItineraryPlanVO;

import java.util.List;

public interface PlanService {

    /**
     * 根据用户需求生成行程
     */
    ItineraryPlanVO plan(String query);

    /**
     * 供流式调用：检索候选点位
     */
    List<Poi> searchCandidates(String query);

    /**
     * 供流式调用：拼提示词
     */
    String buildPrompt(String query, List<Poi> candidates);

    /**
     * 供流式调用：把模型输出转成结构化行程
     */
    ItineraryPlanVO parseText(String text, List<Poi> candidates);

    /**
     * 多轮微调：在已有行程上按指令改写，返回新结果但不落库
     */
    ItineraryPlanVO refine(Long itineraryId, String instruction);

    /**
     * 系统提示词开放给流式服务复用
     */
    String systemPrompt();
}
