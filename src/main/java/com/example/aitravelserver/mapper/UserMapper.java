package com.example.aitravelserver.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aitravelserver.entity.User;

/**
 * BaseMapper 已提供 insert / selectById / selectCount 等常用方法，
 * 普通增删改查不需要自己写 SQL，特殊查询再加方法或写 XML。
 */
public interface UserMapper extends BaseMapper<User> {

}