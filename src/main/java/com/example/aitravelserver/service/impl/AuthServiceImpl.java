package com.example.aitravelserver.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.aitravelserver.common.auth.JwtUtils;
import com.example.aitravelserver.common.config.JwtProperties;
import com.example.aitravelserver.common.exception.BizException;
import com.example.aitravelserver.dto.LoginDTO;
import com.example.aitravelserver.dto.RegisterDTO;
import com.example.aitravelserver.entity.User;
import com.example.aitravelserver.mapper.UserMapper;
import com.example.aitravelserver.service.AuthService;
import com.example.aitravelserver.service.CaptchaService;
import com.example.aitravelserver.vo.LoginVO;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 注册与登录核心逻辑。
 * 刷新令牌同时写进 Redis，目的是支持“登出即失效”，纯 JWT 做不到主动吊销。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final JwtProperties props;
    private final StringRedisTemplate redis;
    private final CaptchaService captchaService;

    @Autowired
    public AuthServiceImpl(UserMapper userMapper,
                           PasswordEncoder passwordEncoder,
                           JwtUtils jwtUtils,
                           JwtProperties props,
                           StringRedisTemplate redis,
                           CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.props = props;
        this.redis = redis;
        this.captchaService = captchaService;
    }

    /**
     * 注册：先校验验证码，再查重、加密落库；昵称为空时给默认值
     */
    @Override
    public void register(RegisterDTO dto) {
        if (!captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
            throw new BizException(400, "验证码错误或已过期");
        }

        Long count = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getPhone, dto.getPhone())
        );

        if (count != null && count > 0) {
            throw new BizException(400, "该手机号已注册");
        }

        User user = new User();
        user.setPhone(dto.getPhone());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : "旅行者");
        user.setRole("USER");
        user.setStatus(1);

        userMapper.insert(user);
    }

    /**
     * 登录：先校验验证码，再校验状态、密码；提示语统一，避免泄露账号是否存在
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        if (!captchaService.verify(dto.getCaptchaId(), dto.getCaptchaCode())) {
            throw new BizException(400, "验证码错误或已过期");
        }

        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getPhone, dto.getPhone())
        );

        if (user == null) {
            throw new BizException(400, "手机号或密码不正确");
        }

        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(403, "账号已被禁用");
        }

        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(400, "手机号或密码不正确");
        }

        return issue(user);
    }

    /**
     * 刷新：令牌本身要验签，还要和 Redis 里的副本比对，才能确认未被注销
     */
    @Override
    public LoginVO refresh(String refreshToken) {
        try {
            var claims = jwtUtils.parse(refreshToken);
            Long userId = Long.valueOf(claims.getSubject());

            String stored = redis.opsForValue().get("refresh:" + userId);
            if (!refreshToken.equals(stored)) {
                throw new BizException(401, "登录已过期，请重新登录");
            }

            User user = userMapper.selectById(userId);
            if (user == null) {
                throw new BizException(401, "账号不存在");
            }

            return issue(user);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(401, "登录已过期，请重新登录");
        }
    }

    /**
     * 签发双令牌并缓存刷新令牌，登录与刷新共用
     */
    private LoginVO issue(User user) {
        String access = jwtUtils.createAccess(user.getId(), user.getRole());
        String refresh = jwtUtils.createRefresh(user.getId());

        redis.opsForValue().set(
                "refresh:" + user.getId(),
                refresh,
                props.getRefreshDays(),
                TimeUnit.DAYS
        );

        return LoginVO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .expiresIn(props.getAccessMinutes() * 60)
                .userId(user.getId())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }
}
