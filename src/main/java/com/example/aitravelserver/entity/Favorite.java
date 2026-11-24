package com.example.aitravelserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("favorite")
public class Favorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long poiId;

    private String poiKey;

    private Integer poiType;

    private String name;

    private String tags;

    private String intro;

    private String cover;

    private BigDecimal rating;

    private BigDecimal price;

    private String tips;

    private String address;

    private BigDecimal lng;

    private BigDecimal lat;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}