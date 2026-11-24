package com.example.aitravelserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("poi")
public class Poi {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String city;
    /** 1 景点 2 美食 3 项目 4 打卡点 */
    private Integer type;
    private String name;
    private String intro;
    private String address;
    // 经度 纬度
    private BigDecimal lng;
    private BigDecimal lat;
    // 价格 开放时间 评分
    private BigDecimal price;
    private String openTime;
    private BigDecimal rating;
    private String cover;
    private String tags;
    private String bestTime;
    private String tips;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}