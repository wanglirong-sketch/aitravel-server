package com.example.aitravelserver.dto.request;

import java.math.BigDecimal;
import lombok.Data;

@Data

public class FavoriteRequest {

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
}