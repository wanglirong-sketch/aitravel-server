package com.example.aitravelserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.mapper.PoiMapper;
import com.example.aitravelserver.service.PoiService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PoiServiceImpl implements PoiService {

    private final PoiMapper poiMapper;

    public PoiServiceImpl(PoiMapper poiMapper) {
        this.poiMapper = poiMapper;
    }

    /**
     * 目前支持的城市，后续加城市只要往这里补名字
     */
    private static final List<String> CITIES = Arrays.asList("广州", "珠海", "桂林");

    /**
     * 口语里的动词、虚词、时间词，拆意图前先剔掉
     */
    private static final List<String> STOP_WORDS = Arrays.asList(
            "想看", "想", "想要", "想去", "想要去", "要", "去", "顺便", "然后", "还有", "以及", "和",
            "两天", "三天", "四天", "一天", "几天", "周末",
            "推荐", "帮我", "安排", "一下", "旅游", "旅行", "玩玩", "玩", "逛",
            "的", "了", "吗", "呢", "有", "什么", "哪些", "怎么", "如何", "可以"
    );

    /**
     * 类型词典，把口语词映射成 type 字段
     */
    private static final Map<Integer, List<String>> TYPE_WORDS = new HashMap<Integer, List<String>>() {{
        put(1, Arrays.asList("景点", "景区", "公园", "山", "古镇", "寺庙", "博物馆", "地标"));
        put(2, Arrays.asList("美食", "早茶", "吃", "餐厅", "小吃", "米粉", "海鲜", "甜品", "宵夜"));
        put(3, Arrays.asList("项目", "玩", "体验", "乐园", "游船", "漂流", "温泉", "演出", "骑行"));
        put(4, Arrays.asList("打卡", "拍照", "机位", "街拍", "出片", "夜景"));
    }};

    @Override
    public List<Poi> list(String city, Integer type, String key, String tag) {
        LambdaQueryWrapper<Poi> q = Wrappers.<Poi>lambdaQuery();

        q.eq(StringUtils.hasText(city), Poi::getCity, city)
                .eq(type != null, Poi::getType, type)
                .like(StringUtils.hasText(tag), Poi::getTags, tag)
                .and(StringUtils.hasText(key), w -> w.like(Poi::getName, key)
                        .or().like(Poi::getTags, key))
                .orderByDesc(Poi::getRating);

        return poiMapper.selectList(q);
    }

    @Override
    public List<Poi> search(String q) {
        // 查询为空：按评分返回全部
        if (!StringUtils.hasText(q)) {
            return poiMapper.selectList(Wrappers.<Poi>lambdaQuery()
                    .orderByDesc(Poi::getRating));
        }

        String rest = q.trim();

        // 拆城市
        String city = null;
        for (String c : CITIES) {
            if (rest.contains(c)) {
                city = c;
                rest = rest.replace(c, " ");
            }
        }

        // 拆类型词
        Integer type = null;
        for (Map.Entry<Integer, List<String>> e : TYPE_WORDS.entrySet()) {
            for (String w : e.getValue()) {
                if (rest.contains(w)) {
                    type = e.getKey();
                    rest = rest.replace(w, " ");
                }
            }
        }

        // 停用词清洗，位置在切词之前
        for (String w : STOP_WORDS) {
            rest = rest.replace(w, " ");
        }

        List<String> tokens = Arrays.stream(rest.split("[\\s,，、]+"))
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());

        LambdaQueryWrapper<Poi> w = Wrappers.<Poi>lambdaQuery()
                .eq(city != null, Poi::getCity, city)
                .eq(type != null, Poi::getType, type);

        if (!tokens.isEmpty()) {
            // 城市/类型/关键词同时生效，AND 关系；匹配不到就返回空
            w.and(inner -> {
                for (int i = 0; i < tokens.size(); i++) {
                    String t = tokens.get(i);
                    if (i > 0) {
                        inner.or();
                    }
                    inner.like(Poi::getName, t)
                            .or().like(Poi::getTags, t)
                            .or().like(Poi::getIntro, t)
                            .or().like(Poi::getAddress, t);
                }
            });

            List<Poi> result = poiMapper.selectList(w);
            result.sort((a, b) -> {
                int sa = score(a, tokens);
                int sb = score(b, tokens);
                if (sa != sb) {
                    return sb - sa;
                }
                return compareRating(b, a);
            });
            return result;
        }

        // 没有关键词：只按城市/类型过滤，按评分排
        return poiMapper.selectList(w.orderByDesc(Poi::getRating));
    }

    @Override
    public Poi detail(Long id) {
        return poiMapper.selectById(id);
    }

    /**
     * 计算匹配分数
     */
    private int score(Poi p, List<String> tokens) {
        int s = 0;
        for (String t : tokens) {
            if (contains(p.getName(), t)) {
                s += 3;
            }
            if (contains(p.getTags(), t)) {
                s += 2;
            }
            if (contains(p.getIntro(), t)) {
                s += 1;
            }
        }
        return s;
    }

    /**
     * 判断字符串是否包含目标词
     */
    private boolean contains(String src, String t) {
        return src != null && src.contains(t);
    }

    /**
     * 按评分比较
     */
    private int compareRating(Poi a, Poi b) {
        double ra = a.getRating() == null ? 0 : a.getRating().doubleValue();
        double rb = b.getRating() == null ? 0 : b.getRating().doubleValue();
        return Double.compare(ra, rb);
    }
}
