package com.example.aitravelserver.controller;


import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.common.auth.UserContext;
import com.example.aitravelserver.common.exception.BizException;
import com.example.aitravelserver.entity.User;
import com.example.aitravelserver.mapper.UserMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台：用户管理，仅 ADMIN 可访问
 */
@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    private final UserMapper userMapper;

    public AdminUserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /** 用户列表，支持关键词搜索 */
    @GetMapping("/users")
    public Result<List<User>> listUsers(@RequestParam(required = false) String keyword) {
        requireAdmin();

        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .eq(User::getDeleted, 0)
                        .and(StringUtils.hasText(keyword),
                                w -> w.like(User::getPhone, keyword)
                                        .or()
                                        .like(User::getNickname, keyword))
                        .orderByDesc(User::getId)
        );

        return Result.ok(users);
    }

    /** 删除用户 */
    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        requireAdmin();
        userMapper.deleteById(id);
        return Result.ok(null);
    }

    private void requireAdmin() {
        System.out.println("[ADMIN CHECK] role=" + UserContext.getRole());
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new BizException(403, "无权限访问管理后台");
        }
    }
}
