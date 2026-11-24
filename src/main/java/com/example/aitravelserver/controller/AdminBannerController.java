package com.example.aitravelserver.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.entity.Banner;
import com.example.aitravelserver.mapper.BannerMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 后台轮播图管理接口。
 *
 * 这些接口只供管理员使用，包括：
 * 1. 查询轮播图列表；
 * 2. 新增轮播图；
 * 3. 编辑轮播图；
 * 4. 删除轮播图；
 * 5. 上传轮播图片。
 */
@RestController
@RequestMapping("/api/admin/banners")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBannerController {

    private final BannerMapper bannerMapper;

    //上传图片保存根目录。
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    //轮播图上传目录下的子目录。
    @Value("${app.upload.subdir:banner}")
    private String subdir;

    public AdminBannerController(BannerMapper bannerMapper) {
        this.bannerMapper = bannerMapper;
    }

    /**
     * 查询后台轮播图列表。
     *
     * 请求方式：GET
     * 请求地址：/api/admin/banners
     */
    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        LambdaQueryWrapper<Banner> wrapper = new LambdaQueryWrapper<>();

        // 按排序值升序，再按主键 ID 升序
        wrapper.orderByAsc(Banner::getSortNo).orderByAsc(Banner::getId);

        List<Banner> banners = bannerMapper.selectList(wrapper);
        return Result.ok(buildMaps(banners));
    }

    /**
     * 新增轮播图。
     *
     * 请求方式：POST
     * 请求地址：/api/admin/banners
     * 请求体：轮播图标题、副标题、图片地址、链接、排序、状态等 JSON 数据
     */
    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Banner request) {
        // 先校验参数是否合法
        Result<Banner> checked = validate(request);
        if (checked != null) {
            return Result.fail(checked.getCode(), checked.getMessage());
        }

        Banner banner = new Banner();
        copyRequest(banner, request);

        // 如果前端没传状态，默认启用
        banner.setStatus(request.getStatus() == null ? 1 : request.getStatus());

        // 如果前端没传排序值，默认排到 0
        banner.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());

        bannerMapper.insert(banner);
        return Result.ok(buildMap(banner));
    }

    /**
     * 编辑轮播图。
     *
     * 请求方式：PUT
     * 请求地址：/api/admin/banners/{id}
     */
    @PutMapping("/{id}")
    public Result<Map<String, Object>> update(@PathVariable Long id, @RequestBody Banner request) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return Result.fail(404, "轮播图不存在");
        }

        Result<Banner> checked = validate(request);
        if (checked != null) {
            return Result.fail(checked.getCode(), checked.getMessage());
        }

        copyRequest(banner, request);
        bannerMapper.updateById(banner);
        return Result.ok(buildMap(banner));
    }

    /**
     * 删除轮播图。
     * 当前使用 MyBatis-Plus 逻辑删除。
     *
     * 请求方式：DELETE
     * 请求地址：/api/admin/banners/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            return Result.fail(404, "轮播图不存在");
        }
        bannerMapper.deleteById(id);
        return Result.ok();
    }

    /**
     * 上传轮播图片。
     *
     * 请求方式：POST
     * 请求地址：/api/admin/banners/upload
     * 请求格式：multipart/form-data
     * 表单字段名：file
     *
     * 成功后返回图片访问地址，例如：
     * /uploads/banner/20261122103030-xxxx.jpg
     */
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.fail(400, "请选择要上传的图片");
        }

        String original = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "";
        String suffix = getSuffix(original);

        if (!isImageSuffix(suffix)) {
            return Result.fail(400, "仅支持 jpg、jpeg、png、webp 图片");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.fail(400, "图片不能超过 5MB");
        }

        try {
            Path dirPath = Paths.get(uploadDir, subdir).toAbsolutePath();
            File dir = dirPath.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileName = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                    + "-" + UUID.randomUUID().toString().replace("-", "") + suffix;

            File target = dirPath.resolve(fileName).toFile();
            file.transferTo(target);

            Map<String, String> data = new HashMap<>();
            data.put("imageUrl", "/uploads/" + subdir + "/" + fileName);
            return Result.ok(data);
        } catch (IOException ex) {
            ex.printStackTrace();
            return Result.fail(500, "图片保存失败：" + ex.getMessage());
        }
    }




    /**
     * 校验轮播图参数。
     *
     * @return 校验失败时返回失败 Result；校验通过返回 null
     */
    private Result<Banner> validate(Banner request) {
        if (request == null) {
            return Result.fail(400, "请求参数不能为空");
        }
        if (!StringUtils.hasText(request.getImageUrl())) {
            return Result.fail(400, "图片不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            return Result.fail(400, "标题不能为空");
        }
        if (request.getTitle().length() > 100) {
            return Result.fail(400, "标题不能超过 100 个字");
        }
        if (request.getSubtitle() != null && request.getSubtitle().length() > 200) {
            return Result.fail(400, "副标题不能超过 200 个字");
        }
        return null;
    }

    /**
     * 把前端提交的请求数据复制到数据库实体对象中。
     */
    private void copyRequest(Banner target, Banner request) {
        target.setImageUrl(request.getImageUrl().trim());
        target.setTitle(request.getTitle().trim());
        target.setSubtitle(StringUtils.hasText(request.getSubtitle()) ? request.getSubtitle().trim() : null);
        target.setLinkUrl(StringUtils.hasText(request.getLinkUrl()) ? request.getLinkUrl().trim() : null);
        target.setSortNo(request.getSortNo());
        target.setStatus(request.getStatus());
    }

    /**
     * 获取文件后缀，例如 .jpg、.png。
     */
    private String getSuffix(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0) {
            return "";
        }
        return fileName.substring(index).toLowerCase();
    }

    /**
     * 判断文件后缀是否是允许上传的图片格式。
     */
    private boolean isImageSuffix(String suffix) {
        return ".jpg".equals(suffix) || ".jpeg".equals(suffix) || ".png".equals(suffix) || ".webp".equals(suffix);
    }

    /**
     * 把实体列表转换成前端需要的 Map 列表。
     */
    private List<Map<String, Object>> buildMaps(List<Banner> banners) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Banner banner : banners) {
            result.add(buildMap(banner));
        }
        return result;
    }

    /**
     * 把单个实体对象转换成 Map 返回给前端。
     */
    private Map<String, Object> buildMap(Banner banner) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", banner.getId());
        map.put("imageUrl", banner.getImageUrl());
        map.put("title", banner.getTitle());
        map.put("subtitle", banner.getSubtitle());
        map.put("linkUrl", banner.getLinkUrl());
        map.put("sortNo", banner.getSortNo());
        map.put("status", banner.getStatus());
        map.put("createTime", banner.getCreateTime());
        map.put("updateTime", banner.getUpdateTime());
        return map;
    }
}
