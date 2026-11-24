package com.example.aitravelserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aitravelserver.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
}