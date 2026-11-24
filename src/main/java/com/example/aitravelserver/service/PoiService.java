package com.example.aitravelserver.service;

import com.example.aitravelserver.entity.Poi;

import java.util.List;

public interface PoiService {

    /**
     * 按城市、类型、标签、关键词筛选
     */
    List<Poi> list(String city, Integer type, String key, String tag);

    /**
     * 自然语言检索
     */
    List<Poi> search(String q);

    /**
     * 详情
     */
    Poi detail(Long id);
}
