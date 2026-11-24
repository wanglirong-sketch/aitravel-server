package com.example.aitravelserver.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aitravelserver.common.auth.UserContext;
import com.example.aitravelserver.common.exception.BizException;
import com.example.aitravelserver.entity.Itinerary;
import com.example.aitravelserver.entity.ItineraryItem;
import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.mapper.ItineraryItemMapper;
import com.example.aitravelserver.mapper.ItineraryMapper;
import com.example.aitravelserver.mapper.PoiMapper;
import com.example.aitravelserver.service.ItineraryService;
import com.example.aitravelserver.vo.DayPlanVO;
import com.example.aitravelserver.vo.ItineraryDetailVO;
import com.example.aitravelserver.vo.ItineraryPlanVO;
import com.example.aitravelserver.vo.PlanItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 行程的保存、查询、修改。所有操作按当前登录用户隔离。
 */
@Service
public class ItineraryServiceImpl implements ItineraryService {

    private final ItineraryMapper itineraryMapper;
    private final ItineraryItemMapper itemMapper;
    private final PoiMapper poiMapper;

    public ItineraryServiceImpl(ItineraryMapper itineraryMapper,
                                ItineraryItemMapper itemMapper,
                                PoiMapper poiMapper) {
        this.itineraryMapper = itineraryMapper;
        this.itemMapper = itemMapper;
        this.poiMapper = poiMapper;
    }

    @Override
    @Transactional
    public Long save(ItineraryPlanVO plan) {
        if (plan == null || plan.getDays() == null || plan.getDays().isEmpty()) {
            throw new BizException(400, "行程内容为空，无法保存");
        }

        Itinerary it = new Itinerary();
        it.setUserId(UserContext.getUserId());
        it.setTitle(plan.getTitle());
        it.setSummary(plan.getSummary());
        it.setDays(plan.getDays().size());
        it.setBudget(plan.getTotalCost() == null ? 0 : plan.getTotalCost());
        it.setStatus(1);
        it.setCity(plan.getCity());
        it.setCreateTime(LocalDateTime.now());
        it.setUpdateTime(LocalDateTime.now());
        it.setDeleted(0);

        itineraryMapper.insert(it);
        insertItems(it.getId(), plan.getDays());
        return it.getId();
    }

    @Override
    public List<Itinerary> myList() {
        return itineraryMapper.selectList(Wrappers.<Itinerary>lambdaQuery()
                .eq(Itinerary::getUserId, UserContext.getUserId())
                .orderByDesc(Itinerary::getId));
    }

    @Override
    public ItineraryDetailVO detail(Long id) {
        Itinerary it = requireOwned(id);

        List<ItineraryItem> items = itemMapper.selectList(Wrappers.<ItineraryItem>lambdaQuery()
                .eq(ItineraryItem::getItineraryId, id)
                .orderByAsc(ItineraryItem::getDayIndex)
                .orderByAsc(ItineraryItem::getSeq));

        List<Long> poiIds = new ArrayList<>();
        for (ItineraryItem i : items) {
            if (i.getPoiId() != null) {
                poiIds.add(i.getPoiId());
            }
        }

        Map<Long, Integer> typeMap = new HashMap<>();
        if (!poiIds.isEmpty()) {
            List<Poi> pois = poiMapper.selectBatchIds(poiIds);
            for (Poi p : pois) {
                typeMap.put(p.getId(), p.getType());
            }
        }

        ItineraryDetailVO vo = new ItineraryDetailVO();
        vo.setId(it.getId());
        vo.setTitle(it.getTitle());
        vo.setSummary(it.getSummary());
        vo.setCity(it.getCity());
        vo.setDayCount(it.getDays());
        vo.setBudget(it.getBudget());
        vo.setStatus(it.getStatus());
        vo.setCreateTime(it.getCreateTime());

        List<DayPlanVO> days = new ArrayList<>();
        for (ItineraryItem item : items) {
            DayPlanVO day = null;
            for (DayPlanVO d : days) {
                if (d.getDay().equals(item.getDayIndex())) {
                    day = d;
                    break;
                }
            }

            if (day == null) {
                day = new DayPlanVO();
                day.setDay(item.getDayIndex());
                day.setItems(new ArrayList<>());
                days.add(day);
            }

            PlanItemVO pi = new PlanItemVO();
            pi.setTime(item.getStartTime());
            pi.setPoiId(item.getPoiId());
            pi.setName(item.getPlaceName());
            pi.setType(typeMap.get(item.getPoiId()));
            pi.setStayMinutes(item.getDuration());
            pi.setTransport(item.getTransport());
            pi.setCost(item.getCost());
            pi.setNote(item.getNote());

            day.getItems().add(pi);
        }

        vo.setDays(days);
        return vo;
    }

