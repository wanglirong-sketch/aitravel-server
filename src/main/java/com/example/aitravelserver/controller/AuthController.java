package com.example.aitravelserver.controller;


import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.dto.LoginDTO;
import com.example.aitravelserver.vo.LoginVO;
import com.example.aitravelserver.dto.RegisterDTO;
import com.example.aitravelserver.service.AuthService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;



/**
 * 鉴权入口，三个接口全部在 SecurityConfig 中放行。
 * 注意 @Valid 必须加在参数上，只加在 DTO 上不生效。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    //注册，返回空数据体
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.ok();
    }

    //登录，返回双令牌
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        // 登录
        return Result.ok(authService.login(dto));
    }

    // 刷新令牌，用请求参数传，方便前端在拦截器里直接调用
    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@RequestParam String refreshToken) {
        // 刷新令牌
        return Result.ok(authService.refresh(refreshToken));
    }
}
