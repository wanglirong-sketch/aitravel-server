package com.example.aitravelserver.service;

import com.example.aitravelserver.entity.Poi;

import java.util.List;

public interface AiService {

    /**
     * 自然语言推荐：解析成结构化条件后交给规则检索
     */
    List<Poi> recommend(String query);
}