    @Override
    @Transactional
    public void replace(Long id, ItineraryPlanVO plan) {
        Itinerary it = requireOwned(id);
        it.setTitle(plan.getTitle());
        it.setSummary(plan.getSummary());
        it.setDays(plan.getDays().size());
        it.setBudget(plan.getTotalCost() == null ? 0 : plan.getTotalCost());
        if (plan.getCity() != null) {
            it.setCity(plan.getCity());
        }
        it.setUpdateTime(LocalDateTime.now());
        itineraryMapper.updateById(it);

        itemMapper.delete(Wrappers.<ItineraryItem>lambdaQuery()
                .eq(ItineraryItem::getItineraryId, id));

        insertItems(id, plan.getDays());
    }

    @Override
    @Transactional
    public void updateItem(Long itineraryId, Long itemId, PlanItemVO patch) {
        requireOwned(itineraryId);

        ItineraryItem item = itemMapper.selectById(itemId);
        if (item == null || !item.getItineraryId().equals(itineraryId)) {
            throw new BizException(404, "节点不存在");
        }

        if (patch.getTime() != null) {
            item.setStartTime(patch.getTime());
        }
        if (patch.getName() != null) {
            item.setPlaceName(patch.getName());
        }
        if (patch.getStayMinutes() != null) {
            item.setDuration(patch.getStayMinutes());
        }
        if (patch.getTransport() != null) {
            item.setTransport(patch.getTransport());
        }
        if (patch.getCost() != null) {
            item.setCost(patch.getCost());
        }
        if (patch.getNote() != null) {
            item.setNote(patch.getNote());
        }

        itemMapper.updateById(item);
        recalcBudget(itineraryId);
    }

    @Override
    @Transactional
    public void deleteItem(Long itineraryId, Long itemId) {
        requireOwned(itineraryId);
        itemMapper.deleteById(itemId);
        recalcBudget(itineraryId);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireOwned(id);
        itineraryMapper.deleteById(id);
        itemMapper.delete(Wrappers.<ItineraryItem>lambdaQuery()
                .eq(ItineraryItem::getItineraryId, id));
    }

    @Override
    public List<Poi> poisOf(Long itineraryId) {
        List<ItineraryItem> items = itemMapper.selectList(
                Wrappers.<ItineraryItem>lambdaQuery()
                        .eq(ItineraryItem::getItineraryId, itineraryId)
        );

        List<Long> ids = new ArrayList<>();
        for (ItineraryItem i : items) {
            if (i.getPoiId() != null) {
                ids.add(i.getPoiId());
            }
        }

        if (ids.isEmpty()) {
            return new ArrayList<>();
        }

        return poiMapper.selectBatchIds(ids);
    }

    private void insertItems(Long itineraryId, List<DayPlanVO> days) {
        for (DayPlanVO day : days) {
            int idx = 1;
            for (PlanItemVO pi : day.getItems()) {
                ItineraryItem item = new ItineraryItem();
                item.setItineraryId(itineraryId);
                item.setDayIndex(day.getDay());
                item.setSeq(idx++);
                item.setPoiId(pi.getPoiId());
                item.setPlaceName(pi.getName());
                item.setStartTime(pi.getTime());
                item.setDuration(pi.getStayMinutes());
                item.setTransport(pi.getTransport());
                item.setCost(pi.getCost());
                item.setNote(pi.getNote());
                item.setCreateTime(LocalDateTime.now());
                item.setDeleted(0);
                itemMapper.insert(item);
            }
        }
    }

    private void recalcBudget(Long itineraryId) {
        List<ItineraryItem> items = itemMapper.selectList(
                Wrappers.<ItineraryItem>lambdaQuery()
                        .eq(ItineraryItem::getItineraryId, itineraryId)
        );

        int total = 0;
        for (ItineraryItem i : items) {
            total += i.getCost() == null ? 0 : i.getCost();
        }

        Itinerary it = itineraryMapper.selectById(itineraryId);
        if (it != null) {
            it.setBudget(total);
            it.setUpdateTime(LocalDateTime.now());
            itineraryMapper.updateById(it);
        }
    }

    private Itinerary requireOwned(Long id) {
        Itinerary it = itineraryMapper.selectById(id);
        if (it == null || !UserContext.getUserId().equals(it.getUserId())) {
            throw new BizException(404, "行程不存在");
        }
        return it;
    }
}
