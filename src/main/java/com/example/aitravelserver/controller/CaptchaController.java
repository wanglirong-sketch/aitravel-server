package com.example.aitravelserver.controller;

import com.example.aitravelserver.common.Result;
import com.example.aitravelserver.dto.request.CaptchaVO;
import com.example.aitravelserver.service.CaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CaptchaController {

    @Autowired
    private CaptchaService captchaService;

    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        return Result.ok(captchaService.createCaptcha());
    }
}