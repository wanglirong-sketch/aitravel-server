package com.example.aitravelserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("itinerary")
public class Itinerary {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String city;
    private Integer days;
    private Integer budget;
    private Integer status;
    private String summary;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}