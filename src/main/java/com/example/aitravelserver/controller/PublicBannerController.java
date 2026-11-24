package com.example.aitravelserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.entity.Banner;
import com.example.aitravelserver.mapper.BannerMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页轮播图公开接口。
 *
 * 这个接口不需要管理员权限，普通用户、游客打开首页时都可以访问。
 * 只返回状态为“启用”的轮播图，停用的轮播图不会展示在首页。
 */
@RestController
@RequestMapping("/api/banners")
public class PublicBannerController {

    private final BannerMapper bannerMapper;

    /**
     * 构造器注入 BannerMapper。
     * Spring 启动时会自动把 Mapper 实现类传进来。
     */
    public PublicBannerController(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    /**
     * 查询首页公开轮播图。
     *
     * 请求方式：GET
     * 请求地址：/api/banners/public
     *
     * 查询规则：
     * 1. 只查询 status = 1 的轮播图，1 表示启用；
     * 2. 按 sort_no 升序排序，数值越小越靠前；
     * 3. 排序相同时，再按 id 升序排序。
     *
     * @return 启用状态的轮播图列表，统一用 Result.ok 返回
     */
    @GetMapping("/public")
    public Result<List<Map<String, Object>>> listPublic() {
        // 构建 MyBatis-Plus 查询条件
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();

        // 只查询启用状态的轮播图：status = 1
        wrapper.eq(Banner::getStatus, 1)
                // 按排序字段升序，sort_no 越小展示位置越靠前
                .orderByAsc(Banner::getSortNo)
                // 排序相同时，按主键 id 升序
                .orderByAsc(Banner::getId);

        // 从数据库查询轮播图列表
        List<Banner> banners = bannerMapper.selectList(wrapper);

        // 把实体对象转换成前端容易使用的 Map 列表
        List<Map<String, Object>> result = new ArrayList<>();
        for (Banner banner : banners) {
            Map<String, Object> map = new HashMap<>();

            // 轮播图主键 ID，前端可作为列表 key 使用
            map.put("id", banner.getId());

            // 图片访问地址，例如：/uploads/banner/xxx.jpg
            map.put("imageUrl", banner.getImageUrl());

            // 轮播主标题，显示在首页大图上
            map.put("title", banner.getTitle());

            // 轮播副标题，显示在主标题下方
            map.put("subtitle", banner.getSubtitle());

            // 点击轮播图后跳转的地址，可为空
            map.put("linkUrl", banner.getLinkUrl());

            result.add(map);
        }

        // 按项目统一返回体约定：code = 0 表示成功，data 放业务数据
        return Result.ok(result);
    }
}
