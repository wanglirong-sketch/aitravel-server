package com.example.aitravelserver.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 完整行程 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItineraryPlanVO {
    private String title;
    private String summary;
    private String city;
    private Integer totalCost;
    private List<DayPlanVO> days;
}