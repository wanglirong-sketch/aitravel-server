package com.example.aitravelserver.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ItineraryDetailVO {
    private Long id;
    private String title;
    private String summary;
    private String city;
    private Integer dayCount;      // 总天数
    private Integer budget;        // 总花费
    private Integer status;
    private LocalDateTime createTime;
    private List<DayPlanVO> days;  // 逐天行
}
