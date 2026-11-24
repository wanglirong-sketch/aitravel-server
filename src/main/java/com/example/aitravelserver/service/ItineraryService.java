package com.example.aitravelserver.service;

import com.example.aitravelserver.entity.Itinerary;
import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.vo.ItineraryDetailVO;
import com.example.aitravelserver.vo.ItineraryPlanVO;
import com.example.aitravelserver.vo.PlanItemVO;

import java.util.List;

public interface ItineraryService {

    /**
     * 保存生成结果，返回行程 id
     */
    Long save(ItineraryPlanVO plan);

    /**
     * 我的行程列表
     */
    List<Itinerary> myList();

    /**
     * 行程详情，含所有节点
     */
    ItineraryDetailVO detail(Long id);

    /**
     * 全量覆盖，用于“重新生成后替换”
     */
    void replace(Long id, ItineraryPlanVO plan);

    /**
     * 修改一个节点
     */
    void updateItem(Long itineraryId, Long itemId, PlanItemVO patch);

    /**
     * 删除一个节点
     */
    void deleteItem(Long itineraryId, Long itemId);

    /**
     * 删除整个行程
     */
    void delete(Long id);

    /**
     * 取某个行程里用到的所有点位，供微调时做白名单
     */
    List<Poi> poisOf(Long itineraryId);
}
