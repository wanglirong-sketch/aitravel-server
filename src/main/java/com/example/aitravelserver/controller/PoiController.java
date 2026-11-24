package com.example.aitravelserver.controller;


import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.entity.Poi;
import com.example.aitravelserver.service.AmapClient;
import com.example.aitravelserver.service.PoiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pois")//POI相关接口，用于获取POI信息
public class PoiController {
    private final PoiService poiService;
    private final AmapClient amapClient;

    public PoiController(PoiService poiService, AmapClient amapClient) {
        this.amapClient = amapClient;

        this.poiService = poiService;
    }

    //获取POI列表，支持按城市、类型、关键词筛选
    @GetMapping
    public Result<List<Poi>> list(@RequestParam(required = false) String city,
                                  @RequestParam(required = false) Integer type,
                                  @RequestParam(required = false) String key,
                                  @RequestParam(required = false) String tag) {
        return Result.ok(poiService.list(city, type, key, tag));
    }

    /** 在线点位：走高德，实时返回，不依赖库内死数据 */
    @GetMapping("/online")
    public Result<List<Poi>> online(@RequestParam(required = false) String city,
                                    @RequestParam(required = false) Integer type,
                                    @RequestParam(required = false) String key) {
        return Result.ok(amapClient.search(key, city, type));
    }

    //获取POI详情
    @GetMapping("/detail")
    public Result<Poi> detail(@RequestParam Long id) {
        return Result.ok(poiService.detail(id));
    }

    /** 自然语言检索：/api/pois/search?q=广州 早茶 */
    @GetMapping("/search")
    public Result<List<Poi>> search(@RequestParam String q) {
        return Result.ok(poiService.search(q));
    }

}
