package com.example.aitravelserver.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.common.auth.UserContext;
import com.example.aitravelserver.dto.request.PasswordRequest;
import com.example.aitravelserver.dto.request.ProfileUpdateRequest;
import com.example.aitravelserver.entity.User;
import com.example.aitravelserver.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {


    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /** 返回当前登录用户 id，用来验证令牌链路是否打通 */
    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        return Result.ok(buildUserMap(user));
    }

    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(@RequestBody ProfileUpdateRequest request) {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        if (StringUtils.hasText(request.getNickname())) {
            String nickname = request.getNickname().trim();
            if (nickname.length() > 20) {
                return Result.fail(400, "昵称不能超过 20 个字");
            }
            user.setNickname(nickname);
        }

        String avatar = null;
        if (request.getAvatar() != null) {
            avatar = request.getAvatar().trim();
            if (avatar.length() > 1000000) {
                return Result.fail(400, "头像图片过大，请更换更小的图片");
            }
            user.setAvatar(avatar);
        }

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);

        if (StringUtils.hasText(request.getNickname())) {
            updateWrapper.set(User::getNickname, user.getNickname());
        }

        if (avatar != null) {
            updateWrapper.set(User::getAvatar, avatar);
        }

        userMapper.update(null, updateWrapper);

        User latest = userMapper.selectById(userId);
        return Result.ok(buildUserMap(latest));
    }


    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody PasswordRequest request) {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }

        String oldPassword = request.getOldPassword() == null ? "" : request.getOldPassword();
        String newPassword = request.getNewPassword() == null ? "" : request.getNewPassword();

        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            return Result.fail(400, "密码不能为空");
        }

        if (newPassword.length() < 6) {
            return Result.fail(400, "新密码至少 6 位");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return Result.fail(400, "当前密码不正确");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return Result.ok(null);
    }

    private Map<String, Object> buildUserMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("phone", user.getPhone());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        map.put("role", user.getRole());
        return map;
    }
}