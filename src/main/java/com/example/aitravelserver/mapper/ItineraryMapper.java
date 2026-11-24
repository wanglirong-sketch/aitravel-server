package com.example.aitravelserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aitravelserver.entity.Itinerary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ItineraryMapper extends BaseMapper<Itinerary> {
}
