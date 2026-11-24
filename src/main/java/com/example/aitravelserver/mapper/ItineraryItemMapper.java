package com.example.aitravelserver.mapper;

import com.example.aitravelserver.entity.ItineraryItem;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
public interface ItineraryItemMapper extends BaseMapper<ItineraryItem> {
}