package com.example.aitravelserver.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 上传图片静态资源映射配置。
 *
 * 作用：浏览器访问 /uploads/** 时，
 * 后端会从本地 uploads 目录读取图片文件。
 *
 * 例如：
 * 数据库保存：/uploads/banner/xxx.jpg
 * 浏览器访问：http://localhost:8080/uploads/banner/xxx.jpg
 */
@Configuration// 配置类
public class WebResourceConfig implements WebMvcConfigurer {

    // 上传目录
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    // 添加资源处理器
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 把上传目录转换为绝对路径，避免项目启动目录不同导致找不到图片
        String absoluteDir = Paths.get(uploadDir).toAbsolutePath().toString();
        // /uploads/** 表示访问上传目录下的所有图片资源
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteDir + "/");
    }
}
