package com.example.aitravelserver.vo;

import lombok.Data;

import java.util.List;

/** 单天行程 */
@Data
public class DayPlanVO {
    private Integer day;
    private List<PlanItemVO> items;
}