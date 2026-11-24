package com.example.aitravelserver.vo;


import lombok.Data;

/** 单个行程项 */
@Data
public class PlanItemVO {
    private String time;
    private Long poiId;
    private String name;
    private Integer type;
    private Integer stayMinutes;
    private Integer cost;
    private String note;
    private String transport;   // 交通方式，如“地铁”“步行”
}