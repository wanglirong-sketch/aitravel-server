package com.example.aitravelserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("itinerary_item")
public class ItineraryItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long itineraryId;
    private Integer dayIndex;
    private Integer seq;
    private Long poiId;
    private String placeName;
    private String startTime;
    private Integer duration;
    private String transport;
    private Integer cost;
    private String note;
    private LocalDateTime createTime;
    private Integer deleted;
}