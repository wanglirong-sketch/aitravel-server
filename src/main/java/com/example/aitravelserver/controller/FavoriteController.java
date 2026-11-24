package com.example.aitravelserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.common.auth.UserContext;
import com.example.aitravelserver.dto.request.FavoriteRequest;
import com.example.aitravelserver.entity.Favorite;
import com.example.aitravelserver.mapper.FavoriteMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteMapper favoriteMapper;

    public FavoriteController(FavoriteMapper favoriteMapper) {
        this.favoriteMapper = favoriteMapper;
    }

    /**
     * 当前用户收藏列表
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(value = "type", required = false) Integer type) {
        Long userId = UserContext.getUserId();

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        if (type != null) {
            wrapper.eq(Favorite::getPoiType, type);
        }
        wrapper.orderByDesc(Favorite::getCreateTime);

        List<Favorite> favorites = favoriteMapper.selectList(wrapper);
        return Result.ok(buildMaps(favorites));
    }

    /**
     * 统计：景点数、美食数、总数
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Long userId = UserContext.getUserId();
        List<Favorite> favorites = favoriteMapper.selectList(
                new LambdaQueryWrapper<Favorite>().eq(Favorite::getUserId, userId)
        );

        long scenic = favorites.stream()
                .filter(f -> f.getPoiType() != null && f.getPoiType() == 1)
                .count();
        long food = favorites.stream()
                .filter(f -> f.getPoiType() != null && f.getPoiType() == 2)
                .count();

        Map<String, Object> data = new HashMap<>();
        data.put("total", favorites.size());
        data.put("scenic", scenic);
        data.put("food", food);
        return Result.ok(data);
    }

    /**
     * 查询当前用户是否收藏了某个点位
     */
    @GetMapping("/check")
    public Result<Map<String, Object>> check(
            @RequestParam(value = "poiId", required = false) Long poiId,
            @RequestParam(value = "poiKey", required = false) String poiKey) {
        Long userId = UserContext.getUserId();
        String key = resolvePoiKey(poiId, poiKey);

        Favorite favorite = findByKey(userId, key);

        Map<String, Object> data = new HashMap<>();
        data.put("favorited", favorite != null);
        data.put("poiKey", key);
        return Result.ok(data);
    }

    /**
     * 收藏
     */
    @PostMapping
    public Result<Map<String, Object>> add(@RequestBody FavoriteRequest request) {
        Long userId = UserContext.getUserId();

        if (request == null) {
            return Result.fail(400, "收藏参数不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            return Result.fail(400, "点位名称不能为空");
        }

        String poiKey = resolvePoiKey(request.getPoiId(), request.getPoiKey());
        if (!StringUtils.hasText(poiKey)) {
            return Result.fail(400, "点位标识不能为空");
        }

        Favorite favorite = findByKey(userId, poiKey);
        if (favorite != null) {
            copyRequest(favorite, request);
            favoriteMapper.updateById(favorite);
        } else {
            favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setPoiId(request.getPoiId());
            favorite.setPoiKey(poiKey);
            copyRequest(favorite, request);
            favoriteMapper.insert(favorite);
        }

        return Result.ok(buildMap(favorite));
    }

    /**
     * 取消收藏：按收藏记录 ID 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        Favorite favorite = favoriteMapper.selectById(id);
        if (favorite == null) {
            return Result.fail(404, "收藏记录不存在");
        }
        if (!userId.equals(favorite.getUserId())) {
            return Result.fail(403, "无权删除该收藏");
        }
        favoriteMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 取消收藏：按点位 key 删除，方便前端只传点位信息
     */
    @DeleteMapping("/by-key")
    public Result<Void> removeByKey(
            @RequestParam(value = "poiId", required = false) Long poiId,
            @RequestParam(value = "poiKey", required = false) String poiKey) {
        Long userId = UserContext.getUserId();
        String key = resolvePoiKey(poiId, poiKey);
        if (!StringUtils.hasText(key)) {
            return Result.fail(400, "点位标识不能为空");
        }

        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        wrapper.eq(Favorite::getPoiKey, key);
        favoriteMapper.delete(wrapper);
        return Result.ok();
    }

    private Favorite findByKey(Long userId, String poiKey) {
        LambdaQueryWrapper<Favorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Favorite::getUserId, userId);
        wrapper.eq(Favorite::getPoiKey, poiKey);
        wrapper.last("limit 1");
        return favoriteMapper.selectOne(wrapper);
    }

    private void copyRequest(Favorite favorite, FavoriteRequest request) {
        favorite.setPoiId(request.getPoiId());
        favorite.setPoiKey(resolvePoiKey(request.getPoiId(), request.getPoiKey()));
        favorite.setPoiType(request.getPoiType());
        favorite.setName(trim(request.getName()));
        favorite.setTags(trim(request.getTags()));
        favorite.setIntro(trim(request.getIntro()));
        favorite.setCover(trim(request.getCover()));
        favorite.setRating(normalizeDecimal(request.getRating()));
        favorite.setPrice(normalizeDecimal(request.getPrice()));
        favorite.setTips(trim(request.getTips()));
        favorite.setAddress(trim(request.getAddress()));
        favorite.setLng(request.getLng());
        favorite.setLat(request.getLat());
    }

    private String resolvePoiKey(Long poiId, String poiKey) {
        if (StringUtils.hasText(poiKey)) {
            return poiKey.trim();
        }
        if (poiId != null) {
            return "poi_" + poiId;
        }
        return "";
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return StringUtils.hasText(text) ? text : null;
    }

    private BigDecimal normalizeDecimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value;
    }

    private List<Map<String, Object>> buildMaps(List<Favorite> favorites) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Favorite favorite : favorites) {
            result.add(buildMap(favorite));
        }
        return result;
    }

    private Map<String, Object> buildMap(Favorite favorite) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", favorite.getId());
        map.put("poiId", favorite.getPoiId());
        map.put("poiKey", favorite.getPoiKey());
        map.put("type", favorite.getPoiType());
        map.put("poiType", favorite.getPoiType());
        map.put("name", favorite.getName());
        map.put("tags", favorite.getTags());
        map.put("intro", favorite.getIntro());
        map.put("cover", favorite.getCover());
        map.put("rating", favorite.getRating());
        map.put("price", favorite.getPrice());
        map.put("tips", favorite.getTips());
        map.put("address", favorite.getAddress());
        map.put("lng", favorite.getLng());
        map.put("lat", favorite.getLat());
        map.put("createTime", favorite.getCreateTime());
        return map;
    }
}
