package com.example.aitravelserver.service;

import com.example.aitravelserver.entity.Poi;

import java.util.List;

public interface AmapClient {

    /**
     * 高德关键字搜索
     *
     * @param keyword 关键词（名称/特色）
     * @param city    城市名，可空；为空时查演示城市并合并
     * @param type    1 景点，2 美食，3 项目，4 购物
     */
    List<Poi> search(String keyword, String city, Integer type);
}